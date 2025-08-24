package com.wms.billing.helper;

import com.wms.billing.domain.RateSheet;
import com.wms.billing.domain.Warehouse;
import com.wms.billing.domain.WarehouseCharges;
import com.wms.billing.dto.ChargeCategory;
import com.wms.billing.dto.RateSheetDTO;
import com.wms.billing.dto.RateSheetResponseDTO;
import com.wms.billing.dto.WarehouseChargeDTO;
import com.wms.billing.dto.WarehouseDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    // DTO → Entity
    public static RateSheet toEntity(RateSheetDTO dto) {
        RateSheet rateSheet = new RateSheet();
        rateSheet.setCustomerId(dto.getCustomerId());
        rateSheet.setName(dto.getName());

        // Map warehouses
        List<Warehouse> warehouses = dto.getWarehouses().stream()
                .map(EntityMapper::toEntity)
                .collect(Collectors.toList());
        warehouses.forEach(wh -> wh.setRateSheet(rateSheet)); // Set parent ref

        rateSheet.setWarehouses(warehouses);

        return rateSheet;
    }

    private static Warehouse toEntity(WarehouseDTO dto) {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(dto.getWarehouseId());

        List<WarehouseCharges> charges = dto.getCharges().stream()
                .map(EntityMapper::toEntity)
                .collect(Collectors.toList());
        charges.forEach(c -> c.setWarehouse(warehouse)); // Set parent ref

        warehouse.setCharges(charges);

        return warehouse;
    }

    private static WarehouseCharges toEntity(WarehouseChargeDTO dto) {
        WarehouseCharges charge = new WarehouseCharges();
        charge.setCategory(ChargeCategory.valueOf(dto.getCategory()));
        charge.setType(dto.getType());
        charge.setRate(dto.getRate());
        charge.setUnit(dto.getUnit());
        charge.setAccessorialType(dto.getAccessorialType());
        return charge;
    }

    // Entity → ResponseDTO
    public static RateSheetResponseDTO toResponseDTO(RateSheet rateSheet) {
        RateSheetResponseDTO response = new RateSheetResponseDTO();
        response.setRateSheetId(rateSheet.getRateSheetId());
        response.setCustomerId(rateSheet.getCustomerId());
        response.setName(rateSheet.getName());

        // Map to WarehouseDTO to match RateSheetResponseDTO.setWarehouses(List<WarehouseDTO>)
        List<WarehouseDTO> warehouses = rateSheet.getWarehouses().stream()
                .map(EntityMapper::toWarehouseDTO)
                .collect(Collectors.toList());
        response.setWarehouses(warehouses);

        return response;
    }

    // Helper mappers for nested collections (Entity → DTO)
    private static WarehouseDTO toWarehouseDTO(Warehouse warehouse) {
        WarehouseDTO dto = new WarehouseDTO();
        dto.setWarehouseId(warehouse.getWarehouseId());

        List<WarehouseChargeDTO> charges = warehouse.getCharges().stream()
                .map(EntityMapper::toWarehouseChargeDTO)
                .collect(Collectors.toList());
        dto.setCharges(charges);

        return dto;
    }

    private static WarehouseChargeDTO toWarehouseChargeDTO(WarehouseCharges charge) {
        WarehouseChargeDTO dto = new WarehouseChargeDTO();
        dto.setCategory(charge.getCategory() != null ? charge.getCategory().name() : null);
        dto.setType(charge.getType());
        dto.setRate(charge.getRate());
        dto.setUnit(charge.getUnit());
        dto.setAccessorialType(charge.getAccessorialType());
        return dto;
    }
}