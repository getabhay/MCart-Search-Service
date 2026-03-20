package com.nova.mcart.service.impl;

import com.nova.mcart.common.bulk.ProductBulkProcessor;
import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.enums.EntityType;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.repository.BulkUploadJobRepository;
import com.nova.mcart.service.ProductBulkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductBulkServiceImpl implements ProductBulkService {

    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final ProductBulkProcessor productBulkProcessor;

    @Override
    @Transactional
    public Long startBulkJob(String s3Key) {
        BulkUploadJob job = new BulkUploadJob();
        job.setFileUrl(s3Key);
        job.setStatus(JobStatus.PENDING);
        job.setEntityType(EntityType.PRODUCT);
        job = bulkUploadJobRepository.save(job);
        productBulkProcessor.process(job.getId());
        return job.getId();
    }
}
