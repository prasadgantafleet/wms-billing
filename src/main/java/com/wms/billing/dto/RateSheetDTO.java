package com.wms.billing.dto;

import lombok.Data;

import java.util.List;

@Data
public class RateSheetDTO {
    private Long id;
    private String customerId;
    private String name;
    private List<WarehouseDTO> warehouses;
}