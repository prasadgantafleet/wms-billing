package com.wms.billing.repository;

import com.wms.billing.domain.InvoiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceTemplateRepository extends JpaRepository<InvoiceTemplate, Long> {
    List<InvoiceTemplate> findByIsActiveTrue();
}
