package com.nova.mcart.dto.response;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchFacetsResponse {

    private List<FacetBucketResponse> brands;          // brandSlug + brandName + count
    private List<FacetBucketResponse> categories;      // categoryPath + categoryName + count

    private PriceFacetResponse price;
    private RatingFacetResponse rating;

    // attributeSlug -> list of values
    private Map<String, List<FacetBucketResponse>> productAttributes;
    private Map<String, List<FacetBucketResponse>> variantAttributes;
}
