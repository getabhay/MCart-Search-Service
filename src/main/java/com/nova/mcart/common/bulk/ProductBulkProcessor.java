package com.nova.mcart.common.bulk;

import com.nova.mcart.common.util.SlugGenerator;
import com.nova.mcart.dto.request.ProductCreateRequest;
import com.nova.mcart.entity.BulkUploadJob;
import com.nova.mcart.entity.enums.ImageType;
import com.nova.mcart.entity.enums.JobStatus;
import com.nova.mcart.entity.enums.ProductStatus;
import com.nova.mcart.repository.*;
import com.nova.mcart.service.aws.ImageIngestService;
import com.nova.mcart.service.aws.S3Service;
import com.nova.mcart.service.impl.BulkUploadJobTxService;
import com.nova.mcart.service.impl.ProductBulkIngestionService;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductBulkProcessor {

    private static final String FAILED_FOLDER = "product/failed_result";

    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final S3Service s3Service;

    private final ProductBulkIngestionService bulkIngestionService;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeRepository attributeRepository;
    private final ImageIngestService imageIngestService;
    private final ProductRepository productRepository;

    @Value("${spring.cloud.aws.s3.bucket-name}")
    private String bulkBucketName;

    // run-scope caches (cleared at end)
    private final Map<String, ProductCreateRequest> productMap = new LinkedHashMap<>();
    private final Map<String, String> urlToS3KeyCache = new HashMap<>();
    private final BulkUploadJobTxService jobTxService;

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void process(Long jobId) {

        if (!jobTxService.markProcessingIfPending(jobId)) {
            return;
        }

        BulkUploadJob job = jobTxService.getJobOrNull(jobId);
        if (job == null) {
            return;
        }

        int totalRows = 0;
        int parseFailures = 0;
        int createdSuccess = 0;
        int createFailures = 0;

        List<String> failed = new ArrayList<>();
        String failedKey = null;

        try {
            String s3Key = job.getFileUrl();

            // ========================
            // PARSE STAGE (CSV SAFE)
            // ========================
            try (InputStream inputStream = s3Service.download(s3Key);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 CSVParser parser = CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .setTrim(true)
                         .setIgnoreEmptyLines(true)
                         .build()
                         .parse(reader)) {

                for (CSVRecord record : parser) {
                    totalRows++;

                    try {
                        parseRecordIntoRequest(record);
                    } catch (Exception ex) {
                        parseFailures++;
                        failed.add(toFailedRow(
                                "PARSE",
                                ex,
                                safeGet(record, "product_slug"),
                                safeGet(record, "sku"),
                                record.toMap().toString()
                        ));
                    }
                }
            }

            // ===========================
            // CREATE STAGE (REQUIRES_NEW)
            // ===========================
            for (Map.Entry<String, ProductCreateRequest> entry : productMap.entrySet()) {

                String productSlug = entry.getKey();
                ProductCreateRequest request = entry.getValue();

                try {
                    bulkIngestionService.createSingle(request); // ✅ REQUIRES_NEW
                    createdSuccess++;

                } catch (IllegalArgumentException | IllegalStateException ex) {
                    createFailures++;
                    failed.add(toFailedRow("VALIDATION", ex, productSlug, "", ""));

                } catch (Exception ex) {
                    createFailures++;
                    failed.add(toFailedRow("EXCEPTION", ex, productSlug, "", ""));
                }
            }

            if (!failed.isEmpty()) {
                failedKey = FAILED_FOLDER + "/failed_" + jobId + ".csv";
                uploadFailedCsv(failedKey, failed);
            }

            jobTxService.updateFinal(
                    jobId,
                    JobStatus.COMPLETED,
                    totalRows,
                    createdSuccess,
                    parseFailures + createFailures,
                    failedKey
            );

        } catch (Exception ex) {

            failedKey = FAILED_FOLDER + "/failed_" + jobId + ".csv";
            failed.add(toFailedRow("JOB", ex, "", "", ""));
            uploadFailedCsv(failedKey, failed);

            jobTxService.updateFinal(
                    jobId,
                    JobStatus.FAILED,
                    totalRows,
                    createdSuccess,
                    Math.max(1, failed.size()),
                    failedKey
            );

        } finally {
            productMap.clear();
            urlToS3KeyCache.clear();
        }
    }

    /**
     * Runs in its own small transaction (safe even in async).
     */
    @Transactional
    protected int markJobProcessing(Long jobId) {
        int i = bulkUploadJobRepository.markProcessingIfPending(jobId);
        return i;
    }

    private String generateUniqueSlug(String name) {
        String base = SlugGenerator.generate(name);
        String slug = base;
        int counter = 1;

        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private void parseRecordIntoRequest(CSVRecord record) {

//        String productSlug = safeGet(record, "product_slug");

        String name = safeGet(record, "name");
        String productSlug = generateUniqueSlug(name);
        String shortDesc = safeGet(record, "short_description");
        String description = safeGet(record, "description");

        String brandSlug = safeGet(record, "brand_slug");
        String categorySlug = safeGet(record, "category_slug");
        ProductStatus productStatus = ProductStatus.valueOf(safeGet(record, "product_status"));

        String productAttrRaw = safeGet(record, "product_attributes");

        String sku = safeGet(record, "sku");
        BigDecimal mrp = new BigDecimal(safeGet(record, "mrp"));
        BigDecimal sellingPrice = new BigDecimal(safeGet(record, "selling_price"));
        Integer stockQuantity = Integer.parseInt(safeGet(record, "stock_quantity"));

        ProductStatus variantStatus = ProductStatus.valueOf(safeGet(record, "variant_status"));
        String variantAttrRaw = safeGet(record, "variant_attributes");
        String variantImagesRaw = safeGet(record, "variant_images");

        String productImagesRaw = record.isMapped("product_images") ? safeGet(record, "product_images") : "";

        Long brandId = brandRepository.findBySlug(brandSlug)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + brandSlug))
                .getId();

        Long categoryId = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categorySlug))
                .getId();

        ProductCreateRequest request = productMap.computeIfAbsent(productSlug, k -> {

            ProductCreateRequest r = new ProductCreateRequest();
            r.setName(name);
            r.setShortDescription(shortDesc);
            r.setDescription(description);
            r.setBrandId(brandId);
            r.setCategoryId(categoryId);
            r.setStatus(productStatus);

            r.setAttributes(parseProductAttributes(productAttrRaw));
            r.setImages(parseImagesFromUrls(productImagesRaw, "product/images/" + productSlug + "/product"));
            r.setVariants(new ArrayList<>());

            return r;
        });

        ProductCreateRequest.VariantRequest variant = new ProductCreateRequest.VariantRequest();
        variant.setSku(sku);
        variant.setMrp(mrp);
        variant.setSellingPrice(sellingPrice);
        variant.setStockQuantity(stockQuantity);
        variant.setStatus(variantStatus);

        variant.setAttributes(parseVariantAttributes(variantAttrRaw));
        variant.setImages(parseImagesFromUrls(variantImagesRaw, "product/images/" + productSlug + "/variant/" + sku));

        request.getVariants().add(variant);
    }

    private List<ProductCreateRequest.ProductAttributeRequest> parseProductAttributes(String raw) {

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<ProductCreateRequest.ProductAttributeRequest> list = new ArrayList<>();

        for (String entry : raw.split("\\|")) {

            String[] parts = entry.split(":", 2);
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid product attribute: " + entry);
            }

            ProductCreateRequest.ProductAttributeRequest attr =
                    new ProductCreateRequest.ProductAttributeRequest();

            attr.setAttributeSlug(parts[0].trim());
            attr.setValue(parts[1].trim());
            list.add(attr);
        }

        return list;
    }

    private List<ProductCreateRequest.VariantAttributeRequest> parseVariantAttributes(String raw) {

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<ProductCreateRequest.VariantAttributeRequest> list = new ArrayList<>();

        for (String entry : raw.split("\\|")) {

            String[] parts = entry.split(":", 2);
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid variant attribute: " + entry);
            }

            String key = parts[0].trim();
            String value = parts[1].trim();

            Long attributeId = key.matches("\\d+")
                    ? Long.parseLong(key)
                    : attributeRepository.findBySlugAndIsActiveTrue(key)
                    .orElseThrow(() -> new IllegalArgumentException("Attribute not found: " + key))
                    .getId();

            ProductCreateRequest.VariantAttributeRequest attr =
                    new ProductCreateRequest.VariantAttributeRequest();

            attr.setAttributeId(attributeId);
            attr.setValue(value);
            list.add(attr);
        }

        return list;
    }

    private List<ProductCreateRequest.ProductImageRequest> parseImagesFromUrls(String raw, String keyPrefix) {

        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<ProductCreateRequest.ProductImageRequest> list = new ArrayList<>();
        int order = 1;

        for (String url : raw.split("\\|")) {

            String trimmed = url == null ? "" : url.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            try {
                String s3Key = urlToS3KeyCache.computeIfAbsent(trimmed, u ->
                        imageIngestService.ingestToS3(u, keyPrefix)
                );

                ProductCreateRequest.ProductImageRequest img =
                        new ProductCreateRequest.ProductImageRequest();

                img.setFileKey(s3Key);
                img.setImageType(ImageType.GALLERY);
                img.setDisplayOrder(order++);

                list.add(img);

            } catch (Exception ignored) {
                // best-effort: skip bad URL and continue
            }
        }

        return list;
    }

    private void uploadFailedCsv(String key, List<String> rows) {

        StringBuilder builder = new StringBuilder();
        builder.append(getFailedHeader()).append("\n");

        for (String r : rows) {
            builder.append(r).append("\n");
        }

        byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);

        s3Service.uploadFile(
                key,
                new ByteArrayInputStream(bytes),
                bytes.length,
                "text/csv",
                bulkBucketName
        );
    }

    private String getFailedHeader() {
        return "stage,error_type,error_message,product_slug,sku,raw_row";
    }

    private String toFailedRow(String stage, Exception ex, String productSlug, String sku, String rawRow) {

        String errorType = ex.getClass().getSimpleName();
        String msg = sanitize(ex.getMessage());
        String row = sanitize(rawRow);

        return csv(stage) + "," +
                csv(errorType) + "," +
                csv(msg) + "," +
                csv(productSlug) + "," +
                csv(sku) + "," +
                csv(row);
    }

    private String safeGet(CSVRecord record, String key) {
        if (!record.isMapped(key)) {
            return "";
        }
        String v = record.get(key);
        return v == null ? "" : v.trim();
    }

    private String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\n", " ").replace("\r", " ").trim();
    }

    private String csv(String v) {
        if (v == null) {
            v = "";
        }
        v = v.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }
}
