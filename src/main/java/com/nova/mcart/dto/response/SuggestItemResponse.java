package com.nova.mcart.dto.response;

import com.nova.mcart.dto.enums.SuggestType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuggestItemResponse {

    private String text;        // label to show
    private SuggestType type;   // PRODUCT | BRAND | CATEGORY

    private String slug;        // optional
    private Long id;            // optional
    private String path;        // optional
}
