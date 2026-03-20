package com.nova.mcart.service.impl;

import com.nova.mcart.common.bulk.AttributeBulkProcessor;
import com.nova.mcart.common.util.SlugGenerator;
import com.nova.mcart.dto.request.CreateAttributeRequest;
import com.nova.mcart.dto.request.UpdateAttributeRequest;
import com.nova.mcart.entity.Attribute;
import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.enums.AttributeType;
import com.nova.mcart.entity.enums.EntityType;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.repository.AttributeRepository;
import com.nova.mcart.repository.BulkUploadJobRepository;
import com.nova.mcart.service.AttributeService;
import com.nova.mcart.service.aws.S3Service;
import jakarta.transaction.Transactional;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService {

    private final AttributeRepository attributeRepository;
    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final S3Service s3Service;
    private final AttributeBulkProcessor attributeBulkProcessor;

    @Override
    @Transactional
    public Long startBulkJob(String s3Key) {

        BulkUploadJob job = new BulkUploadJob();
        job.setFileUrl(s3Key);
        job.setStatus(JobStatus.PENDING);
        job.setEntityType(EntityType.ATTRIBUTE);

        job = bulkUploadJobRepository.save(job);

        attributeBulkProcessor.process(job.getId());

        return job.getId();
    }


    @Override
    public Attribute create(CreateAttributeRequest request) {

        String slug = SlugGenerator.generate(request.getName());

        if (attributeRepository.existsBySlug(slug)) {
            throw new IllegalStateException("Attribute already exists");
        }

        Attribute attribute = new Attribute();
        attribute.setName(request.getName());
        attribute.setSlug(slug);
        attribute.setDataType(AttributeType.valueOf(request.getDataType()));
        attribute.setIsFilterable(request.getIsFilterable());
        attribute.setIsSearchable(request.getIsSearchable());
        attribute.setIsVariant(request.getIsVariant());
        attribute.setIsRequired(request.getIsRequired());
        attribute.setIsActive(true);

        return attributeRepository.save(attribute);
    }

    @Override
    public Attribute update(Long id, UpdateAttributeRequest request) {

        Attribute attribute = getById(id);

        attribute.setName(request.getName());
        attribute.setIsFilterable(request.getIsFilterable());
        attribute.setIsSearchable(request.getIsSearchable());
        attribute.setIsVariant(request.getIsVariant());
        attribute.setIsRequired(request.getIsRequired());
        attribute.setIsActive(request.getIsActive());

        return attributeRepository.save(attribute);
    }

    @Override
    public Attribute getById(Long id) {
        return attributeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Attribute not found"));
    }

    @Override
    public Page<Attribute> getAll(Pageable pageable) {
        return attributeRepository.findByIsActiveTrue(pageable);
    }

    @Override
    public void delete(Long id) {
        Attribute attribute = getById(id);
        attribute.setIsActive(false);
        attributeRepository.save(attribute);
    }

    @Override
    @Transactional
    public Long uploadAndProcessAsync(MultipartFile file) throws IOException {

        String s3Key = "attribute/upload/" + System.currentTimeMillis() + ".csv";

        s3Service.uploadFile(
                s3Key,
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
        );

        BulkUploadJob job = new BulkUploadJob();
        job.setFileUrl(s3Key);
        job.setStatus(JobStatus.PENDING);
        job.setEntityType(EntityType.ATTRIBUTE);

        job = bulkUploadJobRepository.save(job);

        processAsync(job.getId());

        return job.getId();
    }

//    @Override
//    @Transactional
//    public Long startBulkJob(String s3Key) {
//
//        BulkUploadJob job = new BulkUploadJob();
//        job.setFileUrl(s3Key);
//        job.setStatus(JobStatus.PENDING);
//        job.setEntityType(EntityType.ATTRIBUTE);
//
//        job = bulkUploadJobRepository.save(job);
//
//        processAsync(job.getId());
//
//        return job.getId();
//    }

    @Async
    @Transactional
    public void processAsync(Long jobId) {

        BulkUploadJob job = bulkUploadJobRepository.findById(jobId).orElseThrow();

        job.setStatus(JobStatus.PROCESSING);
        bulkUploadJobRepository.save(job);

        List<String> failedRows = new ArrayList<>();
        List<Attribute> batch = new ArrayList<>();

        int total = 0;
        int success = 0;
        int failure = 0;

        try (InputStream inputStream = s3Service.download(job.getFileUrl())) {

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream));

            String line;

            while ((line = reader.readLine()) != null) {

                total++;

                try {

                    String[] data = line.split(",");

                    String name = data[0].trim();
                    String type = data[1].trim();
                    Boolean isFilterable = Boolean.parseBoolean(data[2]);
                    Boolean isSearchable = Boolean.parseBoolean(data[3]);
                    Boolean isVariant = Boolean.parseBoolean(data[4]);
                    Boolean isRequired = Boolean.parseBoolean(data[5]);

                    Attribute attribute = new Attribute();
                    attribute.setName(name);
                    attribute.setSlug(generateUniqueSlug(name));
                    attribute.setDataType(AttributeType.valueOf(type));
                    attribute.setIsFilterable(isFilterable);
                    attribute.setIsSearchable(isSearchable);
                    attribute.setIsVariant(isVariant);
                    attribute.setIsRequired(isRequired);
                    attribute.setIsActive(true);

                    batch.add(attribute);
                    success++;

                    if (batch.size() == 500) {
                        attributeRepository.saveAll(batch);
                        batch.clear();
                    }

                } catch (Exception ex) {
                    failure++;
                    failedRows.add(line + "," + ex.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                attributeRepository.saveAll(batch);
            }

            if (!failedRows.isEmpty()) {

                String failedKey =
                        "attribute/failed_result/attribute_failed_" + jobId + ".csv";

                generateFailedCsvAndUpload(failedRows, failedKey);

                job.setResultFileUrl(failedKey);
            }

            job.setTotalRecords(total);
            job.setSuccessCount(success);
            job.setFailureCount(failure);
            job.setStatus(JobStatus.COMPLETED);

        } catch (Exception ex) {

            job.setStatus(JobStatus.FAILED);
        }

        bulkUploadJobRepository.save(job);
    }

    private void generateFailedCsvAndUpload(List<String> failedRows, String key) {

        try {

            StringBuilder builder = new StringBuilder();
            builder.append("name,type,isFilterable,isSearchable,isVariant,isRequired,error\n");

            for (String row : failedRows) {
                builder.append(row).append("\n");
            }

            byte[] bytes = builder.toString().getBytes();

            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(bytes);

            s3Service.uploadFile(
                    key,
                    inputStream,
                    bytes.length,
                    "text/csv"
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate failed CSV");
        }
    }

    private String generateUniqueSlug(String name) {

        String baseSlug = SlugGenerator.generate(name);
        String slug = baseSlug;
        int counter = 1;

        while (attributeRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }


}
