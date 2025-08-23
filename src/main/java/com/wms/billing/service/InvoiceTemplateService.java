package com.wms.billing.service;

import com.wms.billing.domain.CustomerInvoiceTemplate;
import com.wms.billing.domain.InvoiceTemplate;
import com.wms.billing.repository.CustomerInvoiceTemplateRepository;
import com.wms.billing.repository.InvoiceTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class InvoiceTemplateService {

    private final InvoiceTemplateRepository templateRepo;
    private final CustomerInvoiceTemplateRepository mappingRepo;

    public InvoiceTemplateService(InvoiceTemplateRepository templateRepo,
                                  CustomerInvoiceTemplateRepository mappingRepo) {
        this.templateRepo = templateRepo;
        this.mappingRepo = mappingRepo;
    }

    public InvoiceTemplate createTemplate(InvoiceTemplate template) {
        return templateRepo.save(template);
    }

    public List<InvoiceTemplate> getActiveTemplates() {
        return templateRepo.findByIsActiveTrue();
    }

    public CustomerInvoiceTemplate assignTemplateToCustomer(String customerId, Long templateId) {
        InvoiceTemplate tpl = templateRepo.findById(templateId).orElseThrow();
        CustomerInvoiceTemplate cit = new CustomerInvoiceTemplate();
        cit.setCustomerId(customerId);
        cit.setTemplate(tpl);
        cit.setEffectiveFrom(LocalDate.now());
        return mappingRepo.save(cit);
    }

    /**
     * Resolve a customer's template with robust fallbacks:
     * 1) Use the most recent active mapping for the customer, if present.
     * 2) If no mapping exists, fall back to a single active template (system default).
     * 3) Otherwise, return a clear 400 error instructing to configure templates.
     */
    public InvoiceTemplate getCustomerTemplate(String customerId) {
        List<CustomerInvoiceTemplate> maps = mappingRepo.findActiveByCustomer(customerId);

        if (!maps.isEmpty()) {
            // Prefer the most recent effective assignment if multiple exist
            return maps.stream()
                    .sorted(Comparator.comparing(CustomerInvoiceTemplate::getEffectiveFrom,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(CustomerInvoiceTemplate::getTemplate)
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Template mapping is invalid for customer: " + customerId
                    ));
        }

        // Reasonable fallback to a single active template if configured as a system default
        List<InvoiceTemplate> activeTemplates = templateRepo.findByIsActiveTrue();
        if (activeTemplates.size() == 1) {
            return activeTemplates.get(0);
        }

        // Clear client error (400) with actionable message
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No invoice template configured for customer: " + customerId +
                        ". Assign a template to the customer or configure a single active default template."
        );
    }
}