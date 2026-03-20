package com.nova.mcart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryTreeItemResponse {

    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private Boolean isLeaf;
}
