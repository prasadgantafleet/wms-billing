package com.wms.billing.domain;

import com.wms.billing.dto.AccessorialType;
import com.wms.billing.dto.ChargeCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Table(name = "warehouse_charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseCharges {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Internal PK, not exposed in API

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeCategory category;

    private String type;
    private BigDecimal rate;
    private String unit;

    // For Accessorial charges only
    @Enumerated(EnumType.STRING)
    private AccessorialType accessorialType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @PrePersist
    @PreUpdate
    private void validateInvariants() {
        if (category == ChargeCategory.ACCESSORIAL) {
            if (accessorialType == null) {
                throw new IllegalStateException("accessorialType is required when category=ACCESSORIAL");
            }
            // Optional: normalize non-accessorial 'type' to null for ACCESSORIAL
            // type = null;
        } else {
            // accessorialType must not be set for non-accessorial categories
            if (accessorialType != null) {
                throw new IllegalStateException("accessorialType must be null when category!="
                        + ChargeCategory.ACCESSORIAL);
            }
            // Optional: ensure a 'type' is present for non-accessorial charges
            if (type == null || type.isBlank()) {
                throw new IllegalStateException("type is required when category!="
                        + ChargeCategory.ACCESSORIAL);
            }
        }
    }


}
