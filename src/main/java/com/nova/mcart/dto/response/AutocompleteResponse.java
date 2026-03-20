package com.nova.mcart.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutocompleteResponse {

    private String q;

    private List<AutocompleteSuggestionResponse> products;
    private List<AutocompleteSuggestionResponse> brands;
    private List<AutocompleteSuggestionResponse> categories;
}
