package com.wms.billing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouses")
@Setter
@Getter
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String warehouseId; // comes from request

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_sheet_id")
    private RateSheet rateSheet;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarehouseCharges> charges = new ArrayList<>();
}
