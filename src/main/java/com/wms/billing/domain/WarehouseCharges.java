package com.wms.billing.domain;

import lombok.Data;

import java.util.List;


@Data
public class WarehouseCharges {
    private String warehouseId;
    private List<Charge> charges;
}
