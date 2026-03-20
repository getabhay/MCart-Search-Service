package com.nova.mcart.common.bulk;

import com.nova.mcart.common.util.SlugGenerator;
import com.nova.mcart.entity.Attribute;
import com.nova.mcart.entity.enums.AttributeType;
import com.nova.mcart.repository.AttributeRepository;
import com.nova.mcart.service.aws.S3Service;
import com.nova.mcart.service.impl.BulkUploadJobTxService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AttributeBulkProcessor
        extends AbstractBulkUploadProcessor<Attribute> {

    private final AttributeRepository attributeRepository;

    public AttributeBulkProcessor(
            BulkUploadJobTxService bulkUploadJobTxService,
            S3Service s3Service,
            AttributeRepository attributeRepository) {

        super(bulkUploadJobTxService, s3Service);
        this.attributeRepository = attributeRepository;
    }

    @Override
    protected Attribute parseRow(String line) {

        String[] data = line.split(",");

        String name = data[0].trim();
        String type = data[1].trim();
        Boolean isFilterable = Boolean.valueOf(data[2].trim());
        Boolean isVariant = Boolean.valueOf(data[4].trim());
        Boolean isSearchable = Boolean.valueOf(data[3].trim());
        Boolean isRequired = Boolean.valueOf(data[5].trim());

        Attribute attribute = new Attribute();
        attribute.setName(name);
        attribute.setSlug(generateUniqueSlug(name));
        attribute.setDataType(AttributeType.valueOf(type));
        attribute.setIsFilterable(isFilterable);
        attribute.setIsRequired(isRequired);
        attribute.setIsSearchable(isSearchable);
        attribute.setIsVariant(isVariant);
        attribute.setIsActive(true);

        return attribute;
    }

    @Override
    protected void saveBatch(List<Attribute> batch) {
        attributeRepository.saveAll(batch);
    }

    @Override
    protected String getFailedFolder() {
        return "attribute/failed_result";
    }

    @Override
    protected String getFailedHeader() {
        return "name,type,error";
    }

    private String generateUniqueSlug(String name) {

        String baseSlug = SlugGenerator.generate(name);
        String slug = baseSlug;
        int counter = 1;

        while (attributeRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }
}
