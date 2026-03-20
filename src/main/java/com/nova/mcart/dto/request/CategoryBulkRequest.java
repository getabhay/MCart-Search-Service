package com.nova.mcart.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryBulkRequest {

    private String name;
    private String parentSlug;
}
