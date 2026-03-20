package com.nova.mcart.common.bulk;

import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.service.aws.S3Service;
import com.nova.mcart.service.impl.BulkUploadJobTxService;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public abstract class AbstractBulkUploadProcessor<T> {

    private final BulkUploadJobTxService jobTxService;
    private final S3Service s3Service;

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void process(Long jobId) {

        // ✅ must be inside TX => handled by jobTxService (REQUIRES_NEW)
        if (!jobTxService.markProcessingIfPending(jobId)) {
            return;
        }

        BulkUploadJob job = jobTxService.getJobOrNull(jobId);
        if (job == null) {
            return;
        }

        int total = 0;
        int success = 0;
        int failure = 0;

        List<T> batch = new ArrayList<>();
        List<String> failedRows = new ArrayList<>();
        String failedKey = null;

        try (InputStream inputStream = s3Service.download(job.getFileUrl());
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            reader.readLine(); // header

            String line;
            while ((line = reader.readLine()) != null) {
                total++;

                try {
                    T entity = parseRow(line);

                    if (entity != null) {
                        batch.add(entity);
                        if (batch.size() >= 500) {
                            saveBatch(batch);
                            batch.clear();
                        }
                    }

                    success++;

                } catch (Exception ex) {
                    failure++;
                    failedRows.add(line + "," + sanitize(ex.getMessage()));
                }
            }

            if (!batch.isEmpty()) {
                saveBatch(batch);
            }

            if (!failedRows.isEmpty()) {
                failedKey = getFailedFolder() + "/failed_" + jobId + ".csv";
                generateFailedCsvAndUpload(failedRows, failedKey);
            }

            // ✅ update inside its own TX (REQUIRES_NEW)
            jobTxService.updateFinal(jobId, JobStatus.COMPLETED, total, success, failure, failedKey);

        } catch (Exception ex) {

            failedKey = getFailedFolder() + "/failed_" + jobId + ".csv";
            failedRows.add("JOB_ERROR," + sanitize(ex.getMessage()));

            try {
                generateFailedCsvAndUpload(failedRows, failedKey);
            } catch (Exception ignored) {}

            jobTxService.updateFinal(jobId, JobStatus.FAILED, total, success, Math.max(1, failure), failedKey);
        }
    }

    protected void generateFailedCsvAndUpload(List<String> failedRows, String key) {

        StringBuilder builder = new StringBuilder();
        builder.append(getFailedHeader()).append("\n");
        for (String row : failedRows) {
            builder.append(row).append("\n");
        }

        byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);

        s3Service.uploadFile(
                key,
                new ByteArrayInputStream(bytes),
                bytes.length,
                "text/csv"
        );
    }

    private String sanitize(String msg) {
        if (msg == null) return "";
        return msg.replace("\n", " ").replace("\r", " ").trim();
    }

    protected abstract T parseRow(String line);

    protected abstract void saveBatch(List<T> batch);

    protected abstract String getFailedFolder();

    protected abstract String getFailedHeader();
}
