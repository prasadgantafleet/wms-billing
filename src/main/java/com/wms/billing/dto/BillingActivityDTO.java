package com.wms.billing.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BillingActivityDTO {
    private String type;
    private String category;
    private BigDecimal quantity;
}
