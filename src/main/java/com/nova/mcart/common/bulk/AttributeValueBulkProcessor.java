package com.nova.mcart.common.bulk;

import com.nova.mcart.common.util.SlugGenerator;
import com.nova.mcart.entity.Attribute;
import com.nova.mcart.entity.AttributeValue;
import com.nova.mcart.repository.AttributeRepository;
import com.nova.mcart.repository.AttributeValueRepository;
import com.nova.mcart.service.aws.S3Service;
import com.nova.mcart.service.impl.BulkUploadJobTxService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AttributeValueBulkProcessor
        extends AbstractBulkUploadProcessor<AttributeValue> {

    private final AttributeValueRepository attributeValueRepository;
    private final AttributeRepository attributeRepository;
    private final static Map<String, Integer> attributeValueOrderCounter = new ConcurrentHashMap<>();

    public AttributeValueBulkProcessor(
            BulkUploadJobTxService bulkUploadJobTxService,
            S3Service s3Service,
            AttributeValueRepository attributeValueRepository,
            AttributeRepository attributeRepository) {

        super(bulkUploadJobTxService, s3Service);
        this.attributeValueRepository = attributeValueRepository;
        this.attributeRepository = attributeRepository;
    }

    @Override
    protected AttributeValue parseRow(String line) {

        // CSV FORMAT:
        // attribute_slug,value

        String[] data = line.split(",");

        String attributeSlug = data[0].trim();
        String value = data[1].trim();

        Attribute attribute =
                attributeRepository.findBySlugAndIsActiveTrue(attributeSlug)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Attribute not found"));

        AttributeValue attributeValue = new AttributeValue();
        attributeValue.setAttribute(attribute);
        attributeValue.setSlug(SlugGenerator.generate(value));
        attributeValue.setValue(value);
        attributeValue.setSortOrder(attributeValueOrderCounter.merge(attributeValue.getSlug(), 1, Integer::sum));

        attributeValue.setIsActive(true);

        return attributeValue;
    }

    @Override
    protected void saveBatch(List<AttributeValue> batch) {
        attributeValueRepository.saveAll(batch);
    }

    @Override
    protected String getFailedFolder() {
        return "attribute-value/failed_result";
    }

    @Override
    protected String getFailedHeader() {
        return "attribute_slug,value,error";
    }
}
