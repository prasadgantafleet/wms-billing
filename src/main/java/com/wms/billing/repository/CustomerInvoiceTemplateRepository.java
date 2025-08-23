package com.wms.billing.repository;

import com.wms.billing.domain.CustomerInvoiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerInvoiceTemplateRepository extends JpaRepository<CustomerInvoiceTemplate, Long> {

    @Query("SELECT cit FROM CustomerInvoiceTemplate cit WHERE cit.customerId=:customerId AND (cit.effectiveTo IS NULL OR cit.effectiveTo>=CURRENT_DATE) ORDER BY cit.effectiveFrom DESC")
    List<CustomerInvoiceTemplate> findActiveByCustomer(@Param("customerId") String customerId);
}
