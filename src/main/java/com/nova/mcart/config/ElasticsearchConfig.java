package com.nova.mcart.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.nova.mcart.config.props.McartElasticProperties;
import com.nova.mcart.config.props.ProductSearchProperties;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({McartElasticProperties.class, ProductSearchProperties.class})
public class ElasticsearchConfig {

    @Bean
    public RestClient restClient(McartElasticProperties props) {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(props.getUrl()));

        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            Header[] headers = new Header[] { new BasicHeader("Authorization", "ApiKey " + props.getApiKey()) };
            builder.setDefaultHeaders(headers);
        }

        return builder.build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    @Bean
    public DisposableBean elasticsearchShutdown(RestClient restClient) {
        return restClient::close;
    }
}
