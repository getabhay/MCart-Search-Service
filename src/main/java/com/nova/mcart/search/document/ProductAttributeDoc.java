package com.nova.mcart.search.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductAttributeDoc {
    private Long attributeId;
    private String attributeName;
    private String attributeSlug;

    private Long valueId;
    private String value;
    private String valueSlug;

    private Boolean filterable;
    private Boolean searchable;
}
