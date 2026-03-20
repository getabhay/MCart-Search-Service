package com.nova.mcart.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAttributeRequest {

    private String name;
    private Boolean isFilterable;
    private Boolean isSearchable;
    private Boolean isVariant;
    private Boolean isRequired;
    private Boolean isActive;
}
