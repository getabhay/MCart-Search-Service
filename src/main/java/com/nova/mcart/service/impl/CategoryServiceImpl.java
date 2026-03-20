package com.nova.mcart.service.impl;

import com.nova.mcart.common.util.SlugGenerator;
import com.nova.mcart.dto.request.CategoryCreateRequest;
import com.nova.mcart.dto.response.BreadcrumbItemResponse;
import com.nova.mcart.dto.response.CategoryResponse;
import com.nova.mcart.dto.response.CategoryTreeItemResponse;
import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.Category;
import com.nova.mcart.entity.enums.EntityType;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.repository.BulkUploadJobRepository;
import com.nova.mcart.repository.CategoryRepository;
import com.nova.mcart.service.CategoryService;
import com.nova.mcart.service.aws.S3Service;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final S3Service s3Service;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               BulkUploadJobRepository bulkUploadJobRepository,
                               S3Service s3Service) {
        this.categoryRepository = categoryRepository;
        this.bulkUploadJobRepository = bulkUploadJobRepository;
        this.s3Service = s3Service;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {

        String slug = SlugGenerator.generate(request.getName());

        if (categoryRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Category already exists");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(slug);
        category.setIsLeaf(true);

        // Parent handling
        if (request.getParentId() != null) {

            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent not found"));

            category.setParent(parent);
            parent.setIsLeaf(false);
            categoryRepository.save(parent);
        }

        category = categoryRepository.save(category);

        // Path calculation
        if (category.getParent() == null) {

            category.setPath(String.valueOf(category.getId()));

        } else {

            category.setPath(
                    category.getParent().getPath() + "/" + category.getId()
            );
        }

        category = categoryRepository.save(category);

        return mapToResponse(category);
    }

    @Override
    public CategoryResponse getBySlug(String slug) {

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        return mapToResponse(category);
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        return mapToResponse(category);
    }

    @Override
    public CategoryResponse updateCategoryStatus(Long id, Boolean staus) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        category.setIsActive(staus);
        category = categoryRepository.save(category);

        return mapToResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with children");
        }

        category.setIsActive(false);
        categoryRepository.save(category);

        // If parent has no more children → mark as leaf
        Category parent = category.getParent();
        if (parent != null && parent.getChildren().stream().noneMatch(Category::getIsActive)) {
            parent.setIsLeaf(true);
            categoryRepository.save(parent);
        }
    }

    @Override
    public List<BreadcrumbItemResponse> getBreadcrumb(Long categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        String[] pathIds = category.getPath().split("/");

        List<Long> ids = new ArrayList<>();
        for (String idStr : pathIds) {
            ids.add(Long.valueOf(idStr));
        }

        List<Category> categories = categoryRepository.findAllById(ids);

        return ids.stream()
                .map(id -> categories.stream()
                        .filter(cat -> cat.getId().equals(id))
                        .findFirst()
                        .orElseThrow())
                .map(cat -> {
                    BreadcrumbItemResponse item = new BreadcrumbItemResponse();
                    item.setId(cat.getId());
                    item.setName(cat.getName());
                    return item;
                })
                .toList();
    }


    private CategoryResponse mapToResponse(Category category) {

        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setSlug(category.getSlug());
        response.setIsLeaf(category.getIsLeaf());

        if (category.getParent() != null) {
            response.setParentId(category.getParent().getId());
        }
        response.setIsActive(Objects.nonNull(category.getIsActive()) ? category.getIsActive() : false);

        return response;
    }

    @Override
    public List<CategoryTreeItemResponse> getTree() {

        List<Category> categories =
                categoryRepository.findAllByIsActiveTrueOrderByPathAsc();

        List<CategoryTreeItemResponse> response = new ArrayList<>();

        for (Category category : categories) {

            CategoryTreeItemResponse item = new CategoryTreeItemResponse();
            item.setId(category.getId());
            item.setName(category.getName());
            item.setSlug(category.getSlug());
            item.setIsLeaf(category.getIsLeaf());

            if (category.getParent() != null) {
                item.setParentId(category.getParent().getId());
            }

            response.add(item);
        }

        return response;
    }

    @Override
    public List<CategoryResponse> getSubtree(Long categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        List<Category> subtree =
                categoryRepository.findByPathStartingWithAndIsActiveTrue(category.getPath());

        List<CategoryResponse> response = new ArrayList<>();

        for (Category cat : subtree) {
            response.add(mapToResponse(cat));
        }

        return response;
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryCreateRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        // Update name + slug
        category.setName(request.getName());
        category.setSlug(SlugGenerator.generate(request.getName()));

        Category oldParent = category.getParent();
        Category newParent = null;

        if (request.getParentId() != null) {

            newParent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent not found"));

            // Prevent circular hierarchy
            if (newParent.getPath().startsWith(category.getPath())) {
                throw new IllegalStateException("Cannot move category inside its subtree");
            }

            category.setParent(newParent);
            newParent.setIsLeaf(false);
            categoryRepository.save(newParent);

        } else {
            category.setParent(null);
        }

        category = categoryRepository.save(category);

        // Recalculate path
        String newPath = (category.getParent() == null)
                ? String.valueOf(category.getId())
                : category.getParent().getPath() + "/" + category.getId();

        updateSubtreePaths(category, newPath);

        // Update old parent leaf
        if (oldParent != null &&
                oldParent.getChildren().stream().allMatch(c -> !c.getIsActive())) {
            oldParent.setIsLeaf(true);
            categoryRepository.save(oldParent);
        }

        return mapToResponse(category);
    }

    private void updateSubtreePaths(Category category, String newPath) {

        String oldPath = category.getPath();

        List<Category> subtree =
                categoryRepository.findByPathStartingWithAndIsActiveTrue(oldPath);

        for (Category cat : subtree) {

            String updatedPath = cat.getPath().replaceFirst(oldPath, newPath);
            cat.setPath(updatedPath);
        }

        categoryRepository.saveAll(subtree);
    }

    @Override
    public Page<CategoryResponse> getAll(String search, int page, int size) {

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        Page<Category> categoryPage;

        if (search != null && !search.isBlank()) {

            categoryPage = categoryRepository
                    .findByIsActiveTrueAndNameContainingIgnoreCase(search, pageable);

        } else {

            categoryPage = categoryRepository
                    .findByIsActiveTrue(pageable);
        }

        return categoryPage.map(this::mapToResponse);
    }


    @Override
    @Transactional
    public Long startBulkUpload(String s3Key) {

        BulkUploadJob job = new BulkUploadJob();
        job.setEntityType(EntityType.CATEGORY);
        job.setFileUrl(s3Key);
        job.setStatus(JobStatus.PENDING);

        job = bulkUploadJobRepository.save(job);

        processAsync(job.getId());

        return job.getId();
    }

    @Async
    @Transactional
    public void processAsync(Long jobId) {

        BulkUploadJob job = bulkUploadJobRepository.findById(jobId).orElseThrow();

        job.setStatus(JobStatus.PROCESSING);
        bulkUploadJobRepository.save(job);

        List<String> failedRows = new ArrayList<>();

        int total = 0;
        int success = 0;
        int failure = 0;

        try (InputStream inputStream = s3Service.download(job.getFileUrl())) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            List<String[]> rows = new ArrayList<>();
            String line;
            int rowNumber = 0;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (rowNumber == 1) continue; // skip header

                total++;
                rows.add(line.split(","));
            }

            Map<String, String> pending = new LinkedHashMap<>();

            for (String[] row : rows) {
                String name = row[0].trim();
                String parentSlug = row.length > 1 ? row[1].trim() : null;
                pending.put(name, parentSlug);
            }

            boolean progressMade;

            do {
                progressMade = false;

                Iterator<Map.Entry<String, String>> iterator = pending.entrySet().iterator();

                while (iterator.hasNext()) {

                    Map.Entry<String, String> entry = iterator.next();

                    String name = entry.getKey();
                    String parentSlug = entry.getValue();

                    try {

                        if (parentSlug == null || parentSlug.isBlank()) {

                            createInternal(name, null);

                        } else {

                            Category parent =
                                    categoryRepository.findBySlug(parentSlug).orElse(null);

                            if (parent == null) {
                                continue; // wait for parent
                            }

                            createInternal(name, parent.getId());
                        }

                        success++;
                        iterator.remove();
                        progressMade = true;

                    } catch (Exception ex) {

                        failure++;
                        failedRows.add(name + "," + parentSlug + "," + ex.getMessage());
                        iterator.remove();
                    }
                }

            } while (progressMade && !pending.isEmpty());

            // Remaining unresolved records = failure
            if (!pending.isEmpty()) {
                for (Map.Entry<String, String> entry : pending.entrySet()) {
                    failure++;
                    failedRows.add(entry.getKey() + "," + entry.getValue() + ",Parent not found");
                }
            }

            if (!failedRows.isEmpty()) {

                String failedKey = "category/failed_result/category_failed_" + jobId + ".csv";
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
            builder.append("name,parent_slug,error\n");

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

    private void createInternal(String name, Long parentId) {

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName(name);
        request.setParentId(parentId);

        create(request); // reuse existing logic
    }

}
