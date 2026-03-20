package com.nova.mcart.validation.impl;

import com.nova.mcart.dto.request.ProductCreateRequest;
import com.nova.mcart.entity.Category;
import com.nova.mcart.repository.AttributeRepository;
import com.nova.mcart.repository.BrandRepository;
import com.nova.mcart.repository.CategoryRepository;
import com.nova.mcart.repository.ProductVariantRepository;
import com.nova.mcart.validation.ProductValidator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductValidatorImpl implements ProductValidator {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeRepository attributeRepository;
    private final ProductVariantRepository variantRepository;

    @Override
    public void validateCreate(ProductCreateRequest request) {

        validateBrand(request.getBrandId());
        validateCategory(request.getCategoryId());
        validateVariants(request.getVariants());
        validateProductAttributes(request.getAttributes());
        validateVariantAttributes(request.getVariants());
    }

    private void validateBrand(Long brandId) {

        if (brandId == null) {
            throw new IllegalArgumentException("Brand id is required");
        }

        if (!brandRepository.existsById(brandId)) {
            throw new IllegalArgumentException("Brand does not exist: " + brandId);
        }
    }

    private Category validateCategory(Long categoryId) {

        if (categoryId == null) {
            throw new IllegalArgumentException("Category id is required");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Category does not exist: " + categoryId));

        if (Boolean.FALSE.equals(category.getIsLeaf())) {
            throw new IllegalArgumentException("Product must belong to leaf category");
        }

        return category;
    }

    private void validateVariants(List<ProductCreateRequest.VariantRequest> variants) {

        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("At least one variant is required");
        }

        Set<String> requestSkus = new HashSet<>();

        for (ProductCreateRequest.VariantRequest variant : variants) {

            if (variant.getSku() == null || variant.getSku().isBlank()) {
                throw new IllegalArgumentException("SKU is required");
            }

            if (variant.getMrp() == null || variant.getSellingPrice() == null) {
                throw new IllegalArgumentException(
                        "MRP and selling price are required for SKU: " + variant.getSku());
            }

            if (variant.getSellingPrice().compareTo(variant.getMrp()) > 0) {
                throw new IllegalArgumentException(
                        "Selling price cannot be greater than MRP for SKU: " + variant.getSku());
            }

            if (!requestSkus.add(variant.getSku())) {
                throw new IllegalArgumentException("Duplicate SKU in request: " + variant.getSku());
            }

            if (variantRepository.existsBySku(variant.getSku())) {
                throw new IllegalArgumentException("SKU already exists: " + variant.getSku());
            }
        }
    }

    private void validateProductAttributes(
            List<ProductCreateRequest.ProductAttributeRequest> attributes) {

        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        Set<String> attrSlugs = new HashSet<>();

        for (ProductCreateRequest.ProductAttributeRequest attr : attributes) {

            String slug = attr.getAttributeSlug();

            if (slug == null || slug.isBlank()) {
                throw new IllegalArgumentException("Product attribute slug is required");
            }

            String normalized = slug.trim().toLowerCase();

            if (!attrSlugs.add(normalized)) {
                throw new IllegalArgumentException("Duplicate product attribute: " + slug);
            }

            if (attributeRepository.findBySlugAndIsActiveTrue(slug).isEmpty()) {
                throw new IllegalArgumentException("Attribute does not exist: " + slug);
            }
        }
    }

    private void validateVariantAttributes(
            List<ProductCreateRequest.VariantRequest> variants) {

        if (variants == null || variants.isEmpty()) {
            return;
        }

        for (ProductCreateRequest.VariantRequest variant : variants) {

            List<ProductCreateRequest.VariantAttributeRequest> attrs =
                    variant.getAttributes();

            if (attrs == null || attrs.isEmpty()) {
                continue;
            }

            Set<Long> attrIds = new HashSet<>();

            for (ProductCreateRequest.VariantAttributeRequest attr : attrs) {

                Long attributeId = attr.getAttributeId();

                if (attributeId == null) {
                    throw new IllegalArgumentException(
                            "Variant attributeId is required for SKU: " + variant.getSku());
                }

                if (!attrIds.add(attributeId)) {
                    throw new IllegalArgumentException(
                            "Duplicate variant attribute for SKU: "
                                    + variant.getSku() + " attrId=" + attributeId);
                }

                if (!attributeRepository.existsById(attributeId)) {
                    throw new IllegalArgumentException("Attribute does not exist: " + attributeId);
                }
            }
        }
    }
}
