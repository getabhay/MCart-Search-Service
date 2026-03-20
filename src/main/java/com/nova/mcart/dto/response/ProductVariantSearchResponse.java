package com.nova.mcart.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantSearchResponse {
    private Long id;
    private String sku;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private Integer stockQuantity;
    private Boolean isActive;
    private String status;
    private String thumbnailUrl;

    private List<VariantAttributeSearchResponse> attrs;
}
