package com.nova.mcart.controller;

import com.nova.mcart.dto.request.CategoryCreateRequest;
import com.nova.mcart.dto.response.BreadcrumbItemResponse;
import com.nova.mcart.dto.response.CategoryResponse;
import com.nova.mcart.dto.response.CategoryTreeItemResponse;
import com.nova.mcart.service.CategoryService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Create Category
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @RequestBody CategoryCreateRequest request) {

        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update Category
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @RequestBody CategoryCreateRequest request) {

        CategoryResponse response = categoryService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft Delete Category
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get Category by Slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryResponse> getBySlug(
            @PathVariable String slug) {

        return ResponseEntity.ok(categoryService.getBySlug(slug));
    }

    /**
     * Get Category By Id
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(
            @PathVariable Long id) {

        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<CategoryResponse> updateCategoryStatus(
            @PathVariable Long id, @RequestParam Boolean status) {

        return ResponseEntity.ok(categoryService.updateCategoryStatus(id, status));
    }

    /**
     * Get Full Tree (Flat List)
     */
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeItemResponse>> getTree() {

        return ResponseEntity.ok(categoryService.getTree());
    }

    /**
     * Get Breadcrumb
     */
    @GetMapping("/{id}/breadcrumb")
    public ResponseEntity<List<BreadcrumbItemResponse>> getBreadcrumb(
            @PathVariable Long id) {

        return ResponseEntity.ok(categoryService.getBreadcrumb(id));
    }

    /**
     * Get Subtree
     */
    @GetMapping("/{id}/subtree")
    public ResponseEntity<List<CategoryResponse>> getSubtree(
            @PathVariable Long id) {

        return ResponseEntity.ok(categoryService.getSubtree(id));
    }

    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(
                categoryService.getAll(search, page, size)
        );
    }

    @GetMapping("/bulk-upload/start")
    public ResponseEntity<Long> startBulkUpload(
            @RequestParam String s3Key) {

        Long jobId = categoryService.startBulkUpload(s3Key);
        return ResponseEntity.ok(jobId);
    }


}
