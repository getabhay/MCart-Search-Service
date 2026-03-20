//package com.nova.mcart.dto.request;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
//import lombok.Getter;
//import lombok.Setter;
//
//@Getter
//@Setter
//public class ProductSearchRequest {
//
//    // full text query (name/brand/category/attributes)
//    private String q;
//
//    // category click filter (any node id)
//    private Long categoryId;
//
//    // category path prefix filter: "1/12" or "1/12/50"
//    private String categoryPathPrefix;
//
//    private Long brandId;
//
//    private BigDecimal minPrice;
//    private BigDecimal maxPrice;
//
//    private BigDecimal minRating;
//
////    // product-level attribute filters: attributeSlug -> valueSlug
////    // example: {"gender":"women", "material":"leather"}
////    private Map<String, String> attributes;
////
////    // variant-level attribute filters: attributeSlug -> valueSlug
////    // example: {"color":"black", "size":"m"}
////    private Map<String, String> variantAttributes;
//
//    // product-level attribute filters: attributeSlug -> list of valueSlugs
//// example: {"gender":["women"], "material":["leather","cotton"]}
//    private Map<String, List<String>> attributes;
//
//    // variant-level attribute filters: attributeSlug -> list of valueSlugs
//// example: {"color":["black","red"], "size":["m","l"]}
//    private Map<String, List<String>> variantAttributes;
//
//
//    // pagination
//    private Integer page = 0;
//    private Integer size = 20;
//
//    // sorting
//    // RELEVANCE | PRICE_ASC | PRICE_DESC | RATING_DESC | POPULARITY_DESC
//    private String sort = "RELEVANCE";
//}

package com.nova.mcart.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchRequest {

    // full text query (name/brand/category/attributes)
    private String q;

    // category click filter (any node id)
    private Long categoryId;

    // category path prefix filter: "1/12" or "1/12/50"
    private String categoryPathPrefix;

    private List<Long> categoryIds;
    private List<String> categoryPathPrefixes;
    private List<String> categorySlugs;

    // ✅ existing single brand filter (keep for backward compatibility)
    private Long brandId;

    // ✅ NEW: multi-select brand filters
    private List<Long> brandIds;
    private List<String> brandSlugs;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private BigDecimal minRating;

    // product-level attribute filters: attributeSlug -> list of valueSlugs
    private Map<String, List<String>> attributes;

    // variant-level attribute filters: attributeSlug -> list of valueSlugs
    private Map<String, List<String>> variantAttributes;

    // pagination
    private Integer page = 0;
    private Integer size = 20;

    // sorting
    // RELEVANCE | PRICE_ASC | PRICE_DESC | RATING_DESC | POPULARITY_DESC
    private String sort = "RELEVANCE";
}
