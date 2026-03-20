package com.nova.mcart.config;

import com.nova.mcart.config.props.ProductSearchProperties;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

@Slf4j
@Configuration
@EnableConfigurationProperties(ProductSearchProperties.class)
public class SearchIndexDefinitionLoader {

    public SearchIndexDefinitionLoader(ProductSearchProperties props, ResourceLoader resourceLoader) {

        String loc = props.getIndexDefinitionLocation();
        if (loc == null || loc.isBlank()) {
            return; // allow running without bootstrap
        }

        try {
            Resource resource = resourceLoader.getResource(loc);
            if (!resource.exists()) {
                throw new IllegalStateException("Index definition resource not found: " + loc);
            }

            String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            props.setIndexDefinitionJson(json);

            log.info("Loaded ES index definition JSON from {}", loc);

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load ES index definition JSON from " + loc, ex);
        }
    }
}
