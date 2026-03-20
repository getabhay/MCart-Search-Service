package com.nova.mcart.service.impl;

import com.nova.mcart.common.bulk.BrandBulkProcessor;
import com.nova.mcart.common.util.SlugGenerator;
import com.nova.mcart.dto.request.BrandCreateRequest;
import com.nova.mcart.dto.request.BrandUpdateRequest;
import com.nova.mcart.dto.response.BrandResponse;
import com.nova.mcart.entity.Brand;
import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.enums.EntityType;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.repository.BrandRepository;
import com.nova.mcart.repository.BulkUploadJobRepository;
import com.nova.mcart.service.BrandService;
import com.nova.mcart.service.aws.S3Service;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final S3Service s3Service;
    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final BrandBulkProcessor brandBulkProcessor;

    @Override
    @Transactional
    public Long startBulkJob(String s3Key) {

        BulkUploadJob job = new BulkUploadJob();
        job.setFileUrl(s3Key);
        job.setStatus(JobStatus.PENDING);
        job.setEntityType(EntityType.BRAND);

        job = bulkUploadJobRepository.save(job);

        brandBulkProcessor.process(job.getId());

        return job.getId();
    }

    @Override
    @Transactional
    public BrandResponse createBrand(BrandCreateRequest request) {

        if (brandRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Brand already exists");
        }

        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setSlug(generateUniqueSlug(request.getName()));

        brand = brandRepository.save(brand);

        return mapToResponse(brand);
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, BrandUpdateRequest request) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        brand.setName(request.getName());
        brand.setSlug(generateUniqueSlug(request.getName()));

        brand = brandRepository.save(brand);

        return mapToResponse(brand);
    }

    @Override
    public BrandResponse getBrandById(Long id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        return mapToResponse(brand);
    }

    @Override
    public BrandResponse getBrandBySlug(String slug) {

        Brand brand = brandRepository.findAll()
                .stream()
                .filter(b -> b.getSlug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        return mapToResponse(brand);
    }

    @Override
    public Page<BrandResponse> getAllBrands(String search, Pageable pageable) {

        Page<Brand> page;

        if (search != null && !search.isBlank()) {
            page = (Page<Brand>) brandRepository.findAll(pageable)
                    .map(b -> b)
                    .filter(b -> b.getName().toLowerCase().contains(search.toLowerCase()));
        } else {
            page = brandRepository.findAll(pageable);
        }

        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        brand.setIsActive(false);
        brandRepository.save(brand);
    }

    @Override
    @Transactional
    public Long uploadAndProcessAsync(MultipartFile file) throws IOException {

        String s3Key = "brand/upload/" + System.currentTimeMillis() + ".csv";

        s3Service.uploadFile(
                s3Key,
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
        );

        BulkUploadJob job = new BulkUploadJob();
        job.setFileUrl(s3Key);
        job.setStatus(JobStatus.PENDING);
        job.setEntityType(EntityType.BRAND);

        job = bulkUploadJobRepository.save(job);

        processAsync(job.getId());

        return job.getId();
    }

    private BrandResponse mapToResponse(Brand brand) {

        BrandResponse response = new BrandResponse();
        response.setId(brand.getId());
        response.setName(brand.getName());
        response.setSlug(brand.getSlug());
        return response;
    }

    @Async
    @Transactional
    public void processAsync(Long jobId) {

        BulkUploadJob job = bulkUploadJobRepository.findById(jobId).orElseThrow();

        job.setStatus(JobStatus.PROCESSING);
        bulkUploadJobRepository.save(job);

        try (InputStream inputStream = s3Service.download(job.getFileUrl())){

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            List<Brand> batch = new ArrayList<>();
            List<String> failedRows = new ArrayList<>();

            int total = 0;
            int success = 0;
            int failure = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                try {
                    String name = line.trim();

                    Brand brand = new Brand();
                    brand.setName(name);
                    brand.setSlug(generateUniqueSlug(name));

                    batch.add(brand);
                    success++;

                    if (batch.size() == 500) {
                        brandRepository.saveAll(batch);
                        batch.clear();
                    }

                } catch (Exception ex) {
                    failure++;
                    failedRows.add(line + "," + ex.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                brandRepository.saveAll(batch);
            }

            if(!failedRows.isEmpty()) {
                // Generate failed CSV
                String failedKey = "brand/failed_result/brand_failed_" + jobId + ".csv";
                generateFailedCsvAndUpload(failedRows, failedKey);
                job.setResultFileUrl(failedKey);
            }

            job.setStatus(JobStatus.COMPLETED);
            job.setTotalRecords(total);
            job.setSuccessCount(success);
            job.setFailureCount(failure);

        } catch (Exception ex) {

            job.setStatus(JobStatus.FAILED);
        } finally {
            bulkUploadJobRepository.save(job);
        }
    }

    private void generateFailedCsvAndUpload(List<String> failedRows, String key) {

        try {

            StringBuilder builder = new StringBuilder();
            builder.append("name\n");

            for (String row : failedRows) {
                builder.append(row).append("\n");
            }

            byte[] bytes = builder.toString().getBytes();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

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

        while (brandRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }

//    @Override
//    @Transactional
//    public Long startBulkJob(String s3Key) {
//
//        BulkUploadJob job = new BulkUploadJob();
//        job.setFileUrl(s3Key);
//        job.setStatus(JobStatus.PENDING);
//        job.setEntityType(EntityType.BRAND);
//
//        job = bulkUploadJobRepository.save(job);
//
//        processAsync(job.getId());
//
//        return job.getId();
//    }

}
