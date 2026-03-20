package com.nova.mcart.service;

import com.nova.mcart.dto.request.CreateAttributeValueRequest;
import com.nova.mcart.entity.AttributeValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AttributeValueService {

    AttributeValue create(CreateAttributeValueRequest request);

    Page<AttributeValue> getByAttribute(Long attributeId, Pageable pageable);

    Long startBulkJob(String s3Key);
}
