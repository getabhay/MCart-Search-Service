package com.nova.mcart.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingFacetResponse {

    // ex: 4+, 3+, 2+, 1+
    private List<FacetBucketResponse> buckets;
}
