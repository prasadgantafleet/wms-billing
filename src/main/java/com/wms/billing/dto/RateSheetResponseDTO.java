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
public class RateSheetResponseDTO {

    private Long rateSheetId;
    private String customerId;
    private String name;
    private List<WarehouseDTO> warehouses;

}