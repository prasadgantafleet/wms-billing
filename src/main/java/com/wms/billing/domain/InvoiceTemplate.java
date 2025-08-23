package com.wms.billing.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "invoice_template")
@Data
public class InvoiceTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String filePath;
    private Boolean isActive;
}
