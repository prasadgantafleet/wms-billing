package com.wms.billing.validator;

import com.wms.billing.dto.WarehouseChargeDTO;
import org.springframework.stereotype.Component;

@Component
public class WarehouseChargeValidator {
    public void validate(WarehouseChargeDTO charge) {
        if (charge.getCategory() == null || charge.getCategory().isEmpty()) {
            throw new IllegalArgumentException("Charge category cannot be null or empty");
        }

        if ("ACCESSORIAL".equalsIgnoreCase(charge.getCategory()) && charge.getAccessorialType() == null) {
            throw new IllegalArgumentException("Accessorial charge must have an accessorialType");
        }

        if (charge.getRate() == null || charge.getRate().doubleValue() <= 0) {
            throw new IllegalArgumentException("Charge rate must be greater than 0");
        }

        if (charge.getUnit() == null || charge.getUnit().isEmpty()) {
            throw new IllegalArgumentException("Charge unit cannot be null or empty");
        }
    }
}
