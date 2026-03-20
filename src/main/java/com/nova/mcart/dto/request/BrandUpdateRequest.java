package com.nova.mcart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String name;
}
