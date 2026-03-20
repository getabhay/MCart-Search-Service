package com.nova.mcart.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryAttributeRequest {

    private Long categoryId;
    private Long attributeId;
    private Boolean isVariantLevel;
    private Boolean isRequired;
    private Boolean isFilterable;
}
