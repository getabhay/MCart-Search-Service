package com.nova.mcart.service.impl;

import com.nova.mcart.common.bulk.AttributeValueBulkProcessor;
import com.nova.mcart.common.util.SlugGenerator;
import com.nova.mcart.dto.request.CreateAttributeValueRequest;
import com.nova.mcart.entity.Attribute;
import com.nova.mcart.entity.AttributeValue;
import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.enums.EntityType;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.repository.AttributeRepository;
import com.nova.mcart.repository.AttributeValueRepository;
import com.nova.mcart.repository.BulkUploadJobRepository;
import com.nova.mcart.service.AttributeValueService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttributeValueServiceImpl implements AttributeValueService {

    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeValueBulkProcessor attributeValueBulkProcessor;
    private final BulkUploadJobRepository bulkUploadJobRepository;

    @Override
    @Transactional
    public Long startBulkJob(String s3Key) {

        BulkUploadJob job = new BulkUploadJob();
        job.setFileUrl(s3Key);
        job.setStatus(JobStatus.PENDING);
        job.setEntityType(EntityType.ATTRIBUTE_VALUE);

        job = bulkUploadJobRepository.save(job);

        attributeValueBulkProcessor.process(job.getId());

        return job.getId();
    }


    @Override
    public AttributeValue create(CreateAttributeValueRequest request) {

        Attribute attribute = attributeRepository.findById(request.getAttributeId())
                .orElseThrow(() -> new IllegalStateException("Attribute not found"));

        if (!attribute.getDataType().supportsPredefinedValues()) {
            throw new IllegalStateException(
                    "Attribute values allowed only for SELECT or MULTI_SELECT type"
            );
        }

        String slug = SlugGenerator.generate(request.getValue());

        AttributeValue value = new AttributeValue();
        value.setAttribute(attribute);
        value.setValue(request.getValue());
        value.setSlug(slug);
        value.setSortOrder(request.getSortOrder());
        value.setIsActive(true);

        return attributeValueRepository.save(value);
    }

    @Override
    public Page<AttributeValue> getByAttribute(Long attributeId, Pageable pageable) {
        return attributeValueRepository.findByAttributeIdAndIsActiveTrue(attributeId, pageable);
    }

}
