package com.nova.mcart.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchResponse {

    private long total;
    private int page;
    private int size;

    // query metadata
    private String originalQuery;
    private String correctedQuery;
    private String usedQuery;
    private List<String> didYouMean;

    private List<ProductSearchItemResponse> items;
    private ProductSearchFacetsResponse facets;
}
