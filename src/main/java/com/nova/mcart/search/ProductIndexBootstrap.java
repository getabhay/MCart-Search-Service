package com.nova.mcart.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.nova.mcart.config.props.ProductSearchProperties;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductIndexBootstrap {

    private final ElasticsearchClient client;
    private final ProductSearchProperties props;

    @Bean
    @ConditionalOnProperty(prefix = "mcart.search", name = "bootstrap", havingValue = "true")
    public ApplicationRunner bootstrapProductIndex() {
        return args -> {
            String index = props.getIndex();
            String alias = props.getAlias();

            ensureIndexExists(index);
            ensureAliasPointsToIndex(alias, index);

            log.info("Search bootstrap done. index='{}', alias='{}'", index, alias);
        };
    }

    private void ensureIndexExists(String index) throws IOException {
        boolean exists = client.indices().exists(b -> b.index(index)).value();
        if (exists) {
            log.info("Index already exists: {}", index);
            return;
        }

        String indexDefinition = """
{
  "settings": {
    "analysis": {
      "filter": {
        "autocomplete_filter": {
          "type": "edge_ngram",
          "min_gram": 2,
          "max_gram": 20
        }
      },
      "tokenizer": {
        "category_path_tokenizer": {
          "type": "path_hierarchy",
          "delimiter": "/"
        }
      },
      "analyzer": {
        "autocomplete_index": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "asciifolding", "autocomplete_filter"]
        },
        "autocomplete_search": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "asciifolding"]
        },
        "category_path_analyzer": {
          "type": "custom",
          "tokenizer": "category_path_tokenizer"
        }
      }
    }
  },
  "mappings": {
    "dynamic": "false",
    "properties": {
      "id": { "type": "long" },
      "primaryImageUrl": {"type": "keyword", "ignore_above": 1024 },
      "productAttrValues": {
          "type": "text",
          "fields": {
            "autocomplete": {
              "type": "text",
              "analyzer": "autocomplete_index",
              "search_analyzer": "autocomplete_search"
            },
            "keyword": { "type": "keyword", "ignore_above": 256 }
          }
        },
        "variantAttrValues": {
          "type": "text",
          "fields": {
            "autocomplete": {
              "type": "text",
              "analyzer": "autocomplete_index",
              "search_analyzer": "autocomplete_search"
            },
            "keyword": { "type": "keyword", "ignore_above": 256 }
          }
        },
      "name": {
        "type": "text",
        "fields": {
          "autocomplete": { "type": "text", "analyzer": "autocomplete_index", "search_analyzer": "autocomplete_search" },
          "keyword": { "type": "keyword", "ignore_above": 256 }
        }
      },
      "slug": { "type": "keyword" },

      "status": { "type": "keyword" },
      "isActive": { "type": "boolean" },

      "brandId": { "type": "long" },
      "brandName": {
        "type": "text",
        "fields": {
          "autocomplete": { "type": "text", "analyzer": "autocomplete_index", "search_analyzer": "autocomplete_search" },
          "keyword": { "type": "keyword", "ignore_above": 256 }
        }
      },
      "brandSlug": { "type": "keyword" },

      "categoryId": { "type": "long" },
      "categoryName": {
        "type": "text",
        "fields": {
          "autocomplete": { "type": "text", "analyzer": "autocomplete_index", "search_analyzer": "autocomplete_search" },
          "keyword": { "type": "keyword", "ignore_above": 256 }
        }
      },
      "categorySlug": { "type": "keyword" },

      "categoryPath": { "type": "keyword" },
      "categoryPathIds": { "type": "long" },
      "categoryPathTree": { "type": "text", "analyzer": "category_path_analyzer" },

      "categoryPathNames": {
        "type": "text",
        "fields": {
          "autocomplete": { "type": "text", "analyzer": "autocomplete_index", "search_analyzer": "autocomplete_search" },
          "keyword": { "type": "keyword", "ignore_above": 256 }
        }
      },
      "categoryPathSlugs": { "type": "keyword" },

      "minPrice": { "type": "scaled_float", "scaling_factor": 100 },
      "maxPrice": { "type": "scaled_float", "scaling_factor": 100 },

      "avgRating": { "type": "scaled_float", "scaling_factor": 100 },
        "totalRatingCount": { "type": "integer" },
        "ratingCount1": { "type": "integer" },
        "ratingCount2": { "type": "integer" },
        "ratingCount3": { "type": "integer" },
        "ratingCount4": { "type": "integer" },
        "ratingCount5": { "type": "integer" },

      "popularityScore": { "type": "double" },

      "productAttributes": {
        "type": "nested",
        "properties": {
          "attributeId": { "type": "long" },
          "attributeSlug": { "type": "keyword" },
          "valueId": { "type": "long" },
          "valueSlug": { "type": "keyword" }
        }
      },

      "variants": {
        "type": "nested",
        "properties": {
          "id": { "type": "long" },
          "sku": { "type": "keyword" },
          "primaryImageUrl": { "type": "keyword", "ignore_above": 1024 },
          "mrp": { "type": "scaled_float", "scaling_factor": 100 },
          "sellingPrice": { "type": "scaled_float", "scaling_factor": 100 },
          "stockQuantity": { "type": "integer" },
          "isActive": { "type": "boolean" },
          "status": { "type": "keyword" },
          "avgRating": { "type": "scaled_float", "scaling_factor": 100 },  
          "totalRatingCount": { "type": "integer" },         
          "ratingCount1": { "type": "integer" },         
          "ratingCount2": { "type": "integer" },         
          "ratingCount3": { "type": "integer" },         
          "ratingCount4": { "type": "integer" },         
          "ratingCount5": { "type": "integer" },         

          "attrs": {
            "type": "nested",
            "properties": {
              "attributeId": { "type": "long" },
              "attributeSlug": { "type": "keyword" },
              "valueId": { "type": "long" },
              "valueSlug": { "type": "keyword" }
            }
          }
        }
      }
    }
  }
}
""";

        client.indices().create(b -> b.index(index).withJson(new StringReader(indexDefinition)));
        log.info("Created index with settings + mappings: {}", index);
    }

    private void ensureAliasPointsToIndex(String alias, String index) throws IOException {
        Set<String> current = new HashSet<>();

        try {
            var catResp = client.cat().aliases(a -> a.name(alias));
            for (var record : catResp.aliases()) {
                if (record.index() != null && !record.index().isBlank()) {
                    current.add(record.index());
                }
            }
        } catch (Exception ignored) {
            // alias doesn't exist yet
        }

        if (current.size() == 1 && current.contains(index)) {
            log.info("Alias '{}' already points to '{}'", alias, index);
            return;
        }

        client.indices().updateAliases(b -> {
            for (String idx : current) {
                b.actions(a -> a.remove(r -> r.index(idx).alias(alias)));
            }
            b.actions(a -> a.add(ad -> ad.index(index).alias(alias)));
            return b;
        });

        log.info("Alias switched: '{}' -> '{}'", alias, index);
    }
}
