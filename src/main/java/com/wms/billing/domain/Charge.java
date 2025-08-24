package com.wms.billing.domain;

import com.wms.billing.dto.ChargeCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "charges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Internal PK

    // Map enum as STRING to match YAML values like STORAGE, INBOUND, etc.
    @Enumerated(EnumType.STRING)
    private ChargeCategory category;
    private String type;
    @Column(precision = 19, scale = 4)
    private BigDecimal rate;
    private String unit;
}
