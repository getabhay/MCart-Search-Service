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
public class ProductVariantDoc {
    private Long id;
    private String sku;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private Integer stockQuantity;
    private Boolean isActive;
    private String status;
    private List<VariantAttributeDoc> attrs;
    private String primaryImageUrl;
    private BigDecimal avgRating;
    private Integer totalRatingCount;
    private Integer ratingCount1;
    private Integer ratingCount2;
    private Integer ratingCount3;
    private Integer ratingCount4;
    private Integer ratingCount5;
}
