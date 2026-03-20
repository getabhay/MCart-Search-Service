package com.nova.mcart.controller;

import com.nova.mcart.dto.response.ProductSuggestResponse;
import com.nova.mcart.search.ProductSuggestService;
import java.io.IOException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class ProductSuggestController {

    private final ProductSuggestService service;

    public ProductSuggestController(ProductSuggestService service) {
        this.service = service;
    }

    @GetMapping("/suggest")
    public ProductSuggestResponse suggest(@RequestParam(value = "q", required = false) String q) throws IOException {
        if (q == null || q.isBlank()) {
            ProductSuggestResponse out = new ProductSuggestResponse();
            out.setQ(q);
            out.setCorrectedQuery(null);
            out.setDidYouMean(java.util.List.of());
            out.setUsedQuery(q);
            out.setProducts(java.util.List.of());
            out.setBrands(java.util.List.of());
            out.setCategories(java.util.List.of());
            return out;
        }
        return service.suggest(q);
    }
}
