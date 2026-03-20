package com.nova.mcart.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSuggestResponse {

    private String q;

    // ✅ new
    private String correctedQuery;
    private List<String> didYouMean;

    private List<SuggestItemResponse> products;
    private List<SuggestItemResponse> brands;
    private List<SuggestItemResponse> categories;

    // add in ProductSuggestResponse
    private String usedQuery;

}
