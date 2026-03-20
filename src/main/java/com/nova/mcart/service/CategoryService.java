package com.nova.mcart.service;

import com.nova.mcart.dto.request.CategoryCreateRequest;
import com.nova.mcart.dto.response.BreadcrumbItemResponse;
import com.nova.mcart.dto.response.CategoryResponse;
import com.nova.mcart.dto.response.CategoryTreeItemResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public interface CategoryService {

    CategoryResponse create(CategoryCreateRequest request);

    CategoryResponse getBySlug(String slug);

    CategoryResponse getCategoryById(Long id);
    CategoryResponse updateCategoryStatus(Long id, Boolean status);

    void delete(Long id);

    List<BreadcrumbItemResponse> getBreadcrumb(Long categoryId);

    List<CategoryTreeItemResponse> getTree();

    List<CategoryResponse> getSubtree(Long categoryId);

    CategoryResponse update(Long id, CategoryCreateRequest request);

    Page<CategoryResponse> getAll(String search, int page, int size);

    Long startBulkUpload(String s3Key);

}
