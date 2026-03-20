package com.nova.mcart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacetBucketResponse {
    private String key;     // slug or path or valueSlug
    private String label;   // display name
    private long count;
}
