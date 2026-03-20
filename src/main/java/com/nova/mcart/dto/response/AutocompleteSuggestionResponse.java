package com.nova.mcart.dto.response;

import com.nova.mcart.dto.enums.AutocompleteType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutocompleteSuggestionResponse {

    private String text;
    private AutocompleteType type;

    // routing
    private Long id;       // productId or categoryId (brandId optional if you add later)
    private String slug;   // for product/brand/category

    // category click-search
    private String path;   // "1/12/50" etc (only for CATEGORY)
}
