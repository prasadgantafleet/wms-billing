package com.wms.billing.service;

import com.wms.billing.domain.RateSheet;
import com.wms.billing.domain.Warehouse;
import com.wms.billing.dto.RateSheetResponseDTO;
import com.wms.billing.exception.RateSheetNotFoundException;
import com.wms.billing.helper.EntityMapper;
import com.wms.billing.repository.RateSheetRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateSheetServiceImpl implements RateSheetService {

    private final RateSheetRepository rateSheetRepository;

    @Override
    @Transactional
    public RateSheet createRateSheet(RateSheet rateSheetRequest) {
        return rateSheetRepository.save(rateSheetRequest);
    }

    @Override
    public RateSheetResponseDTO getRateSheetByCustomerAndWarehouses(String customerId, String customerName, List<String> warehouseIds) {
        // Normalize incoming ids (trim, dedup)
        Set<String> ids = warehouseIds.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(HashSet::new));

        if (ids.isEmpty()) {
            throw new IllegalArgumentException("At least one valid warehouseId is required");
        }

        List<RateSheet> matches = rateSheetRepository.findByCustomerAndAllWarehouseIds(
                customerId,
                customerName,
                ids,
                ids.size()
        );

        if (matches.isEmpty()) {
            throw new RateSheetNotFoundException("RateSheet not found for CustomerId=" + customerId +
                    ", CustomerName=" + customerName + ", WarehouseIds=" + ids);
        }
        if (matches.size() > 1) {
            // Ambiguous data. Make criteria stricter or enforce uniqueness at the data level.
            throw new IllegalStateException("Multiple RateSheets matched for CustomerId=" + customerId +
                    ", CustomerName=" + customerName + ", WarehouseIds=" + ids +
                    ". Provide a unique customerName or ensure data uniqueness.");
        }

        RateSheet rateSheet = matches.get(0);

        // Map to DTO first, then filter warehouses in DTO to only the requested ids
        RateSheetResponseDTO dto = EntityMapper.toResponseDTO(rateSheet);
        if (dto.getWarehouses() != null) {
            dto.setWarehouses(
                    dto.getWarehouses().stream()
                            .filter(w -> w.getWarehouseId() != null && ids.contains(w.getWarehouseId().trim()))
                            .collect(Collectors.toList())
            );
        }
        return dto;

    }


    @Override
    @Transactional
    public RateSheet updateRateSheet(Long rateSheetId, RateSheet rateSheetRequest) {
        RateSheet existing = rateSheetRepository.findById(rateSheetId)
                .orElseThrow(() -> new RateSheetNotFoundException("RateSheet not found: " + rateSheetId));

        // Update simple fields
        existing.setCustomerId(rateSheetRequest.getCustomerId());
        existing.setName(rateSheetRequest.getName());

        // Merge warehouses by warehouseId (append new, update existing, keep others)
        if (rateSheetRequest.getWarehouses() != null) {
            if (existing.getWarehouses() == null) {
                existing.setWarehouses(new ArrayList<>());
            }

            // Index existing warehouses by business key: warehouseId
            Map<String, Warehouse> existingByWhId = existing.getWarehouses().stream()
                    .filter(w -> w.getWarehouseId() != null)
                    .collect(Collectors.toMap(
                            Warehouse::getWarehouseId,
                            Function.identity(),
                            (a, b) -> a,
                            java.util.LinkedHashMap::new
                    ));

            // Detect duplicates in incoming payload
           Set<String> seen = new HashSet<>();

            for (Warehouse incoming : rateSheetRequest.getWarehouses()) {
                if (incoming.getWarehouseId() == null || incoming.getWarehouseId().isBlank()) {
                    throw new IllegalArgumentException("warehouseId is required for each warehouse");
                }
                if (!seen.add(incoming.getWarehouseId())) {
                    throw new IllegalArgumentException("Duplicate warehouseId in request: " + incoming.getWarehouseId());
                }

                Warehouse target = existingByWhId.get(incoming.getWarehouseId());
                if (target == null) {
                    // New warehouse -> attach and set back-references
                    incoming.setId(null); // ignore any client-sent DB id
                    incoming.setRateSheet(existing);
                    if (incoming.getCharges() != null) {
                        incoming.getCharges().forEach(ch -> ch.setWarehouse(incoming));
                    }
                    existing.getWarehouses().add(incoming);
                } else {
                    // Update existing warehouse details
                    // Note: business key (warehouseId) should not change
                    // Merge/replace charges. Here we replace for clarity; orphanRemoval will delete removed ones
                    if (incoming.getCharges() != null) {
                        target.getCharges().clear();
                        incoming.getCharges().forEach(ch -> {
                            ch.setWarehouse(target);
                            target.getCharges().add(ch);
                        });
                    }
                    // If you have other mutable fields on Warehouse, set them here on 'target'
                }
            }

            // Optional: if you want to REMOVE warehouses that are not in the incoming list,

       Set<String> incomingWhIds = rateSheetRequest.getWarehouses().stream()
                .map(Warehouse::getWarehouseId)
                .collect(Collectors.toSet());
        existing.getWarehouses().removeIf(w -> !incomingWhIds.contains(w.getWarehouseId()));

        }

        return rateSheetRepository.save(existing);


    }

}