package com.wms.billing.domain;

import com.wms.billing.dto.ChargeCategory;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class Activity {
    private String type;
    private BigDecimal quantity;
    private String warehouseId;   // WH001, WH002 etc.

    @Enumerated(EnumType.STRING)
    private ChargeCategory category;     // STORAGE, INBOUND, OUTBOUND, LABOR, ACCESSORIAL
    private BigDecimal amount;    // calculated by rules

}
