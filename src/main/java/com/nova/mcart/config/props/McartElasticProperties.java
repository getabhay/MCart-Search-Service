package com.nova.mcart.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mcart.elastic")
public class McartElasticProperties {
    private String url;
    private String apiKey;
}
