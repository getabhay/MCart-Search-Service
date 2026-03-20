package com.nova.mcart.service.impl;

import com.nova.mcart.dto.request.ProductCreateRequest;
import com.nova.mcart.service.ProductCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductBulkIngestionService {

    private final ProductCommandService productCommandService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSingle(ProductCreateRequest request) {
        productCommandService.createProduct(request);
    }
}