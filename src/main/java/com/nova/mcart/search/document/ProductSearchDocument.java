package com.nova.mcart.search.document;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDocument {

    private Long id;

    private String name;
    private String slug;

    private String status;
    private Boolean isActive;

    private Long brandId;
    private String brandName;
    private String brandSlug;

    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    private String categoryPath;
    private List<Long> categoryPathIds;
    private String categoryPathTree;

    private List<String> categoryPathNames;
    private List<String> categoryPathSlugs;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private BigDecimal avgRating;
    private Integer totalRatingCount;
    private Integer ratingCount1;
    private Integer ratingCount2;
    private Integer ratingCount3;
    private Integer ratingCount4;
    private Integer ratingCount5;

    private Double popularityScore;

    private List<ProductAttributeFilterDoc> productAttributes;
    private List<ProductVariantDoc> variants;

    private List<String> productAttrValues;
    private List<String> variantAttrValues;
    private String primaryImageUrl;
}
