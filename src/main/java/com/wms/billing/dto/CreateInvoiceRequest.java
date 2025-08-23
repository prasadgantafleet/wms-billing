package com.wms.billing.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateInvoiceRequest {
    private String warehouseId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private List<BillingActivityDTO> activities;
}
