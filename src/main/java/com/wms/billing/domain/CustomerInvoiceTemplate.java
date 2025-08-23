package com.wms.billing.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "customer_invoice_template")
@Data
public class CustomerInvoiceTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerId;

    @ManyToOne
    private InvoiceTemplate template;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
