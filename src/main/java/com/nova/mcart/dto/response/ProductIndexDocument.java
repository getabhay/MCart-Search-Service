package com.nova.mcart.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductIndexDocument {
    private Long productId;
    private String name;
    private String slug;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private Double minPrice;
    private Double maxPrice;
    private List<String> attributeValues;
    private OffsetDateTime createdAt;
}