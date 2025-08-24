package com.wms.billing.dto;

import lombok.Data;

import java.util.List;

@Data
public class WarehouseDTO {

    private String warehouseId;
    private List<WarehouseChargeDTO> charges;
}
