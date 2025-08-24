package com.wms.billing.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Builder
@Entity
@Table(name = "rate_sheets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "warehouses")
public class RateSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Postgres sequence
    private Long rateSheetId;

    @NotBlank
    private String customerId;

    @NotBlank
    private String name;

    @OneToMany(mappedBy = "rateSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Warehouse> warehouses = new ArrayList<>();

}