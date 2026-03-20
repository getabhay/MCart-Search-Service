package com.nova.mcart.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutocompleteItemResponse {
    private Long id;
    private String name;
    private String slug;
    private String brandName;
    private String brandSlug;
    private String categoryName;
    private String categorySlug;
    private String thumbnailUrl;
    // ✅ NEW
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private Long variantId;
    private String sku;
    private BigDecimal avgRating;
    private Integer totalRatingCount;
}
