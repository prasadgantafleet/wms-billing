package com.wms.billing.domain;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Charge {
    private ChargeCategory category;
    private String type;
    private BigDecimal rate;
    private String unit;
}
