package com.nova.mcart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VariantAttributeSearchResponse {
    private Long attributeId;
    private String attributeSlug;
    private Long valueId;
    private String valueSlug;
}
