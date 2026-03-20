package com.nova.mcart.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.nova.mcart.config.props.ProductSearchProperties;
import com.nova.mcart.dto.enums.SuggestType;
import com.nova.mcart.dto.response.ProductSuggestResponse;
import com.nova.mcart.dto.response.SuggestItemResponse;
import com.nova.mcart.search.document.ProductSearchDocument;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSuggestService {

    private final ElasticsearchClient client;
    private final ProductSearchProperties props;

    public ProductSuggestResponse suggest(String q) throws IOException {

        String original = (q == null) ? "" : q.trim();
        if (original.isBlank()) {
            return empty(original);
        }

        // 1) Search using original (typed) query
        SearchResponse<ProductSearchDocument> resp = searchSuggest(original);

        // 2) Extract + validate didYouMean from same response
        List<String> raw = extractDidYouMean(resp, "did_you_mean");
        List<String> didYouMean = filterToExistingSuggestions(raw);
        String corrected = didYouMean.isEmpty() ? null : bestCorrection(original, didYouMean);

        // 3) If original yields no hits but corrected exists -> re-run search with corrected query
        String used;
        if (isZeroHits(resp) && corrected != null) {
            resp = searchSuggest(corrected);
            used = corrected;
        } else {
            used = original;
        }

        // 4) Build buckets from resp (now either original or corrected response)
        LinkedHashMap<String, SuggestItemResponse> productMap = new LinkedHashMap<>();
        LinkedHashMap<String, SuggestItemResponse> brandMap = new LinkedHashMap<>();
        LinkedHashMap<String, SuggestItemResponse> categoryMap = new LinkedHashMap<>();

        if (resp.hits() != null && resp.hits().hits() != null) {
            resp.hits().hits().forEach(hit -> {
                ProductSearchDocument d = hit.source();
                if (d == null) return;

                // gate using USED query now
                if (!containsAllTokens(d.getName(), used) && !containsAllTokens(d.getCategoryName(), used)) {
                    return;
                }

                addProduct(productMap, d, 10);
                addBrand(brandMap, d, 10);
                addCategoryLeaf(categoryMap, d, 10);
                addCategoryParents(categoryMap, d, 10);
            });
        }

        ProductSuggestResponse out = new ProductSuggestResponse();
        out.setQ(original);
        out.setDidYouMean(didYouMean);
        out.setCorrectedQuery(corrected);
        out.setUsedQuery(used); // ✅ important
        out.setProducts(new ArrayList<>(productMap.values()));
        out.setBrands(new ArrayList<>(brandMap.values()));
        out.setCategories(new ArrayList<>(categoryMap.values()));
        return out;
    }

    private SearchResponse<ProductSearchDocument> searchSuggest(String qText) throws IOException {

        Query autocompleteQuery = buildAutocompleteQuery(qText);

        return client.search(s -> {
                    s.index(props.getAlias());
                    s.size(30);
                    s.query(autocompleteQuery);

                    s.source(src -> src.filter(f -> f.includes(
                            "id", "name", "slug",
                            "brandName", "brandSlug",
                            "categoryId", "categoryName", "categorySlug",
                            "categoryPath", "categoryPathNames", "categoryPathSlugs"
                    )));

                    // phrase suggester only needed for the first run,
                    // but harmless to keep; it will just produce empty options on corrected input
                    s.suggest(su -> su
                            .text(qText)
                            .suggesters("did_you_mean", sug -> sug
                                    .phrase(p -> p
                                            .field("name")
                                            .size(3)
                                            .gramSize(2)
                                            .realWordErrorLikelihood(0.95)
                                            .confidence(0.0)
                                            .maxErrors(2.0)
                                    )
                            )
                    );

                    return s;
                },
                ProductSearchDocument.class
        );
    }

    private boolean isZeroHits(SearchResponse<?> resp) {
        return resp == null
                || resp.hits() == null
                || resp.hits().total() == null
                || resp.hits().total().value() == 0;
    }


    // ----------------------------
    // Query builders
    // ----------------------------

    private Query buildAutocompleteQuery(String qText) {
        return Query.of(qq -> qq.bool(b -> b
                .filter(f -> f.term(t -> t.field("isActive").value(true)))
                .filter(f -> f.term(t -> t.field("status").value("ACTIVE")))

                // precision boost
                .should(s1 -> s1.multiMatch(mm -> mm
                        .query(qText)
                        .type(TextQueryType.PhrasePrefix)
                        .slop(1)
                        .maxExpansions(50)
                        .fields("name^20", "categoryName^18", "brandName^8")
                ))

                // recall but strict
                .should(s2 -> s2.multiMatch(mm -> mm
                        .query(qText)
                        .type(TextQueryType.BoolPrefix)
                        .fields(
                                "name.autocomplete^10",
                                "categoryName.autocomplete^8",
                                "brandName.autocomplete^3",
                                "categoryPathNames.autocomplete^4"
                        )
                        .minimumShouldMatch(minShouldMatchForAutocomplete(qText))
                ))

                .minimumShouldMatch("1")
        ));
    }

    private Query buildSuggestionValidationQuery(String suggestion) {
        return Query.of(q -> q.bool(b -> b
                .filter(f -> f.term(t -> t.field("isActive").value(true)))
                .filter(f -> f.term(t -> t.field("status").value("ACTIVE")))
                .must(m -> m.multiMatch(mm -> mm
                        .query(suggestion)
                        .type(TextQueryType.BestFields)
                        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                        .fields(
                                "name^6",
                                "categoryName^4",
                                "categoryPathNames^3",
                                "brandName^2"
                        )
                ))
        ));
    }

    private String minShouldMatchForAutocomplete(String q) {
        String[] parts = q.trim().split("\\s+");
        int n = parts.length;
        if (n <= 1) return "1";
        if (n == 2) return "2";
        return "75%";
    }

    // ----------------------------
    // "Did you mean" parsing + validation
    // ----------------------------

    private List<String> extractDidYouMean(SearchResponse<?> resp, String key) {

        if (resp == null || resp.suggest() == null) return List.of();

        List<? extends Suggestion<?>> suggestions = resp.suggest().get(key);
        if (suggestions == null || suggestions.isEmpty()) return List.of();

        List<String> out = new ArrayList<>();

        for (Suggestion s : suggestions) {
            if (!s.isPhrase()) continue;

            var phrase = s.phrase();
            if (phrase == null || phrase.options() == null) continue;

            phrase.options().forEach(opt -> {
                if (opt != null && opt.text() != null) {
                    out.add(opt.text());
                }
            });
        }

        return out.stream().distinct().toList();
    }

    private List<String> filterToExistingSuggestions(List<String> candidates) throws IOException {

        if (candidates == null || candidates.isEmpty()) return List.of();

        List<String> valid = new ArrayList<>();

        for (String c : candidates) {
            if (c == null) continue;
            String s = c.trim();
            if (s.isBlank()) continue;

            if (hasHitsForSuggestion(s)) {
                valid.add(s);
            }

            if (valid.size() >= 3) break;
        }

        return valid.stream().distinct().toList();
    }

    private boolean hasHitsForSuggestion(String suggestion) throws IOException {

        SearchResponse<ProductSearchDocument> resp = client.search(s -> s
                        .index(props.getAlias())
                        .size(0)
                        .query(buildSuggestionValidationQuery(suggestion)),
                ProductSearchDocument.class);

        return resp.hits() != null
                && resp.hits().total() != null
                && resp.hits().total().value() > 0;
    }

    private String bestCorrection(String original, List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) return null;

        String o = original.trim().toLowerCase();
        for (String s : suggestions) {
            if (s == null) continue;
            String cand = s.trim();
            if (!cand.isBlank() && !cand.toLowerCase().equals(o)) {
                return cand;
            }
        }
        return null;
    }

    // ----------------------------
    // Bucketing helpers
    // ----------------------------

    private void addProduct(Map<String, SuggestItemResponse> map, ProductSearchDocument d, int limit) {
        if (map.size() >= limit) return;
        if (d.getName() == null || d.getSlug() == null) return;

        map.computeIfAbsent(d.getSlug(), k -> {
            SuggestItemResponse it = new SuggestItemResponse();
            it.setType(SuggestType.PRODUCT);
            it.setText(d.getName());
            it.setSlug(d.getSlug());
            it.setId(d.getId());
            return it;
        });
    }

    private void addBrand(Map<String, SuggestItemResponse> map, ProductSearchDocument d, int limit) {
        if (map.size() >= limit) return;
        if (d.getBrandName() == null || d.getBrandSlug() == null) return;

        map.computeIfAbsent(d.getBrandSlug(), k -> {
            SuggestItemResponse it = new SuggestItemResponse();
            it.setType(SuggestType.BRAND);
            it.setText(d.getBrandName());
            it.setSlug(d.getBrandSlug());
            return it;
        });
    }

    private void addCategoryLeaf(Map<String, SuggestItemResponse> map, ProductSearchDocument d, int limit) {
        if (map.size() >= limit) return;
        if (d.getCategoryName() == null || d.getCategorySlug() == null) return;

        map.computeIfAbsent(d.getCategorySlug(), k -> {
            SuggestItemResponse it = new SuggestItemResponse();
            it.setType(SuggestType.CATEGORY);
            it.setText(d.getCategoryName());
            it.setSlug(d.getCategorySlug());
            it.setId(d.getCategoryId());
            it.setPath(d.getCategoryPath());
            return it;
        });
    }

    private void addCategoryParents(Map<String, SuggestItemResponse> map, ProductSearchDocument d, int limit) {

        if (map.size() >= limit) return;
        if (d.getCategoryPath() == null || d.getCategoryPathNames() == null || d.getCategoryPathSlugs() == null) return;

        List<String> names = d.getCategoryPathNames();
        List<String> slugs = d.getCategoryPathSlugs();
        List<String> pathIds = Arrays.asList(d.getCategoryPath().split("/"));

        int max = Math.min(Math.min(names.size(), slugs.size()), pathIds.size());

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < max; i++) {

            if (map.size() >= limit) break;

            if (i == 0) prefix.append(pathIds.get(i));
            else prefix.append("/").append(pathIds.get(i));

            String name = names.get(i);
            String slug = slugs.get(i);
            if (name == null || slug == null) continue;

            map.computeIfAbsent(slug, k -> {
                SuggestItemResponse it = new SuggestItemResponse();
                it.setType(SuggestType.CATEGORY);
                it.setText(name);
                it.setSlug(slug);
                it.setPath(prefix.toString());
                return it;
            });
        }
    }

    // ----------------------------
    // Noise gate helpers
    // ----------------------------

    private boolean containsAllTokens(String text, String q) {
        if (text == null) return false;
        String t = text.toLowerCase();
        for (String tok : q.toLowerCase().split("\\s+")) {
            if (!t.contains(tok)) return false;
        }
        return true;
    }

    private ProductSuggestResponse empty(String qText) {
        ProductSuggestResponse out = new ProductSuggestResponse();
        out.setQ(qText);
        out.setCorrectedQuery(null);
        out.setDidYouMean(List.of());
        out.setProducts(List.of());
        out.setBrands(List.of());
        out.setCategories(List.of());
        return out;
    }
}
