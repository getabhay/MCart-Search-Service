package com.nova.mcart.service;

import com.nova.mcart.dto.request.CreateAttributeRequest;
import com.nova.mcart.dto.request.UpdateAttributeRequest;
import com.nova.mcart.entity.Attribute;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AttributeService {

    Attribute create(CreateAttributeRequest request);

    Attribute update(Long id, UpdateAttributeRequest request);

    Page<Attribute> getAll(Pageable pageable);

    Attribute getById(Long id);

    void delete(Long id);

    Long uploadAndProcessAsync(MultipartFile file) throws IOException;

    Long startBulkJob(String s3Key);
}
