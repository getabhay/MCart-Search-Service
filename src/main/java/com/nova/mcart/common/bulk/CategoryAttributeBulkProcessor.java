package com.nova.mcart.common.bulk;

import com.nova.mcart.entity.Attribute;
import com.nova.mcart.entity.Category;
import com.nova.mcart.entity.CategoryAttribute;
import com.nova.mcart.entity.compositeKey.CategoryAttributeId;
import com.nova.mcart.repository.AttributeRepository;
import com.nova.mcart.repository.CategoryAttributeRepository;
import com.nova.mcart.repository.CategoryRepository;
import com.nova.mcart.service.aws.S3Service;
import com.nova.mcart.service.impl.BulkUploadJobTxService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CategoryAttributeBulkProcessor
        extends AbstractBulkUploadProcessor<CategoryAttribute> {

    private final CategoryRepository categoryRepository;
    private final AttributeRepository attributeRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;

    public CategoryAttributeBulkProcessor(
            BulkUploadJobTxService bulkUploadJobTxService,
            S3Service s3Service,
            CategoryRepository categoryRepository,
            AttributeRepository attributeRepository,
            CategoryAttributeRepository categoryAttributeRepository) {

        super(bulkUploadJobTxService, s3Service);
        this.categoryRepository = categoryRepository;
        this.attributeRepository = attributeRepository;
        this.categoryAttributeRepository = categoryAttributeRepository;
    }

    @Override
    protected CategoryAttribute parseRow(String line) {

        // CSV FORMAT:
        // category_slug,attribute_slug,is_variant_level,is_required,is_filterable

        String[] data = line.split(",");

        if (data.length < 5) {
            throw new IllegalArgumentException("Invalid column count");
        }

        String categorySlug = data[0].trim();
        String attributeSlug = data[1].trim();

        Boolean isVariantLevel = Boolean.valueOf(data[2].trim());
        Boolean isRequired = Boolean.valueOf(data[3].trim());
        Boolean isFilterable = Boolean.valueOf(data[4].trim());

        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categorySlug));

        Attribute attribute = attributeRepository.findBySlugAndIsActiveTrue(attributeSlug)
                .orElseThrow(() -> new IllegalArgumentException("Attribute not found: " + attributeSlug));

        if (Boolean.TRUE.equals(isVariantLevel) && Boolean.FALSE.equals(attribute.getIsVariant())) {
            throw new IllegalArgumentException("Attribute is not variant-enabled: " + attributeSlug);
        }

        CategoryAttributeId id = new CategoryAttributeId(category.getId(), attribute.getId());

        if (categoryAttributeRepository.existsById(id)) {
            throw new IllegalArgumentException("Mapping already exists");
        }

        CategoryAttribute mapping = new CategoryAttribute();
        mapping.setId(id);
        mapping.setCategory(category);
        mapping.setAttribute(attribute);
        mapping.setIsVariantLevel(isVariantLevel);
        mapping.setIsRequired(isRequired);
        mapping.setIsFilterable(isFilterable);

        return mapping;
    }

    @Override
    protected void saveBatch(List<CategoryAttribute> batch) {
        categoryAttributeRepository.saveAll(batch);
    }

    @Override
    protected String getFailedFolder() {
        return "category-attribute/failed_result";
    }

    @Override
    protected String getFailedHeader() {
        return "category_slug,attribute_slug,is_variant_level,is_required,is_filterable,error";
    }
}
