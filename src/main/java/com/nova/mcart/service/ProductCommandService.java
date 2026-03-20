package com.nova.mcart.service;

import com.nova.mcart.dto.request.ProductCreateRequest;
import com.nova.mcart.dto.response.ProductResponse;

public interface ProductCommandService {

    ProductResponse createProduct(ProductCreateRequest request);
}
