package com.nova.mcart.controller;

import com.nova.mcart.dto.request.ProductSearchRequest;
import com.nova.mcart.dto.response.AutocompleteItemResponse;
import com.nova.mcart.dto.response.ProductSearchResponse;
import com.nova.mcart.search.ProductAutocompleteService;
import com.nova.mcart.search.ProductSearchService;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search/products")
public class ProductSearchController {

    private final ProductSearchService productSearchService;
    private final ProductAutocompleteService productAutocompleteService;

    @PostMapping
    public ProductSearchResponse search(@RequestBody ProductSearchRequest request) throws IOException {
        return productSearchService.search(request);
    }

    @GetMapping("/autocomplete")
    public List<AutocompleteItemResponse> autocomplete(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {

        if (q == null || q.isBlank()) {
            return List.of();
        }

        return productAutocompleteService.autocomplete(q, size);
    }
}
