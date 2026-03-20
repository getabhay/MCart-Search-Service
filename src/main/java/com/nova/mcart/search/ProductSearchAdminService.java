package com.nova.mcart.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.nova.mcart.config.props.ProductSearchProperties;
import com.nova.mcart.dto.response.SearchStatusResponse;
import com.nova.mcart.entity.Product;
import com.nova.mcart.repository.ProductAttributeRepository;
import com.nova.mcart.repository.ProductRepository;
import com.nova.mcart.repository.ProductVariantRepository;
import com.nova.mcart.search.document.CategoryPathInfo;
import com.nova.mcart.search.document.ProductSearchDocument;
import com.nova.mcart.search.mapper.ProductDocumentMapper;
import com.nova.mcart.search.reindex.ReindexJob;
import com.nova.mcart.search.reindex.ReindexJobStore;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchAdminService {

    private final ElasticsearchClient client;
    private final ProductSearchProperties props;

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeRepository productAttributeRepository;

    private final ProductDocumentMapper mapper;
    private final CategoryPathResolver categoryPathResolver; // ✅ new (batch breadcrumb resolver)

    private final ReindexJobStore jobStore;

    public void bootstrap() throws IOException {
        ensureIndexExists(props.getIndex());
        ensureAliasPointsToIndex(props.getAlias(), props.getIndex());
    }

    private void ensureIndexExists(String index) throws IOException {
        boolean exists = client.indices().exists(b -> b.index(index)).value();
        if (exists) {
            log.info("Index already exists: {}", index);
            return;
        }

        String indexDefinition = props.getIndexDefinitionJson();
        if (indexDefinition == null || indexDefinition.isBlank()) {
            throw new IllegalStateException("mcart.search.product.index-definition-json is missing");
        }

        client.indices().create(b -> b.index(index).withJson(new StringReader(indexDefinition)));
        log.info("Created index '{}'", index);
    }

    private void ensureAliasPointsToIndex(String alias, String index) throws IOException {

        Set<String> current = new HashSet<>();
        try {
            Map<String, ?> resp = client.indices().getAlias(b -> b.name(alias)).aliases();
            current.addAll(resp.keySet());
        } catch (Exception ignored) {
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

    public SearchStatusResponse status() throws IOException {

        SearchStatusResponse out = new SearchStatusResponse();
        out.setAlias(props.getAlias());

        Map<String, Long> counts = new LinkedHashMap<>();

        boolean aliasExists = false;
        String writeIndex = null;

        try {
            Map<String, ?> aliasResp = client.indices().getAlias(b -> b.name(props.getAlias())).aliases();
            aliasExists = !aliasResp.isEmpty();

            if (aliasExists) {
                writeIndex = aliasResp.keySet().stream().findFirst().orElse(null);
                for (String idx : aliasResp.keySet()) {
                    long c = client.count(b -> b.index(idx)).count();
                    counts.put(idx, c);
                }
            }
        } catch (Exception ignored) {
        }

        out.setAliasExists(aliasExists);
        out.setWriteIndex(writeIndex);
        out.setIndexDocCounts(counts);

        return out;
    }

    public ReindexJob startReindex() {
        ReindexJob job = jobStore.create();
        reindexAsync(job.getId());
        return job;
    }

    @Async
    @Transactional
    public void reindexAsync(String jobId) {

        ReindexJob job = jobStore.get(jobId).orElseThrow();

        try {
            int page = 0;
            int size = 200;

            while (true) {

                var pageData = productRepository.findPageForIndexing(PageRequest.of(page, size));
                List<Product> products = pageData.getContent();
                if (products.isEmpty()) break;

                job.incRead(products.size());

                List<Long> productIds = products.stream().map(Product::getId).toList();

                var productAttrs = productAttributeRepository.findAllForIndexingByProductIds(productIds);
                var variants = productVariantRepository.findAllForIndexingByProductIds(productIds);

                Map<Long, List<com.nova.mcart.entity.ProductAttribute>> attrsByPid = new HashMap<>();
                for (var pa : productAttrs) {
                    attrsByPid.computeIfAbsent(pa.getProduct().getId(), k -> new ArrayList<>()).add(pa);
                }

                Map<Long, List<com.nova.mcart.entity.ProductVariant>> varsByPid = new HashMap<>();
                for (var v : variants) {
                    varsByPid.computeIfAbsent(v.getProduct().getId(), k -> new ArrayList<>()).add(v);
                }

                // ✅ batch resolve breadcrumb info
                Map<Long, CategoryPathInfo> categoryInfoByProductId =
                        categoryPathResolver.resolveForProducts(products);

                List<ProductSearchDocument> docs = new ArrayList<>(products.size());
                for (Product p : products) {

                    CategoryPathInfo catInfo = categoryInfoByProductId.get(p.getId());

                    docs.add(mapper.toDocument(
                            p,
                            catInfo,
                            attrsByPid.getOrDefault(p.getId(), List.of()),
                            varsByPid.getOrDefault(p.getId(), List.of())
                    ));
                }

                client.bulk(b -> {
                    b.index(props.getAlias());
                    for (ProductSearchDocument d : docs) {
                        b.operations(op -> op.index(i -> i
                                .id(String.valueOf(d.getId()))
                                .document(d)
                        ));
                    }
                    return b;
                });

                job.incIndexed(docs.size());

                if (pageData.isLast()) break;
                page++;
            }

            job.completed();
            log.info("Reindex completed jobId={}", jobId);

        } catch (Exception ex) {
            job.failed(ex.getMessage());
            log.error("Reindex failed jobId={}", jobId, ex);
        }
    }

    public Optional<ReindexJob> getJob(String jobId) {
        return jobStore.get(jobId);
    }
}
