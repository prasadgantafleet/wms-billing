package com.wms.billing.service;

import com.wms.billing.domain.RateSheet;
import com.wms.billing.dto.RateSheetResponseDTO;
import com.wms.billing.dto.SaveContractResult;

import java.util.List;


public interface RateSheetService {
    public RateSheet createRateSheet(RateSheet rateSheetRequest);
    // New: fetch by multiple warehouse ids
    RateSheetResponseDTO getRateSheetByCustomerAndWarehouses(String customerId, String customerName, List<String> warehouseIds);
    // Add update contract API
    RateSheet updateRateSheet(Long rateSheetId, RateSheet rateSheetRequest);

}
