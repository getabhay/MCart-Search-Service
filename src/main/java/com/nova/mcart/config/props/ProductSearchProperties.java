package com.nova.mcart.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mcart.search.product")
public class ProductSearchProperties {
    private String index;
    private String alias;
    /**
     * Loaded from classpath JSON by configuration (recommended).
     */
    private String indexDefinitionJson;

    /**
     * Example: classpath:es/products-index.json
     */
    private String indexDefinitionLocation;
}
