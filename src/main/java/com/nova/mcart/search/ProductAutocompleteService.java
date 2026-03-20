package com.nova.mcart.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.nova.mcart.config.props.ProductSearchProperties;
import com.nova.mcart.dto.response.AutocompleteItemResponse;
import com.nova.mcart.search.document.ProductSearchDocument;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductAutocompleteService {

    private final ElasticsearchClient client;
    private final ProductSearchProperties props;
    private final ProductSuggestService suggestService; // ✅ NEW

    public List<AutocompleteItemResponse> autocomplete(String q, int size) throws IOException {

        String originalQ = (q == null) ? "" : q.trim();
        if (originalQ.isBlank()) return List.of();

        int finalSize = Math.min(Math.max(size, 1), 200);

        // 1) Run autocomplete with original query
        var resp = executeAutocompleteSearch(originalQ, finalSize);

        // 2) If no hits => try suggest autocorrect and rerun
        if (totalHits(resp) == 0) {
            var suggest = suggestService.suggest(originalQ);
            String corrected = (suggest == null) ? null : suggest.getCorrectedQuery();

            if (corrected != null && !corrected.isBlank()
                    && !corrected.equalsIgnoreCase(originalQ)) {
                resp = executeAutocompleteSearch(corrected.trim(), finalSize);
            }
        }

        return mapAutocomplete(resp, finalSize);
    }

    private co.elastic.clients.elasticsearch.core.SearchResponse<ProductSearchDocument> executeAutocompleteSearch(
            String qText,
            int finalSize
    ) throws IOException {

        Query query = buildAutocompleteQuery(qText);

        return client.search(s -> {
                    s.index(props.getAlias());
                    s.size(finalSize);

                    // keep payload small
                    s.source(src -> src.filter(f -> f.includes(
                            "id", "name", "slug",
                            "brandName", "brandSlug",
                            "categoryName", "categorySlug",
                            "categoryPath", "categoryPathNames", "categoryPathSlugs",
                            "variants.id", "variants.sku", "variants.mrp",
                            "variants.sellingPrice", "variants.isActive", "variants.status",
                            "variants.primaryImageUrl", "variants.avgRating",
                            "variants.totalRatingCount", "variants.ratingCount1",
                            "variants.ratingCount2", "variants.ratingCount3",
                            "variants.ratingCount4", "variants.ratingCount5"
                    )));

                    s.query(query);
                    return s;
                },
                ProductSearchDocument.class
        );
    }

    private long totalHits(co.elastic.clients.elasticsearch.core.SearchResponse<?> resp) {
        if (resp == null || resp.hits() == null || resp.hits().total() == null) return 0;
        return resp.hits().total().value();
    }

    private List<AutocompleteItemResponse> mapAutocomplete(
            co.elastic.clients.elasticsearch.core.SearchResponse<ProductSearchDocument> resp,
            int finalSize
    ) {
        LinkedHashMap<Long, AutocompleteItemResponse> out = new LinkedHashMap<>();

        if (resp != null && resp.hits() != null && resp.hits().hits() != null) {
            resp.hits().hits().forEach(h -> {
                ProductSearchDocument d = h.source();
                if (d == null || d.getId() == null) return;
                if (d.getVariants() == null || d.getVariants().isEmpty()) {
                    return;
                }
                var variants = d.getVariants().stream()
                        .filter(v -> v != null)
                        .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                        .filter(v -> "ACTIVE".equalsIgnoreCase(v.getStatus()))
                        // Optional stable ordering: cheapest first, then id
                        .sorted(Comparator
                                .comparing((com.nova.mcart.search.document.ProductVariantDoc v) -> v.getSellingPrice(), Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(v -> v.getId(), Comparator.nullsLast(Comparator.naturalOrder()))
                        )
                        .toList();

                for (var v : variants) {
                    out.computeIfAbsent(v.getId(), k -> {
                        AutocompleteItemResponse item = new AutocompleteItemResponse();
                        item.setId(d.getId());
                        item.setName(d.getName());
                        item.setSlug(d.getSlug());
                        item.setBrandName(d.getBrandName());
                        item.setBrandSlug(d.getBrandSlug());
                        item.setCategoryName(d.getCategoryName());
                        item.setCategorySlug(d.getCategorySlug());
                        item.setVariantId(v.getId());
                        item.setSku(v.getSku());
                        item.setMrp(v.getMrp());
                        item.setSellingPrice(v.getSellingPrice());
                        item.setThumbnailUrl(v.getPrimaryImageUrl());
                        item.setAvgRating(v.getAvgRating() != null? v.getAvgRating() : BigDecimal.ZERO);
                        item.setTotalRatingCount(v.getTotalRatingCount() != null ? v.getTotalRatingCount() : 0);
                        return item;
                    });
                }
            });
        }
        return out.values().stream().limit(finalSize).toList();
    }

    private Query buildAutocompleteQuery(String qText) {

        String cleaned = (qText == null) ? "" : qText.trim();
        if (cleaned.isBlank()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        return Query.of(qq -> qq.bool(b -> b
                .filter(f -> f.term(t -> t.field("isActive").value(true)))
                .filter(f -> f.term(t -> t.field("status").value("ACTIVE")))

                // Prefix match (fast)
                .should(s1 -> s1.multiMatch(mm -> mm
                        .query(cleaned)
                        .type(TextQueryType.BoolPrefix)
                        .fields(
                                "name.autocomplete^12",
                                "categoryName.autocomplete^10",
                                "categoryPathNames.autocomplete^8",
                                "brandName.autocomplete^4",
                                "variantAttrValues.autocomplete^6",
                                "productAttrValues.autocomplete^4"
                        )
                        .operator(Operator.Or)
                ))

                // Fuzzy fallback (typos: bule -> blue)
                .should(s2 -> s2.multiMatch(mm -> mm
                        .query(cleaned)
                        .type(TextQueryType.BestFields)
                        .fields(
                                "name^10",
                                "categoryName^8",
                                "brandName^5",
                                "categoryPathNames^6",
                                "variantAttrValues^8",
                                "productAttrValues^4"
                        )
                        .operator(Operator.And)
                        .fuzziness("AUTO")
                        .prefixLength(1)
                ))

                .minimumShouldMatch("1")
        ));
    }
}
