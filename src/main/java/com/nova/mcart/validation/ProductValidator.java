package com.nova.mcart.validation;

import com.nova.mcart.dto.request.ProductCreateRequest;

public interface ProductValidator {

    void validateCreate(ProductCreateRequest request);

}
