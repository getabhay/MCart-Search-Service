package com.nova.mcart.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAttributeValueRequest {

    private Long attributeId;
    private String value;
    private String slug;
    private Integer sortOrder;
}
