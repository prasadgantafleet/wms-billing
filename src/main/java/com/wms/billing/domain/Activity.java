package com.wms.billing.domain;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Activity {
    private String type;
    private BigDecimal quantity;
    private String warehouseId;   // WH001, WH002 etc.
    private String category;      // STORAGE, INBOUND, OUTBOUND, LABOR, ACCESSORIAL
    private BigDecimal amount;    // calculated by rules

}
