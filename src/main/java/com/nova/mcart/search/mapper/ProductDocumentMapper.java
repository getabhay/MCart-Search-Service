package com.nova.mcart.search.mapper;

import com.nova.mcart.entity.*;
import com.nova.mcart.search.document.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {
    @Value("${spring.cloud.aws.cdn.url}")
    private String cdnUrl;

    public ProductSearchDocument map(Product p, CategoryPathInfo categoryPathInfo) {

        String categoryPath = (p.getCategory() != null) ? p.getCategory().getPath() : null;

        return ProductSearchDocument.builder()
                .id(p.getId())

                .name(p.getName())
                .slug(p.getSlug())

                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .isActive(p.getIsActive())

                .brandId(p.getBrand() != null ? p.getBrand().getId() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .brandSlug(p.getBrand() != null ? p.getBrand().getSlug() : null)

                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : null)

                .categoryPath(categoryPath)
                .categoryPathTree(categoryPath)
                .categoryPathIds(parsePathIds(categoryPath))

                .categoryPathNames(categoryPathInfo != null ? categoryPathInfo.names() : List.of())
                .categoryPathSlugs(categoryPathInfo != null ? categoryPathInfo.slugs() : List.of())

                .minPrice(minSellingPrice(p.getVariants()))
                .maxPrice(maxSellingPrice(p.getVariants()))

                .avgRating(p.getRatingSummary() != null ? p.getRatingSummary().getAvgRating() : BigDecimal.ZERO)
                .totalRatingCount(p.getRatingSummary() != null ? p.getRatingSummary().getRatingCount() : 0)
                .ratingCount1(p.getRatingSummary() != null ? p.getRatingSummary().getRating1Count() : 0)
                .ratingCount2(p.getRatingSummary() != null ? p.getRatingSummary().getRating2Count() : 0)
                .ratingCount3(p.getRatingSummary() != null ? p.getRatingSummary().getRating3Count() : 0)
                .ratingCount4(p.getRatingSummary() != null ? p.getRatingSummary().getRating4Count() : 0)
                .ratingCount5(p.getRatingSummary() != null ? p.getRatingSummary().getRating5Count() : 0)

                .popularityScore(1.0)

                .productAttributes(mapProductAttributes(p.getProductAttributes()))
                .variants(mapVariants(p))
                .productAttrValues(
                        p.getProductAttributes() == null ? List.of()
                                : p.getProductAttributes().stream()
                                .map(pa -> pa.getAttributeValue() == null ? null : pa.getAttributeValue().getSlug())
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .distinct()
                                .toList()
                ).variantAttrValues(
                        p.getVariants() == null ? List.of()
                                : p.getVariants().stream()
                                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                                .flatMap(v -> v.getVariantAttributes() == null ? java.util.stream.Stream.empty() : v.getVariantAttributes().stream())
                                .map(va -> va.getAttributeValue() == null ? null : va.getAttributeValue().getSlug())
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .distinct()
                                .toList())
                .primaryImageUrl(resolveProductPrimaryImageUrl(p) != null ? cdnUrl + "/" +resolveProductPrimaryImageUrl(p) : null)
                .build();
    }

//    private String resolvePrimaryImageUrl(Product product) {
//
//        if (product == null || product.getImages() == null) {
//            return null;
//        }
//
//        // 1️⃣ Variant image with displayOrder = 1
//        Optional<ProductImage> variantPrimary = product.getImages().stream()
//                .filter(img -> Boolean.TRUE.equals(img.getIsActive()))
//                .filter(img -> img.getVariant() != null)
//                .filter(img -> Integer.valueOf(1).equals(img.getDisplayOrder()))
//                .min(Comparator.comparing(ProductImage::getId)); // ✅ stable pick
//
//        if (variantPrimary.isPresent()) {
//            return variantPrimary.get().getImageUrl();
//        }
//
//        // 2️⃣ Product-level image with displayOrder = 1
//        Optional<ProductImage> productPrimary = product.getImages().stream()
//                .filter(img -> Boolean.TRUE.equals(img.getIsActive()))
//                .filter(img -> img.getVariant() == null)
//                .filter(img -> Integer.valueOf(1).equals(img.getDisplayOrder()))
//                .min(Comparator.comparing(ProductImage::getId));
//
//        if (productPrimary.isPresent()) {
//            return productPrimary.get().getImageUrl();
//        }
//
//        // 3️⃣ Otherwise null
//        return null;
//    }

    private String resolveProductDisplayOrder1Url(Product product) {
        if (product == null || product.getImages() == null) return null;

        return product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsActive()))
                .filter(img -> img.getVariant() == null)
                .filter(img -> Integer.valueOf(1).equals(img.getDisplayOrder()))
                .min(Comparator.comparing(ProductImage::getId))
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    private String resolveVariantDisplayOrder1Url(Product product, ProductVariant variant) {
        if (product == null || variant == null || product.getImages() == null) return null;

        Long variantId = variant.getId();
        if (variantId == null) return null;

        return product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsActive()))
                .filter(img -> img.getVariant() != null && img.getVariant().getId() != null)
                .filter(img -> variantId.equals(img.getVariant().getId()))
                .filter(img -> Integer.valueOf(1).equals(img.getDisplayOrder()))
                .min(Comparator.comparing(ProductImage::getId))
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    private String resolveProductPrimaryImageUrl(Product product) {
        // product thumbnail rule you already implemented earlier:
        // 1) any variant image order=1 else 2) product image order=1 else null
        if (product == null || product.getImages() == null) return null;

        String anyVariantOrder1 = product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsActive()))
                .filter(img -> img.getVariant() != null)
                .filter(img -> Integer.valueOf(1).equals(img.getDisplayOrder()))
                .min(Comparator.comparing(ProductImage::getId))
                .map(ProductImage::getImageUrl)
                .orElse(null);

        if (anyVariantOrder1 != null) return anyVariantOrder1;

        return resolveProductDisplayOrder1Url(product);
    }

    private String resolveVariantThumbnailUrl(Product product, ProductVariant variant) {
        String variantOrder1 = resolveVariantDisplayOrder1Url(product, variant);
        if (variantOrder1 != null) return variantOrder1;

        return resolveProductDisplayOrder1Url(product);
    }

    private List<Long> parsePathIds(String path) {
        if (path == null || path.isBlank()) return Collections.emptyList();
        return Arrays.stream(path.split("/"))
                .filter(s -> s != null && !s.isBlank())
                .map(Long::valueOf)
                .toList();
    }

    private BigDecimal minSellingPrice(List<ProductVariant> variants) {
        if (variants == null) return null;
        return variants.stream()
                .map(ProductVariant::getSellingPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private BigDecimal maxSellingPrice(List<ProductVariant> variants) {
        if (variants == null) return null;
        return variants.stream()
                .map(ProductVariant::getSellingPrice)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private List<ProductAttributeFilterDoc> mapProductAttributes(List<ProductAttribute> attrs) {
        if (attrs == null) return Collections.emptyList();

        return attrs.stream()
                .filter(Objects::nonNull)
                .map(pa -> ProductAttributeFilterDoc.builder()
                        .attributeId(pa.getAttribute() != null ? pa.getAttribute().getId() : null)
                        .attributeSlug(pa.getAttribute() != null ? pa.getAttribute().getSlug() : null)
                        .valueId(pa.getAttributeValue() != null ? pa.getAttributeValue().getId() : null)
                        .valueSlug(pa.getAttributeValue() != null ? pa.getAttributeValue().getSlug() : null)
                        .build())
                .toList();
    }

    private List<ProductVariantDoc> mapVariants(Product product) {
        if (product == null || product.getVariants() == null) return Collections.emptyList();

        return product.getVariants().stream()
                .filter(Objects::nonNull)
                .map(v -> ProductVariantDoc.builder()
                        .id(v.getId())
                        .sku(v.getSku())
                        .mrp(v.getMrp())
                        .sellingPrice(v.getSellingPrice())
                        .stockQuantity(v.getStockQuantity())
                        .isActive(v.getIsActive())
                        .status(v.getStatus() != null ? v.getStatus().name() : null)
                        .attrs(mapVariantAttrs(v.getVariantAttributes()))
                        .primaryImageUrl(resolveVariantThumbnailUrl(product, v) != null ? cdnUrl + "/" + resolveVariantThumbnailUrl(product, v) : null)
                        .avgRating(product.getRatingSummary().getAvgRating())
                        .totalRatingCount(product.getRatingSummary().getRatingCount())
                        .ratingCount1(product.getRatingSummary().getRating1Count())
                        .ratingCount2(product.getRatingSummary().getRating2Count())
                        .ratingCount3(product.getRatingSummary().getRating3Count())
                        .ratingCount4(product.getRatingSummary().getRating4Count())
                        .ratingCount5(product.getRatingSummary().getRating5Count())
                        .build())
                .toList();
    }

    private List<VariantAttributeDoc> mapVariantAttrs(List<VariantAttribute> variantAttributes) {
        if (variantAttributes == null) return Collections.emptyList();

        return variantAttributes.stream()
                .filter(Objects::nonNull)
                .map(va -> VariantAttributeDoc.builder()
                        .attributeId(va.getAttribute() != null ? va.getAttribute().getId() : null)
                        .attributeSlug(va.getAttribute() != null ? va.getAttribute().getSlug() : null)
                        .valueId(va.getAttributeValue() != null ? va.getAttributeValue().getId() : null)
                        .valueSlug(va.getAttributeValue() != null ? va.getAttributeValue().getSlug() : null)
                        .build())
                .toList();
    }

    public ProductSearchDocument toDocument(
            Product p,
            CategoryPathInfo categoryPathInfo,
            List<ProductAttribute> productAttributes,
            List<ProductVariant> variants
    ) {

        String categoryPath = (p.getCategory() != null) ? p.getCategory().getPath() : null;

        return ProductSearchDocument.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())

                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .isActive(p.getIsActive())

                .brandId(p.getBrand() != null ? p.getBrand().getId() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .brandSlug(p.getBrand() != null ? p.getBrand().getSlug() : null)

                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : null)

                .categoryPath(categoryPath)
                .categoryPathTree(categoryPath)
                .categoryPathIds(parsePathIds(categoryPath))

                .categoryPathNames(categoryPathInfo != null ? categoryPathInfo.names() : List.of())
                .categoryPathSlugs(categoryPathInfo != null ? categoryPathInfo.slugs() : List.of())

                .minPrice(minSellingPrice(variants))
                .maxPrice(maxSellingPrice(variants))

                .avgRating(p.getRatingSummary() != null ? p.getRatingSummary().getAvgRating() : null)
                .totalRatingCount(p.getRatingSummary() != null ? p.getRatingSummary().getRatingCount() : null)

                .popularityScore(1.0)

                .productAttributes(mapProductAttributes(productAttributes))
                .variants(mapVariants(p))
                .build();
    }
}
