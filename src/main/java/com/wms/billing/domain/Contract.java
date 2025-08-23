package com.wms.billing.domain;

import lombok.Data;
import java.util.List;

@Data
public class Contract {
    private Long contractId;
    private String customerId;
    private String name;
    private List<WarehouseCharges> warehouses;
}
