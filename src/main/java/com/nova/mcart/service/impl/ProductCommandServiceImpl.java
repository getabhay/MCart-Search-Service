package com.nova.mcart.service.impl;

import com.nova.mcart.common.ProductMapper;
import com.nova.mcart.common.util.SlugGenerator;
import com.nova.mcart.dto.request.ProductCreateRequest;
import com.nova.mcart.dto.response.ProductResponse;
import com.nova.mcart.entity.*;
import com.nova.mcart.entity.enums.ProductStatus;
import com.nova.mcart.repository.*;
import com.nova.mcart.service.ProductCommandService;
import com.nova.mcart.validation.ProductValidator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductCommandServiceImpl implements ProductCommandService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;

    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final VariantAttributeRepository variantAttributeRepository;

    private final ProductRatingSummaryRepository ratingSummaryRepository;
    private final ProductImageRepository productImageRepository;

    private final ProductValidator productValidator;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {

        productValidator.validateCreate(request);

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid brand"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category"));

        Product product = new Product();
        product.setName(request.getName());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setBrand(brand);
        product.setCategory(category);
        product.setStatus(request.getStatus());
        product.setSlug(generateUniqueSlug(request.getName()));
        product.setIsActive(true);

        product = productRepository.save(product);

        // Product attributes (slug/value)
        if (request.getAttributes() != null) {
            for (ProductCreateRequest.ProductAttributeRequest attrReq : request.getAttributes()) {

                Attribute attribute = attributeRepository.findBySlugAndIsActiveTrue(attrReq.getAttributeSlug())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid attribute: " + attrReq.getAttributeSlug()));

                AttributeValue attributeValue = attributeValueRepository
                        .findByAttributeIdAndValue(attribute.getId(), attrReq.getValue())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Invalid attribute value for " + attribute.getSlug() + ": " + attrReq.getValue()
                        ));

                ProductAttribute productAttribute = new ProductAttribute();
                productAttribute.setProduct(product);
                productAttribute.setAttribute(attribute);
                productAttribute.setAttributeValue(attributeValue);

                productAttributeRepository.save(productAttribute);
            }
        }

        validateAtLeastOneActiveVariant(request);

        List<ProductVariant> savedVariants = new ArrayList<>();
        boolean isFirstVariant = true;

        for (ProductCreateRequest.VariantRequest variantReq : request.getVariants()) {

            if (productVariantRepository.existsBySku(variantReq.getSku())) {
                throw new IllegalArgumentException("SKU already exists: " + variantReq.getSku());
            }

            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSku(variantReq.getSku());
            variant.setMrp(variantReq.getMrp());
            variant.setSellingPrice(variantReq.getSellingPrice());
            variant.setStatus(variantReq.getStatus());
            variant.setStockQuantity(variantReq.getStockQuantity());
            variant.setIsActive(true);
            variant.setIsDefault(isFirstVariant);
            isFirstVariant = false;

            variant = productVariantRepository.save(variant);
            savedVariants.add(variant);

            // Variant attributes (id/value)
            if (variantReq.getAttributes() != null) {
                for (ProductCreateRequest.VariantAttributeRequest varAttrReq : variantReq.getAttributes()) {

                    Attribute attribute = attributeRepository.findById(varAttrReq.getAttributeId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid variant attribute: " + varAttrReq.getAttributeId()));

                    AttributeValue attributeValue = attributeValueRepository
                            .findByAttributeIdAndValue(attribute.getId(), varAttrReq.getValue())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Invalid attribute value for " + attribute.getSlug() + ": " + varAttrReq.getValue()
                            ));

                    VariantAttribute va = new VariantAttribute();
                    va.setVariant(variant);
                    va.setAttribute(attribute);
                    va.setAttributeValue(attributeValue);

                    variantAttributeRepository.save(va);
                }
            }

            // Product images (variant=null)
            if (request.getImages() != null) {
                for (ProductCreateRequest.ProductImageRequest imageReq : request.getImages()) {

                    ProductImage img = new ProductImage();
                    img.setProduct(product);
                    img.setVariant(null);
                    img.setImageUrl(imageReq.getFileKey());
                    img.setImageType(imageReq.getImageType());
                    img.setDisplayOrder(imageReq.getDisplayOrder());
                    img.setIsActive(true);

                    productImageRepository.save(img);
                }
            }

            // Variant images
            if (variantReq.getImages() != null) {
                for (ProductCreateRequest.ProductImageRequest imageReq : variantReq.getImages()) {

                    ProductImage img = new ProductImage();
                    img.setProduct(product);
                    img.setVariant(variant);
                    img.setImageUrl(imageReq.getFileKey());
                    img.setImageType(imageReq.getImageType());
                    img.setDisplayOrder(imageReq.getDisplayOrder());
                    img.setIsActive(true);

                    productImageRepository.save(img);
                }
            }
        }

        // rating summary
        ProductRatingSummary rs = new ProductRatingSummary();
        rs.setProduct(product);
        rs.setAvgRating(null);
        rs.setRatingCount(0);
        rs.setRating1Count(0);
        rs.setRating2Count(0);
        rs.setRating3Count(0);
        rs.setRating4Count(0);
        rs.setRating5Count(0);
        ratingSummaryRepository.save(rs);

        return productMapper.map(
                product,
                variantAttributeRepository.findAll()
        );
    }

    private void validateAtLeastOneActiveVariant(ProductCreateRequest request) {
        long activeCount = request.getVariants().stream()
                .filter(v -> v.getStatus() == ProductStatus.ACTIVE)
                .count();

        if (activeCount == 0) {
            throw new IllegalArgumentException("At least one ACTIVE variant required");
        }
    }

    private String generateUniqueSlug(String name) {
        String base = SlugGenerator.generate(name);
        String slug = base;
        int counter = 1;

        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
