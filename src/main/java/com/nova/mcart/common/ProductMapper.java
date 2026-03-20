package com.nova.mcart.common;

import com.nova.mcart.dto.response.ProductResponse;
import com.nova.mcart.entity.*;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    @Value("${spring.cloud.aws.cdn.url}")
    private String cdnUrl;

    public ProductResponse map(Product product,
                               List<VariantAttribute> variantAttributes) {


        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setName(product.getName());
        response.setSlug(product.getSlug());
        response.setShortDescription(product.getShortDescription());
        response.setDescription(product.getDescription());
        response.setStatus(product.getStatus().name());

        // Brand
        ProductResponse.BrandInfo brandInfo = new ProductResponse.BrandInfo();
        brandInfo.setId(product.getBrand().getId());
        brandInfo.setName(product.getBrand().getName());
        brandInfo.setSlug(product.getBrand().getSlug());
        response.setBrand(brandInfo);

        // Category
        ProductResponse.CategoryInfo categoryInfo = new ProductResponse.CategoryInfo();
        categoryInfo.setId(product.getCategory().getId());
        categoryInfo.setName(product.getCategory().getName());
        categoryInfo.setSlug(product.getCategory().getSlug());
        categoryInfo.setPath(product.getCategory().getPath());
        response.setCategory(categoryInfo);

        // Product Attributes
        response.setAttributes(
                product.getProductAttributes().stream()
                        .map(attr -> {
                            ProductResponse.ProductAttributeResponse pa =
                                    new ProductResponse.ProductAttributeResponse();
                            pa.setAttributeId(attr.getAttribute().getId());
                            pa.setAttributeName(attr.getAttribute().getName());
                            pa.setValue(attr.getAttributeValue().getValue());
                            return pa;
                        })
                        .collect(Collectors.toList())
        );

        // Variants
        response.setVariants(
                product.getVariants().stream()
                        .map(variant -> {

                            ProductResponse.VariantResponse vr =
                                    new ProductResponse.VariantResponse();

                            vr.setId(variant.getId());
                            vr.setSku(variant.getSku());
                            vr.setMrp(variant.getMrp());
                            vr.setStockQuantity(variant.getStockQuantity());
                            vr.setSellingPrice(variant.getSellingPrice());
                            vr.setStatus(variant.getStatus().name());

                            // Variant Attributes
                            vr.setAttributes(
                                    variantAttributes.stream()
                                            .filter(v -> v.getVariant() != null
                                                    && v.getVariant().getId() != null
                                                    && v.getVariant().getId().equals(variant.getId()))
                                            .map(v -> {
                                                ProductResponse.VariantAttributeResponse var =
                                                        new ProductResponse.VariantAttributeResponse();
                                                var.setAttributeId(v.getAttribute().getId());
                                                var.setAttributeName(v.getAttribute().getName());
                                                var.setValue(v.getAttributeValue().getValue());
                                                return var;
                                            })
                                            .collect(Collectors.toList())
                            );

                            // Variant Images (only those with variant != null)
                            vr.setImages(
                                    product.getImages().stream()
                                            .filter(i -> i.getVariant() != null
                                                    && i.getVariant().getId() != null
                                                    && i.getVariant().getId().equals(variant.getId()))
                                            .map(this::mapImage)
                                            .collect(Collectors.toList())
                            );

                            return vr;
                        })
                        .collect(Collectors.toList())
        );

        // Product Images (only those with variant == null)
        response.setImages(
                product.getImages().stream()
                        .filter(i -> i.getVariant() == null)
                        .map(this::mapImage)
                        .collect(Collectors.toList())
        );


        // Price Aggregation
        BigDecimal min = product.getVariants().stream()
                .map(ProductVariant::getSellingPrice)
                .filter(v -> v != null)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        BigDecimal max =  product.getVariants().stream()
                .map(ProductVariant::getSellingPrice)
                .filter(v -> v != null)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        response.setMinPrice(min);
        response.setMaxPrice(max);

        ProductResponse.RatingSummary ratingSummary = new ProductResponse.RatingSummary();
        ratingSummary.setTotalRatings(product.getRatingSummary().getRatingCount());
        ratingSummary.setAverageRating(product.getRatingSummary().getAvgRating());
        response.setRatingSummary(ratingSummary);

        return response;
    }

    private ProductResponse.ProductImageResponse mapImage(ProductImage image) {

        ProductResponse.ProductImageResponse img =
                new ProductResponse.ProductImageResponse();

        String base = cdnUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String key = image.getImageUrl();
        if (key != null && key.startsWith("/")) {
            key = key.substring(1);
        }

        img.setImageUrl(base + "/" + key);
        img.setImageType(image.getImageType().name());
        img.setDisplayOrder(image.getDisplayOrder());

        return img;
    }
}
