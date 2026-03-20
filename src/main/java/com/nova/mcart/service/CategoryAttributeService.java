package com.nova.mcart.service;

import com.nova.mcart.dto.request.CreateCategoryAttributeRequest;
import com.nova.mcart.entity.CategoryAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryAttributeService {

    CategoryAttribute map(CreateCategoryAttributeRequest request);

    Page<CategoryAttribute> getByCategory(Long categoryId, Pageable pageable);

    Long startBulkJob(String s3Key);
}
