package com.wms.billing.controller;

import com.wms.billing.domain.RateSheet;
import com.wms.billing.dto.RateSheetDTO;
import com.wms.billing.dto.RateSheetResponseDTO;
import com.wms.billing.helper.EntityMapper;
import com.wms.billing.service.RateSheetService;
import com.wms.billing.validator.WarehouseChargeValidator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratesheet")
public class RateSheetController {

    @Autowired
    private RateSheetService rateSheetService;

    @Autowired
    private  WarehouseChargeValidator warehouseChargeValidator;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createContract(@RequestBody @Valid RateSheetDTO rateSheetRequest) {
        //  Validate each warehouse's charges
        validateCharges(rateSheetRequest);

        //  Map DTO → Entity
        RateSheet rateSheet = EntityMapper.toEntity(rateSheetRequest);

        RateSheet result = rateSheetService.createRateSheet(rateSheet);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Contract " + result.getRateSheetId() + " created successfully" + "for the customer" +result.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{rateSheetId}")
    public ResponseEntity<Map<String, Object>> updateContract(@PathVariable Long rateSheetId,
                                                              @RequestBody @Valid RateSheetDTO rateSheetRequest) {
        validateCharges(rateSheetRequest);

        RateSheet rateSheet = EntityMapper.toEntity(rateSheetRequest);
        // Ensure path id is authoritative
        rateSheet.setRateSheetId(rateSheetId);

        RateSheet updated = rateSheetService.updateRateSheet(rateSheetId, rateSheet);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Contract " + updated.getRateSheetId() + " updated successfully for the customer " + updated.getName());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/search")
    public ResponseEntity<RateSheetResponseDTO> getRateSheetByCustomerAndWarehouse(
            @RequestParam @Valid String customerId,
            @RequestParam(required = false) String customerName,
            // Accept multiple: ?warehouseId=WH01&warehouseId=WH02 or comma-separated ?warehouseId=WH01,WH02
            @RequestParam(name = "warehouseId") List<String> warehouseIds) {

        if (warehouseIds == null || warehouseIds.isEmpty()) {
            throw new IllegalArgumentException("At least one warehouseId is required");
        }

        RateSheetResponseDTO rateSheet = rateSheetService.getRateSheetByCustomerAndWarehouses(customerId, customerName, warehouseIds);
        return ResponseEntity.ok(rateSheet);
    }


    // Helpers
    private void validateCharges(RateSheetDTO dto) {
        if (dto.getWarehouses() != null) {
            dto.getWarehouses().forEach(warehouseDTO -> {
                if (!warehouseDTO.getCharges().isEmpty()) {
                    warehouseDTO.getCharges().forEach(warehouseChargeValidator::validate);
                }
            });
        }
    }

}
