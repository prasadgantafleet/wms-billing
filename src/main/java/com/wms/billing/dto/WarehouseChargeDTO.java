package com.wms.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseChargeDTO {

    @NotNull
    private String category;  // STORAGE, INBOUND, OUTBOUND, LABOR, ACCESSORIAL

    private String type;      // Required for non-accessorials
    private BigDecimal rate;
    private String unit;

    private AccessorialType accessorialType; // Required if category = ACCESSORIAL
}