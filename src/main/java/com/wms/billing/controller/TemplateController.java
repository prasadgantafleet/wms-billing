package com.wms.billing.controller;

import com.wms.billing.domain.InvoiceTemplate;
import com.wms.billing.service.InvoiceTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final InvoiceTemplateService templateService;

    public TemplateController(InvoiceTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<InvoiceTemplate> createTemplate(@RequestBody InvoiceTemplate template) {
        return ResponseEntity.ok(templateService.createTemplate(template));
    }

    @GetMapping("/active")
    public ResponseEntity<List<InvoiceTemplate>> listActive() {
        return ResponseEntity.ok(templateService.getActiveTemplates());
    }

    @PostMapping("/assign/{customerId}/{templateId}")
    public ResponseEntity<?> assign(@PathVariable String customerId, @PathVariable Long templateId) {
        templateService.assignTemplateToCustomer(customerId, templateId);
        return ResponseEntity.ok().build();
    }
}
