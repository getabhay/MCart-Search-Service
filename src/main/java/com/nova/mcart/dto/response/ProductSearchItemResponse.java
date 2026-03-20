package com.nova.mcart.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchItemResponse {

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
    private List<String> categoryPathNames;
    private List<String> categoryPathSlugs;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private BigDecimal avgRating;
    private Integer totalRatingCount;
    private Integer totalRatingCount1;
    private Integer totalRatingCount2;
    private Integer totalRatingCount3;
    private Integer totalRatingCount4;
    private Integer totalRatingCount5;

    private Double popularityScore;
    private List<ProductVariantSearchResponse> variants;
    private String thumbnailUrl;
}
