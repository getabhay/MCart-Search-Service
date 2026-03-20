package com.nova.mcart.search;

import com.nova.mcart.entity.Category;
import com.nova.mcart.entity.Product;
import com.nova.mcart.repository.CategoryRepository;
import com.nova.mcart.search.document.CategoryPathInfo;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryPathResolver {

    private final CategoryRepository categoryRepository;

    public Map<Long, CategoryPathResolved> resolveForProducts(Map<Long, String> productIdToCategoryPath) {

        Set<Long> allCategoryIds = new LinkedHashSet<>();
        for (String path : productIdToCategoryPath.values()) {
            if (path == null || path.isBlank()) continue;
            Arrays.stream(path.split("/"))
                    .filter(s -> s != null && !s.isBlank())
                    .map(Long::valueOf)
                    .forEach(allCategoryIds::add);
        }

        Map<Long, Category> categoryById = new HashMap<>();
        if (!allCategoryIds.isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(allCategoryIds);
            for (Category c : categories) {
                categoryById.put(c.getId(), c);
            }
        }

        Map<Long, CategoryPathResolved> out = new HashMap<>();
        for (Map.Entry<Long, String> e : productIdToCategoryPath.entrySet()) {
            Long productId = e.getKey();
            String path = e.getValue();

            if (path == null || path.isBlank()) {
                out.put(productId, new CategoryPathResolved(List.of(), List.of()));
                continue;
            }

            List<String> names = new ArrayList<>();
            List<String> slugs = new ArrayList<>();

            for (String part : path.split("/")) {
                if (part == null || part.isBlank()) continue;

                Long id = Long.valueOf(part);
                Category c = categoryById.get(id);
                if (c != null) {
                    names.add(Objects.toString(c.getName(), ""));
                    slugs.add(Objects.toString(c.getSlug(), ""));
                }
            }

            out.put(productId, new CategoryPathResolved(names, slugs));
        }

        return out;
    }

    public record CategoryPathResolved(List<String> names, List<String> slugs) {}

    public Map<Long, CategoryPathInfo> resolveForProducts(List<Product> products) {

        Map<Long, CategoryPathInfo> out = new HashMap<>();
        if (products == null || products.isEmpty()) return out;

        // collect all category IDs present in all product paths
        Set<Long> allCatIds = new HashSet<>();
        Map<Long, List<Long>> productPathIds = new HashMap<>();

        for (Product p : products) {
            if (p.getCategory() == null || p.getCategory().getPath() == null) continue;

            List<Long> ids = parsePathIds(p.getCategory().getPath());
            productPathIds.put(p.getId(), ids);
            allCatIds.addAll(ids);
        }

        if (allCatIds.isEmpty()) return out;

        List<Category> cats = categoryRepository.findAllById(allCatIds);
        Map<Long, Category> byId = new HashMap<>();
        for (Category c : cats) byId.put(c.getId(), c);

        for (Map.Entry<Long, List<Long>> e : productPathIds.entrySet()) {
            Long productId = e.getKey();
            List<Long> ids = e.getValue();

            List<String> names = new ArrayList<>();
            List<String> slugs = new ArrayList<>();

            for (Long id : ids) {
                Category c = byId.get(id);
                if (c == null) continue;
                names.add(c.getName());
                slugs.add(c.getSlug());
            }

            out.put(productId, new CategoryPathInfo(names, slugs));
        }

        return out;
    }

    private List<Long> parsePathIds(String path) {
        if (path == null || path.isBlank()) return List.of();
        String[] parts = path.split("/");
        List<Long> ids = new ArrayList<>(parts.length);
        for (String s : parts) {
            if (s == null || s.isBlank()) continue;
            ids.add(Long.valueOf(s));
        }
        return ids;
    }
}
