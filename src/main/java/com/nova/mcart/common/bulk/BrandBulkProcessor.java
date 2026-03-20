package com.nova.mcart.common.bulk;

import com.nova.mcart.common.util.SlugGenerator;
import com.nova.mcart.entity.Brand;
import com.nova.mcart.repository.BrandRepository;
import com.nova.mcart.service.aws.S3Service;
import com.nova.mcart.service.impl.BulkUploadJobTxService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BrandBulkProcessor
        extends AbstractBulkUploadProcessor<Brand> {

    private final BrandRepository brandRepository;

    public BrandBulkProcessor(
            BulkUploadJobTxService bulkUploadJobTxService,
            S3Service s3Service,
            BrandRepository brandRepository) {

        super(bulkUploadJobTxService, s3Service);
        this.brandRepository = brandRepository;
    }

    @Override
    protected Brand parseRow(String line) {

        String name = line.trim();

        Brand brand = new Brand();
        brand.setName(name);
        brand.setSlug(generateUniqueSlug(name));
        brand.setIsActive(true);

        return brand;
    }

    @Override
    protected void saveBatch(List<Brand> batch) {
        brandRepository.saveAll(batch);
    }

    @Override
    protected String getFailedFolder() {
        return "brand/failed_result";
    }

    @Override
    protected String getFailedHeader() {
        return "name,error";
    }

    private String generateUniqueSlug(String name) {

        String baseSlug = SlugGenerator.generate(name);
        String slug = baseSlug;
        int counter = 1;

        while (brandRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }
}
