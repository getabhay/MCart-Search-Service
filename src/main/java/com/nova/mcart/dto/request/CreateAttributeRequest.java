package com.nova.mcart.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAttributeRequest {

    private String name;
    private String slug;
    private String dataType;
    private Boolean isFilterable;
    private Boolean isSearchable;
    private Boolean isVariant;
    private Boolean isRequired;
}
