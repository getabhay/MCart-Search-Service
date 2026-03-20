package com.nova.mcart.service.impl;

import com.nova.mcart.dto.response.BulkUploadJobResponse;
import com.nova.mcart.dto.response.UploadUrlResponse;
import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.repository.BulkUploadJobRepository;
import com.nova.mcart.service.BulkUploadService;
import com.nova.mcart.service.aws.S3Service;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BulkUploadServiceImpl implements BulkUploadService {

    private static final Set<String> ALLOWED_BUCKETS = Set.of("brand", "category", "attribute", "attribute-value", "category-attribute", "product");

    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final S3Service s3Service;

    @Override
    public UploadUrlResponse generateUploadUrl(String bucket) {

        String key = bucket + "/upload/" + System.currentTimeMillis() + ".csv";

        String url = s3Service.generatePresignedUploadUrl(
                key,
                "text/csv",
                Duration.ofMinutes(60)
        );

        UploadUrlResponse response = new UploadUrlResponse();
        response.setUploadUrl(url);
        response.setS3Key(key);

        return response;
    }

    @Override
    public String uploadFile(String bucket, MultipartFile file) throws IOException {

        if (!ALLOWED_BUCKETS.contains(bucket)) {
            throw new IllegalArgumentException("Invalid bucket. Allowed: " + ALLOWED_BUCKETS);
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are allowed");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "text/csv";
        }

        String key = bucket + "/upload/" + System.currentTimeMillis() + ".csv";

        s3Service.uploadFile(
                key,
                file.getInputStream(),
                file.getSize(),
                contentType
        );


        return key;
    }

    @Override
    public BulkUploadJobResponse getJobStatus(Long jobId) {
        BulkUploadJob job = bulkUploadJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        BulkUploadJobResponse response = new BulkUploadJobResponse();
        response.setId(job.getId());
        response.setFileUrl(job.getFileUrl());
        response.setResultFileUrl(job.getResultFileUrl());
        response.setStatus(job.getStatus());
        response.setTotalRecords(job.getTotalRecords());
        response.setSuccessCount(job.getSuccessCount());
        response.setFailureCount(job.getFailureCount());
        response.setEntityType(job.getEntityType());

        return response;
    }
}
