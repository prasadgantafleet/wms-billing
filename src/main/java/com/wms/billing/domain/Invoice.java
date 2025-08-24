package com.wms.billing.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "invoice")
@Data
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rateSheetId;
    private String customerId;
    private String warehouseId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalAmount;
    private String status; // PREVIEW, FINAL

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<InvoiceLine> invoiceLines;
}
