package com.nova.mcart.dto.response;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchStatusResponse {

    private String alias;
    private String writeIndex; // the index alias points to (or null)

    private boolean aliasExists;

    // indexName -> docCount
    private Map<String, Long> indexDocCounts;
}
