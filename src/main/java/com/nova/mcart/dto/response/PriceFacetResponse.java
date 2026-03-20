package com.nova.mcart.dto.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceFacetResponse {
    private BigDecimal min;
    private BigDecimal max;
}
