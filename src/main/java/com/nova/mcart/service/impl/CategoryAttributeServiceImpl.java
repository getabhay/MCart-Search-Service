package com.nova.mcart.service.impl;

import com.nova.mcart.common.bulk.CategoryAttributeBulkProcessor;
import com.nova.mcart.dto.request.CreateCategoryAttributeRequest;
import com.nova.mcart.entity.Attribute;
import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.Category;
import com.nova.mcart.entity.CategoryAttribute;
import com.nova.mcart.entity.compositeKey.CategoryAttributeId;
import com.nova.mcart.entity.enums.EntityType;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.repository.AttributeRepository;
import com.nova.mcart.repository.BulkUploadJobRepository;
import com.nova.mcart.repository.CategoryAttributeRepository;
import com.nova.mcart.repository.CategoryRepository;
import com.nova.mcart.service.CategoryAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryAttributeServiceImpl implements CategoryAttributeService {

    private final CategoryRepository categoryRepository;
    private final AttributeRepository attributeRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;

    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final CategoryAttributeBulkProcessor categoryAttributeBulkProcessor;

    @Override
    @Transactional
    public Long startBulkJob(String s3Key) {

        BulkUploadJob job = new BulkUploadJob();
        job.setFileUrl(s3Key);
        job.setStatus(JobStatus.PENDING);
        job.setEntityType(EntityType.CATEGORY_ATTRIBUTE);

        job = bulkUploadJobRepository.save(job);

        categoryAttributeBulkProcessor.process(job.getId());

        return job.getId();
    }

    @Override
    public CategoryAttribute map(CreateCategoryAttributeRequest request) {
        // existing code unchanged
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalStateException("Category not found"));

        Attribute attribute = attributeRepository.findById(request.getAttributeId())
                .orElseThrow(() -> new IllegalStateException("Attribute not found"));

        if (request.getIsVariantLevel() && !attribute.getIsVariant()) {
            throw new IllegalStateException("Attribute is not configured as variant-enabled");
        }

        CategoryAttributeId id = new CategoryAttributeId(category.getId(), attribute.getId());

        if (categoryAttributeRepository.existsById(id)) {
            throw new IllegalStateException("Attribute already mapped to this category");
        }

        CategoryAttribute mapping = new CategoryAttribute();
        mapping.setId(id);
        mapping.setCategory(category);
        mapping.setAttribute(attribute);
        mapping.setIsVariantLevel(request.getIsVariantLevel());
        mapping.setIsRequired(request.getIsRequired());
        mapping.setIsFilterable(request.getIsFilterable());

        return categoryAttributeRepository.save(mapping);
    }

    @Override
    public Page<CategoryAttribute> getByCategory(Long categoryId, Pageable pageable) {
        // existing code unchanged
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalStateException("Category not found");
        }
        return categoryAttributeRepository.findByIdCategoryId(categoryId, pageable);
    }
}
