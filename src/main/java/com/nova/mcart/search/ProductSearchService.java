package com.nova.mcart.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.nova.mcart.config.props.ProductSearchProperties;
import com.nova.mcart.dto.request.ProductSearchRequest;
import com.nova.mcart.dto.response.*;
import com.nova.mcart.dto.response.ProductSearchItemResponse;
import com.nova.mcart.dto.response.ProductSearchResponse;
import com.nova.mcart.dto.response.ProductVariantSearchResponse;
import com.nova.mcart.dto.response.VariantAttributeSearchResponse;
import com.nova.mcart.search.document.ProductSearchDocument;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchClient client;
    private final ProductSearchProperties props;
    private final ProductSuggestService suggestService;

    public ProductSearchResponse search(ProductSearchRequest req) throws IOException {

        int page = (req.getPage() == null || req.getPage() < 0) ? 0 : req.getPage();
        int size = (req.getSize() == null || req.getSize() <= 0) ? 20 : Math.min(req.getSize(), 100);

        String originalQ = normalizeNullable(req.getQ());
        String usedQ = originalQ;

        // 1) Run search with original query
        SearchResponse<ProductSearchDocument> resp = executeSearch(req, usedQ, page, size);
        long total = totalHits(resp);

        String correctedQuery = null;
        List<String> didYouMean = List.of();

        // 2) Fallback if no hits AND we have a text query
        if (total == 0 && originalQ != null && !originalQ.isBlank()) {

            var suggest = suggestService.suggest(originalQ);
            correctedQuery = suggest.getCorrectedQuery();
            didYouMean = suggest.getDidYouMean() == null ? List.of() : suggest.getDidYouMean();

            if (correctedQuery != null && !correctedQuery.isBlank()
                    && !correctedQuery.equalsIgnoreCase(originalQ)) {

                usedQ = correctedQuery;

                resp = executeSearch(req, usedQ, page, size);
                total = totalHits(resp);
            }
        }

        // 3) Map response
        List<ProductSearchItemResponse> items = mapItems(resp);

        ProductSearchResponse out = new ProductSearchResponse();
        out.setTotal(total);
        out.setPage(page);
        out.setSize(size);

        out.setOriginalQuery(originalQ);
        out.setCorrectedQuery(correctedQuery);
        out.setUsedQuery(usedQ);
        out.setDidYouMean(didYouMean);

        out.setItems(items);
        out.setFacets(mapFacets(resp));
        return out;
    }

    private ProductSearchFacetsResponse mapFacets(SearchResponse<ProductSearchDocument> resp) {

        ProductSearchFacetsResponse facets = new ProductSearchFacetsResponse();

        if (resp == null || resp.aggregations() == null) {
            facets.setBrands(List.of());
            facets.setCategories(List.of());
            facets.setProductAttributes(Map.of());
            facets.setVariantAttributes(Map.of());
            facets.setPrice(null);
            facets.setRating(null);
            return facets;
        }

        facets.setBrands(readBrandFacet(resp));
        facets.setCategories(readLeafCategoryFacet(resp)); // stable
        facets.setPrice(readPriceFacet(resp));
        facets.setRating(readRatingFacet(resp));
        facets.setProductAttributes(readProductAttributeFacet(resp));
        facets.setVariantAttributes(readVariantAttributeFacet(resp));

        return facets;
    }

    private List<FacetBucketResponse> readBrandFacet(SearchResponse<?> resp) {
        var agg = resp.aggregations().get("brands");
        if (agg == null || !agg.isSterms()) return List.of();

        List<FacetBucketResponse> out = new ArrayList<>();

        for (var b : agg.sterms().buckets().array()) {
            String slug = b.key().stringValue();
            long count = b.docCount();

            String label = slug;
            var nameAgg = b.aggregations().get("brand_name");
            if (nameAgg != null && nameAgg.isSterms()
                    && nameAgg.sterms().buckets() != null
                    && !nameAgg.sterms().buckets().array().isEmpty()) {
                label = nameAgg.sterms().buckets().array().get(0).key().stringValue();
            }

            FacetBucketResponse fb = new FacetBucketResponse();
            fb.setKey(slug);
            fb.setLabel(label);
            fb.setCount(count);
            out.add(fb);
        }

        return out;
    }

    private List<FacetBucketResponse> readLeafCategoryFacet(SearchResponse<?> resp) {
        var agg = resp.aggregations().get("leaf_categories");
        if (agg == null || !agg.isSterms()) return List.of();

        List<FacetBucketResponse> out = new ArrayList<>();

        for (var b : agg.sterms().buckets().array()) {
            String catSlug = b.key().stringValue();
            long count = b.docCount();

            String label = catSlug;
            var nameAgg = b.aggregations().get("cat_name");
            if (nameAgg != null && nameAgg.isSterms()
                    && !nameAgg.sterms().buckets().array().isEmpty()) {
                label = nameAgg.sterms().buckets().array().get(0).key().stringValue();
            }

            // For categories, we want click-search path too.
            // We'll encode "slug|path" in key, and keep label = display name.
            String path = null;
            var pathAgg = b.aggregations().get("cat_path");
            if (pathAgg != null && pathAgg.isSterms()
                    && !pathAgg.sterms().buckets().array().isEmpty()) {
                path = pathAgg.sterms().buckets().array().get(0).key().stringValue();
            }

            FacetBucketResponse fb = new FacetBucketResponse();
            fb.setKey(path != null ? path : catSlug); // click-search by path prefix works best
            fb.setLabel(label);
            fb.setCount(count);
            out.add(fb);
        }

        return out;
    }

    private PriceFacetResponse readPriceFacet(SearchResponse<?> resp) {
        var agg = resp.aggregations().get("price_stats");
        if (agg == null || !agg.isStats()) return null;

        var st = agg.stats();

        PriceFacetResponse out = new PriceFacetResponse();
        if (st.min() != null && !Double.isInfinite(st.min())) out.setMin(java.math.BigDecimal.valueOf(st.min()));
        if (st.max() != null && !Double.isInfinite(st.max())) out.setMax(java.math.BigDecimal.valueOf(st.max()));
        return out;
    }

    private RatingFacetResponse readRatingFacet(SearchResponse<?> resp) {
        var agg = resp.aggregations().get("rating_1_5");
        if (agg == null || !agg.isHistogram()) return null;

        List<FacetBucketResponse> buckets = new ArrayList<>();
        for (var b : agg.histogram().buckets().array()) {
            FacetBucketResponse fb = new FacetBucketResponse();
            fb.setKey(String.valueOf(b.key()));
            fb.setLabel(((int) Math.floor(b.key())) + "+");
            fb.setCount(b.docCount());
            buckets.add(fb);
        }

        RatingFacetResponse out = new RatingFacetResponse();
        out.setBuckets(buckets);
        return out;
    }

    private Map<String, List<FacetBucketResponse>> readProductAttributeFacet(SearchResponse<?> resp) {
        var agg = resp.aggregations().get("product_attrs");
        if (agg == null || !agg.isNested()) return Map.of();

        var byAttr = agg.nested().aggregations().get("by_attr");
        if (byAttr == null || !byAttr.isSterms()) return Map.of();

        Map<String, List<FacetBucketResponse>> out = new LinkedHashMap<>();

        for (var ab : byAttr.sterms().buckets().array()) {
            String attrSlug = ab.key().stringValue();

            var byValue = ab.aggregations().get("by_value");
            if (byValue == null || !byValue.isSterms()) continue;

            List<FacetBucketResponse> values = new ArrayList<>();
            for (var vb : byValue.sterms().buckets().array()) {
                FacetBucketResponse fb = new FacetBucketResponse();
                fb.setKey(vb.key().stringValue());
                fb.setLabel(vb.key().stringValue());
                fb.setCount(vb.docCount());
                values.add(fb);
            }

            out.put(attrSlug, values);
        }

        return out;
    }

    private Map<String, List<FacetBucketResponse>> readVariantAttributeFacet(SearchResponse<?> resp) {
        var agg = resp.aggregations().get("variant_attrs");
        if (agg == null || !agg.isNested()) return Map.of();

        var attrs = agg.nested().aggregations().get("attrs");
        if (attrs == null || !attrs.isNested()) return Map.of();

        var byAttr = attrs.nested().aggregations().get("by_attr");
        if (byAttr == null || !byAttr.isSterms()) return Map.of();

        Map<String, List<FacetBucketResponse>> out = new LinkedHashMap<>();

        for (var ab : byAttr.sterms().buckets().array()) {
            String attrSlug = ab.key().stringValue();

            var byValue = ab.aggregations().get("by_value");
            if (byValue == null || !byValue.isSterms()) continue;

            List<FacetBucketResponse> values = new ArrayList<>();
            for (var vb : byValue.sterms().buckets().array()) {
                FacetBucketResponse fb = new FacetBucketResponse();
                fb.setKey(vb.key().stringValue());
                fb.setLabel(vb.key().stringValue());
                fb.setCount(vb.docCount());
                values.add(fb);
            }

            out.put(attrSlug, values);
        }

        return out;
    }

    // ----------------------------
    // Execute
    // ----------------------------

    private SearchResponse<ProductSearchDocument> executeSearch(ProductSearchRequest req, String qText, int page, int size)
            throws IOException {

        Query query = buildQuery(req, qText);
        List<SortOptions> sorts = buildSort(req.getSort());

        return client.search(s -> {
                    s.aggregations("brands", a -> a
                            .terms(t -> t.field("brandSlug").size(20))
                            .aggregations("brand_name", sub -> sub
                                    .terms(tt -> tt.field("brandName.keyword").size(1))
                            )
                    );

                    s.aggregations("leaf_categories", a -> a
                            .terms(t -> t.field("categorySlug").size(30))
                            .aggregations("cat_name", sub -> sub
                                    .terms(tt -> tt.field("categoryName.keyword").size(1))
                            )
                            .aggregations("cat_path", sub -> sub
                                    .terms(tt -> tt.field("categoryPath").size(1))
                            )
                    );


                    s.aggregations("price_stats", a -> a.stats(st -> st.field("minPrice")));
                    s.aggregations("rating_1_5", a -> a.histogram(h -> h.field("avgRating").interval(1.0)));
                    s.aggregations("product_attrs", a -> a
                            .nested(n -> n.path("productAttributes"))
                            .aggregations("by_attr", aa -> aa
                                    .terms(t -> t.field("productAttributes.attributeSlug").size(30))
                                    .aggregations("by_value", av -> av
                                            .terms(t -> t.field("productAttributes.valueSlug").size(50))
                                    )
                            )
                    );
                    s.aggregations("variant_attrs", a -> a
                            .nested(n -> n.path("variants"))
                            .aggregations("attrs", aa -> aa
                                    .nested(n2 -> n2.path("variants.attrs"))
                                    .aggregations("by_attr", ab -> ab
                                            .terms(t -> t.field("variants.attrs.attributeSlug").size(30))
                                            .aggregations("by_value", av -> av
                                                    .terms(t -> t.field("variants.attrs.valueSlug").size(50))
                                            )
                                    )
                            )
                    );

                    s.index(props.getAlias());
                    s.from(page * size);
                    s.size(size);
                    s.query(query);
                    if (!sorts.isEmpty()) {
                        s.sort(sorts);
                    }
                    return s;
                },
                ProductSearchDocument.class
        );
    }

    private long totalHits(SearchResponse<?> resp) {
        if (resp == null || resp.hits() == null || resp.hits().total() == null) return 0;
        return resp.hits().total().value();
    }

    // ----------------------------
    // Query builder
    // ----------------------------

    private Query buildQuery(ProductSearchRequest req, String qOverride) {

        BoolQuery.Builder b = new BoolQuery.Builder();

        // Always filter active products
        b.filter(q -> q.term(t -> t.field("isActive").value(true)));

        // Keep status as requested
        b.filter(q -> q.term(t -> t.field("status").value("ACTIVE")));

        // Brand filters (multi first, then fallback to single)

        if (req.getBrandIds() != null && !req.getBrandIds().isEmpty()) {
            List<Long> ids = req.getBrandIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (!ids.isEmpty()) {
                b.filter(q -> q.terms(t -> t
                        .field("brandId")
                        .terms(v -> v.value(ids.stream().map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList()))
                ));
            }
        } else if (req.getBrandId() != null) {
            // backward compatible single brandId
            b.filter(q -> q.term(t -> t.field("brandId").value(req.getBrandId())));
        }

        if (req.getBrandSlugs() != null && !req.getBrandSlugs().isEmpty()) {
            List<String> slugs = req.getBrandSlugs().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();

            if (!slugs.isEmpty()) {
                b.filter(q -> q.terms(t -> t
                        .field("brandSlug")
                        .terms(v -> v.value(slugs.stream().map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList()))
                ));
            }
        }

        // Price filters (scaled_float => use number())
        if (req.getMinPrice() != null) {
            b.filter(q -> q.range(r -> r.number(n -> n
                    .field("minPrice")
                    .gte(req.getMinPrice().doubleValue())
            )));
        }
        if (req.getMaxPrice() != null) {
            b.filter(q -> q.range(r -> r.number(n -> n
                    .field("maxPrice")
                    .lte(req.getMaxPrice().doubleValue())
            )));
        }

        // Rating filter
        if (req.getMinRating() != null) {
            b.filter(q -> q.range(r -> r.number(n -> n
                    .field("avgRating")
                    .gte(req.getMinRating().doubleValue())
            )));
        }

        // ----------------------------
// Category filters
// - OR within categoryIds
// - OR within categoryPathPrefixes
// - AND with other filters
// Backward compatible with single categoryId / categoryPathPrefix
// ----------------------------

        List<Long> categoryIds = new ArrayList<>();
        if (req.getCategoryIds() != null) {
            req.getCategoryIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(categoryIds::add);
        }
        if (req.getCategoryId() != null) {
            categoryIds.add(req.getCategoryId());
        }
        categoryIds = categoryIds.stream().distinct().toList();

        List<String> prefixes = new ArrayList<>();
        if (req.getCategoryPathPrefixes() != null) {
            req.getCategoryPathPrefixes().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(this::normalizePath)
                    .distinct()
                    .forEach(prefixes::add);
        }
        if (req.getCategoryPathPrefix() != null && !req.getCategoryPathPrefix().isBlank()) {
            prefixes.add(normalizePath(req.getCategoryPathPrefix()));
        }
        prefixes = prefixes.stream().distinct().toList();

// If any category filter present, apply a single bool filter:
// should = (ids OR prefixes...), minimumShouldMatch=1
        if (!categoryIds.isEmpty() || !prefixes.isEmpty()) {
            List<Long> finalCategoryIds = categoryIds;
            List<String> finalPrefixes = prefixes;
            b.filter(q -> q.bool(bb -> {
                // OR by ids
                if (!finalCategoryIds.isEmpty()) {
                    for (Long id : finalCategoryIds) {
                        bb.should(s -> s.term(t -> t.field("categoryPathIds").value(id)));
                    }
                }

                // OR by prefixes (exact path OR subtree)
                if (!finalPrefixes.isEmpty()) {
                    for (String pfx : finalPrefixes) {
                        bb.should(s -> s.term(t -> t.field("categoryPath").value(pfx)));
                        bb.should(s -> s.prefix(p -> p.field("categoryPath").value(pfx + "/")));
                    }
                }

                bb.minimumShouldMatch("1");
                return bb;
            }));
        }

        // Category slug filter (OR within slugs)
        List<String> catSlugs = new ArrayList<>();
        if (req.getCategorySlugs() != null && !req.getCategorySlugs().isEmpty()) {
            catSlugs = req.getCategorySlugs().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();

            if (!catSlugs.isEmpty()) {
                List<String> finalCatSlugs = catSlugs;
                b.filter(q -> q.terms(t -> t
                        .field("categorySlug")
                        .terms(v -> v.value(
                                finalCatSlugs.stream()
                                        .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                        .toList()
                        ))
                ));
            }
        } else {
            catSlugs = List.of();
        }

//        // Product-level attribute filters (nested)
//        if (req.getAttributes() != null && !req.getAttributes().isEmpty()) {
//            for (Map.Entry<String, String> e : req.getAttributes().entrySet()) {
//                if (isBlank(e.getKey()) || isBlank(e.getValue())) continue;
//
//                String aSlug = e.getKey().trim();
//                String vSlug = e.getValue().trim();
//
//                b.filter(q -> q.nested(n -> n
//                        .path("productAttributes")
//                        .query(nq -> nq.bool(nb -> nb
//                                .must(m1 -> m1.term(t -> t.field("productAttributes.attributeSlug").value(aSlug)))
//                                .must(m2 -> m2.term(t -> t.field("productAttributes.valueSlug").value(vSlug)))
//                        ))
//                ));
//            }
//        }

//        // Variant-level attribute filters (nested variants -> nested attrs)
//        if (req.getVariantAttributes() != null && !req.getVariantAttributes().isEmpty()) {
//            for (Map.Entry<String, String> e : req.getVariantAttributes().entrySet()) {
//                if (isBlank(e.getKey()) || isBlank(e.getValue())) continue;
//
//                String aSlug = e.getKey().trim();
//                String vSlug = e.getValue().trim();
//
//                b.filter(q -> q.nested(n1 -> n1
//                        .path("variants")
//                        .query(q1 -> q1.nested(n2 -> n2
//                                .path("variants.attrs")
//                                .query(q2 -> q2.bool(nb -> nb
//                                        .must(m1 -> m1.term(t -> t.field("variants.attrs.attributeSlug").value(aSlug)))
//                                        .must(m2 -> m2.term(t -> t.field("variants.attrs.valueSlug").value(vSlug)))
//                                ))
//                        ))
//                ));
//            }
//        }

        // Variant-level attribute filters (return ONLY matching variants using inner_hits)
//        if (req.getVariantAttributes() != null && !req.getVariantAttributes().isEmpty()) {
//
//            // Build nested query on variants, and within it enforce attrs constraints
//            b.filter(q -> q.nested(nv -> {
//                nv.path("variants");
//
//                nv.query(vq -> vq.bool(vb -> {
//
//                    // Optional: only active variants
//                    vb.filter(f -> f.term(t -> t.field("variants.isActive").value(true)));
//                    vb.filter(f -> f.term(t -> t.field("variants.status").value("ACTIVE")));
//
//                    for (Map.Entry<String, String> e : req.getVariantAttributes().entrySet()) {
//                        if (isBlank(e.getKey()) || isBlank(e.getValue())) continue;
//
//                        String aSlug = e.getKey().trim();
//                        String vSlug = e.getValue().trim();
//
//                        // Important:
//                        // Each requested attribute/value becomes a nested filter on variants.attrs.
//                        vb.filter(f -> f.nested(na -> na
//                                .path("variants.attrs")
//                                .query(aq -> aq.bool(ab -> ab
//                                        .filter(ff -> ff.term(t -> t.field("variants.attrs.attributeSlug").value(aSlug)))
//                                        .filter(ff -> ff.term(t -> t.field("variants.attrs.valueSlug").value(vSlug)))
//                                ))
//                        ));
//                    }
//
//                    return vb;
//                }));
//
//                // ✅ inner hits => ES returns only matching nested variants
//                nv.innerHits(ih -> ih.size(50));
//
//                return nv;
//            }));
//        }

        // Product-level attribute filters (nested): AND across attrs, OR within values
        if (req.getAttributes() != null && !req.getAttributes().isEmpty()) {

            for (Map.Entry<String, List<String>> e : req.getAttributes().entrySet()) {

                String aSlug = (e.getKey() == null) ? null : e.getKey().trim();
                List<String> values = e.getValue();

                if (isBlank(aSlug) || values == null || values.isEmpty()) continue;

                List<String> cleanedValues = values.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(v -> !v.isBlank())
                        .toList();

                if (cleanedValues.isEmpty()) continue;

                b.filter(q -> q.nested(n -> n
                        .path("productAttributes")
                        .query(nq -> nq.bool(nb -> {
                            nb.filter(f -> f.term(t -> t.field("productAttributes.attributeSlug").value(aSlug)));

                            // OR within values
                            nb.filter(f -> f.bool(vb -> {
                                for (String vSlug : cleanedValues) {
                                    vb.should(s -> s.term(t -> t.field("productAttributes.valueSlug").value(vSlug)));
                                }
                                vb.minimumShouldMatch("1");
                                return vb;
                            }));

                            return nb;
                        }))
                ));
            }
        }

        // Variant-level attribute filters:
        // AND across different attrs, OR within values for same attr
        // Returns ONLY matching variants using inner_hits
        if (req.getVariantAttributes() != null && !req.getVariantAttributes().isEmpty()) {

            b.filter(q -> q.nested(nv -> {

                nv.path("variants");

                nv.query(vq -> vq.bool(vb -> {

                    // Optional: only active variants
                    vb.filter(f -> f.term(t -> t.field("variants.isActive").value(true)));
                    vb.filter(f -> f.term(t -> t.field("variants.status").value("ACTIVE")));

                    for (Map.Entry<String, List<String>> e : req.getVariantAttributes().entrySet()) {

                        String aSlug = (e.getKey() == null) ? null : e.getKey().trim();
                        List<String> values = e.getValue();

                        if (isBlank(aSlug) || values == null || values.isEmpty()) continue;

                        List<String> cleanedValues = values.stream()
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(v -> !v.isBlank())
                                .toList();

                        if (cleanedValues.isEmpty()) continue;

                        // For each attribute: enforce nested variants.attrs
                        // attributeSlug MUST match AND valueSlug in (v1 OR v2 OR v3)
                        vb.filter(f -> f.nested(na -> na
                                .path("variants.attrs")
                                .query(aq -> aq.bool(ab -> {
                                    ab.filter(ff -> ff.term(t -> t.field("variants.attrs.attributeSlug").value(aSlug)));

                                    ab.filter(ff -> ff.bool(vb2 -> {
                                        for (String vSlug : cleanedValues) {
                                            vb2.should(s -> s.term(t -> t.field("variants.attrs.valueSlug").value(vSlug)));
                                        }
                                        vb2.minimumShouldMatch("1");
                                        return vb2;
                                    }));

                                    return ab;
                                }))
                        ));
                    }

                    return vb;
                }));

                // ✅ only matching variants will be returned here
                nv.innerHits(ih -> ih.size(50));

                return nv;
            }));
        }

        // Full text
        String qText = normalizeNullable(qOverride);

        if (qText != null && !qText.isBlank()) {

            // Strong AND query across key fields (reduces irrelevant matches)
            b.must(m -> m.multiMatch(mm -> mm
                    .query(qText)
                    .fields(
                            "name^6",
                            "categoryName^7",
                            "categoryPathNames^4",
                            "brandName^3",
                            "slug^1",
                            "brandSlug^1",
                            "categorySlug^1"
                    )
                    .type(TextQueryType.CrossFields)
                    .operator(Operator.And)
            ));

            // Optional fuzzy should for typos (boosted but not required)
            b.should(s -> s.multiMatch(mm -> mm
                    .query(qText)
                    .fields(
                            "name^2",
                            "categoryName^2",
                            "categoryPathNames",
                            "brandName"
                    )
                    .fuzziness("AUTO")
            ));

            // Optional nested variant attrs matching (helps "red 40" etc)
            b.should(s -> s.nested(n1 -> n1
                    .path("variants")
                    .query(q1 -> q1.nested(n2 -> n2
                            .path("variants.attrs")
                            .query(q2 -> q2.multiMatch(mm -> mm
                                    .query(qText)
                                    .fields("variants.attrs.valueSlug^2", "variants.attrs.attributeSlug")
                                    .type(TextQueryType.BestFields)
                            ))
                    ))
            ));

            // keep minimumShouldMatch unset because must already exists
        } else {
            b.must(m -> m.matchAll(ma -> ma));
        }

        return Query.of(q -> q.bool(b.build()));
    }

    // ----------------------------
    // Sort
    // ----------------------------

    private List<SortOptions> buildSort(String sort) {
        if (sort == null) sort = "RELEVANCE";

        return switch (sort) {
            case "PRICE_ASC" -> List.of(sortField("minPrice", SortOrder.Asc));
            case "PRICE_DESC" -> List.of(sortField("minPrice", SortOrder.Desc));
            case "RATING_DESC" -> List.of(
                    sortField("avgRating", SortOrder.Desc),
                    sortField("totalRatingCount", SortOrder.Desc)
            );
            case "POPULARITY_DESC" -> List.of(sortField("popularityScore", SortOrder.Desc));
            default -> List.of(); // relevance by _score
        };
    }

    private SortOptions sortField(String field, SortOrder order) {
        return SortOptions.of(s -> s.field(f -> f.field(field).order(order)));
    }

    // ----------------------------
    // Mapping
    // ----------------------------

    private List<ProductSearchItemResponse> mapItems(SearchResponse<ProductSearchDocument> resp) {

        if (resp == null || resp.hits() == null || resp.hits().hits() == null) return List.of();

        List<ProductSearchItemResponse> items = new ArrayList<>();

        resp.hits().hits().forEach(hit -> {
            ProductSearchDocument d = hit.source();
            if (d == null) return;

            ProductSearchItemResponse item = new ProductSearchItemResponse();
            item.setId(d.getId());
            item.setName(d.getName());
            item.setSlug(d.getSlug());
            item.setThumbnailUrl(d.getPrimaryImageUrl());

            item.setStatus(d.getStatus());
            item.setIsActive(d.getIsActive());

            item.setBrandId(d.getBrandId());
            item.setBrandName(d.getBrandName());
            item.setBrandSlug(d.getBrandSlug());

            item.setCategoryId(d.getCategoryId());
            item.setCategoryName(d.getCategoryName());
            item.setCategorySlug(d.getCategorySlug());

            item.setCategoryPath(d.getCategoryPath());
            item.setCategoryPathIds(d.getCategoryPathIds());
            item.setCategoryPathNames(d.getCategoryPathNames());
            item.setCategoryPathSlugs(d.getCategoryPathSlugs());

            item.setMinPrice(d.getMinPrice());
            item.setMaxPrice(d.getMaxPrice());

            item.setAvgRating(d.getAvgRating() != null ? d.getAvgRating() : BigDecimal.ZERO);
            item.setTotalRatingCount(d.getTotalRatingCount() != null ? d.getTotalRatingCount() : 0);
            item.setTotalRatingCount1(d.getRatingCount1() != null ? d.getRatingCount1() : 0);
            item.setTotalRatingCount2(d.getRatingCount2() != null ? d.getRatingCount2() : 0);
            item.setTotalRatingCount3(d.getRatingCount3() != null ? d.getRatingCount3() : 0);
            item.setTotalRatingCount4(d.getRatingCount4() != null ? d.getRatingCount4() : 0);
            item.setTotalRatingCount5(d.getRatingCount5() != null ? d.getRatingCount5() : 0);

            item.setPopularityScore(d.getPopularityScore());

//            item.setVariants(mapVariants(d));
            item.setVariants(extractVariantsFromInnerHits(hit, d));

            items.add(item);
        });

        return items;
    }

    private List<ProductVariantSearchResponse> extractVariantsFromInnerHits(
            co.elastic.clients.elasticsearch.core.search.Hit<ProductSearchDocument> hit,
            ProductSearchDocument d
    ) {
        // If inner_hits contains variants, use them (only matching variants).
        if (hit.innerHits() != null && hit.innerHits().containsKey("variants")) {

            var ih = hit.innerHits().get("variants");
            if (ih == null || ih.hits() == null || ih.hits().hits() == null) return List.of();

            var jsonpMapper = client._transport().jsonpMapper();

            List<ProductVariantSearchResponse> out = new ArrayList<>();
            for (var vh : ih.hits().hits()) {

                // inner hit _source is JsonData
                var src = vh.source();
                if (src == null) continue;

                // Convert JsonData -> ProductVariantDoc
                var vdoc = src.to(com.nova.mcart.search.document.ProductVariantDoc.class, jsonpMapper);

                ProductVariantSearchResponse vr = new ProductVariantSearchResponse();
                vr.setId(vdoc.getId());
                vr.setSku(vdoc.getSku());
                vr.setMrp(vdoc.getMrp());
                vr.setSellingPrice(vdoc.getSellingPrice());
                vr.setStockQuantity(vdoc.getStockQuantity());
                vr.setIsActive(vdoc.getIsActive());
                vr.setStatus(vdoc.getStatus());
                vr.setThumbnailUrl(
                        vdoc.getPrimaryImageUrl() != null ? vdoc.getPrimaryImageUrl() : d.getPrimaryImageUrl()
                );

                if (vdoc.getAttrs() == null) {
                    vr.setAttrs(List.of());
                } else {
                    vr.setAttrs(vdoc.getAttrs().stream().map(a -> {
                        VariantAttributeSearchResponse ar = new VariantAttributeSearchResponse();
                        ar.setAttributeId(a.getAttributeId());
                        ar.setAttributeSlug(a.getAttributeSlug());
                        ar.setValueId(a.getValueId());
                        ar.setValueSlug(a.getValueSlug());
                        return ar;
                    }).toList());
                }

                out.add(vr);
            }

            return out;
        }

        // Otherwise return all variants from _source (no variant filter requested)
        return mapVariants(d);
    }


    private List<ProductVariantSearchResponse> mapVariants(ProductSearchDocument d) {
        if (d.getVariants() == null) return List.of();

        return d.getVariants().stream().map(v -> {
            ProductVariantSearchResponse vr = new ProductVariantSearchResponse();
            vr.setId(v.getId());
            vr.setSku(v.getSku());
            vr.setMrp(v.getMrp());
            vr.setSellingPrice(v.getSellingPrice());
            vr.setStockQuantity(v.getStockQuantity());
            vr.setIsActive(v.getIsActive());
            vr.setStatus(v.getStatus());
            vr.setThumbnailUrl(
                    v.getPrimaryImageUrl() != null ? v.getPrimaryImageUrl() : d.getPrimaryImageUrl()
            );

            if (v.getAttrs() == null) {
                vr.setAttrs(List.of());
            } else {
                vr.setAttrs(v.getAttrs().stream().map(a -> {
                    VariantAttributeSearchResponse ar = new VariantAttributeSearchResponse();
                    ar.setAttributeId(a.getAttributeId());
                    ar.setAttributeSlug(a.getAttributeSlug());
                    ar.setValueId(a.getValueId());
                    ar.setValueSlug(a.getValueSlug());
                    return ar;
                }).toList());
            }

            return vr;
        }).toList();
    }

    // ----------------------------
    // Utils
    // ----------------------------

    private String normalizeNullable(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }

    private String normalizePath(String path) {
        String p = path.trim();
        while (p.startsWith("/")) p = p.substring(1);
        while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
