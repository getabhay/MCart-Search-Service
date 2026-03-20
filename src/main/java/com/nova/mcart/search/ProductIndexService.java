package com.nova.mcart.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.nova.mcart.config.props.ProductSearchProperties;
import com.nova.mcart.entity.Product;
import com.nova.mcart.entity.ProductAttribute;
import com.nova.mcart.entity.ProductVariant;
import com.nova.mcart.repository.ProductAttributeRepository;
import com.nova.mcart.repository.ProductRepository;
import com.nova.mcart.repository.ProductVariantRepository;
import com.nova.mcart.search.CategoryPathResolver.CategoryPathResolved;
import com.nova.mcart.search.document.CategoryPathInfo;
import com.nova.mcart.search.document.ProductSearchDocument;
import com.nova.mcart.search.mapper.ProductDocumentMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final ElasticsearchClient client;
    private final ProductSearchProperties props;

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductAttributeRepository productAttributeRepository;

    private final CategoryPathResolver categoryPathResolver;
    private final ProductDocumentMapper mapper;

    public void indexProduct(Long productId) throws IOException {
        Product p = productRepository.findBaseForIndexingById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        List<ProductVariant> variants = productVariantRepository.findAllByProductIdInFetch(List.of(productId));
        List<ProductAttribute> attrs = productAttributeRepository.findAllByProductIdInFetch(List.of(productId));

        p.setVariants(variants);
        p.setProductAttributes(attrs);

        String path = (p.getCategory() != null) ? p.getCategory().getPath() : null;
        Map<Long, String> map = Map.of(p.getId(), path);

        CategoryPathResolved resolved = categoryPathResolver.resolveForProducts(map).get(p.getId());
        CategoryPathInfo info = new CategoryPathInfo(resolved.names(), resolved.slugs());

        ProductSearchDocument doc = mapper.map(p, info);

        client.index(i -> i.index(props.getAlias()).id(productId.toString()).document(doc));
        log.info("Indexed product id={}", productId);
    }

    public void bulkReindexAll() throws IOException {
        List<Product> products = productRepository.findAllBaseForIndexing();
        if (products.isEmpty()) {
            log.info("No products found for indexing");
            return;
        }

        List<Long> ids = products.stream().map(Product::getId).toList();

        Map<Long, List<ProductVariant>> variantsByProductId =
                productVariantRepository.findAllByProductIdInFetch(ids).stream()
                        .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

        Map<Long, List<ProductAttribute>> attrsByProductId =
                productAttributeRepository.findAllByProductIdInFetch(ids).stream()
                        .collect(Collectors.groupingBy(pa -> pa.getProduct().getId()));

        Map<Long, String> productIdToCategoryPath = new HashMap<>();
        for (Product p : products) {
            String path = (p.getCategory() != null) ? p.getCategory().getPath() : null;
            productIdToCategoryPath.put(p.getId(), path);
        }

        Map<Long, CategoryPathResolved> categoryResolvedByProductId =
                categoryPathResolver.resolveForProducts(productIdToCategoryPath);

        for (Product p : products) {
            p.setVariants(variantsByProductId.getOrDefault(p.getId(), List.of()));
            p.setProductAttributes(attrsByProductId.getOrDefault(p.getId(), List.of()));

            CategoryPathResolved resolved = categoryResolvedByProductId.get(p.getId());
            CategoryPathInfo info = (resolved == null)
                    ? new CategoryPathInfo(List.of(), List.of())
                    : new CategoryPathInfo(resolved.names(), resolved.slugs());

            ProductSearchDocument doc = mapper.map(p, info);

            client.index(i -> i.index(props.getAlias()).id(p.getId().toString()).document(doc));
        }

        log.info("Bulk reindex completed. Count={}", products.size());
    }
}
