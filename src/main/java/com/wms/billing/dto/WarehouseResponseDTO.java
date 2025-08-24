package com.wms.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponseDTO {

    private String warehouseId;
    private String warehouseName;   // optional, in case you want to show human-readable name
    private List<WarehouseChargeDTO> charges;

}