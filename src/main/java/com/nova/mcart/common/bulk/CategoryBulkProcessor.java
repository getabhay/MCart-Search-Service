package com.nova.mcart.common.bulk;

import com.nova.mcart.dto.request.CategoryCreateRequest;
import com.nova.mcart.entity.Category;
import com.nova.mcart.repository.CategoryRepository;
import com.nova.mcart.service.CategoryService;
import com.nova.mcart.service.aws.S3Service;
import com.nova.mcart.service.impl.BulkUploadJobTxService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CategoryBulkProcessor
        extends AbstractBulkUploadProcessor<Category> {

    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    public CategoryBulkProcessor(
            BulkUploadJobTxService bulkUploadJobTxService,
            S3Service s3Service,
            CategoryRepository categoryRepository,
            CategoryService categoryService) {

        super(bulkUploadJobTxService, s3Service);
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
    }

    @Override
    protected Category parseRow(String line) {

        // CSV FORMAT:
        // name,parent_slug

        String[] data = line.split(",");

        String name = data[0].trim();
        String parentSlug =
                data.length > 1 ? data[1].trim() : null;

        CategoryCreateRequest request =
                new CategoryCreateRequest();

        request.setName(name);

        if (parentSlug != null && !parentSlug.isBlank()) {

            Category parent =
                    categoryRepository.findBySlug(parentSlug)
                            .orElseThrow(() ->
                                    new IllegalArgumentException("Parent not found"));

            request.setParentId(parent.getId());
        }

        // Reuse existing logic
        categoryService.create(request);

        return null; // not used
    }

    @Override
    protected void saveBatch(List<Category> batch) {
        // Not used because create() handles save
    }

    @Override
    protected String getFailedFolder() {
        return "category/failed_result";
    }

    @Override
    protected String getFailedHeader() {
        return "name,parent_slug,error";
    }
}
