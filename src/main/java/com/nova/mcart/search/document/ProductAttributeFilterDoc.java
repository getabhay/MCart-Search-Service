package com.nova.mcart.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeFilterDoc {
    private Long attributeId;
    private String attributeSlug;
    private Long valueId;
    private String valueSlug;
}

