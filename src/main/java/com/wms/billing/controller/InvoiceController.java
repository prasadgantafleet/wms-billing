package com.wms.billing.controller;

import com.wms.billing.domain.RateSheet;
import com.wms.billing.dto.ChargeCategory;
import com.wms.billing.dto.CreateInvoiceRequest;
import com.wms.billing.domain.Activity;
import com.wms.billing.domain.Invoice;
import com.wms.billing.service.InvoiceServiceDrools;
import com.wms.billing.service.JasperInvoiceGenerator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller exposing operations to preview, finalize, and generate PDF invoices.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Validate the existence of a contract before processing.</li>
 *   <li>Map incoming request DTOs into domain {@code Activity} instances.</li>
 *   <li>Delegate invoice computation to a Drools-backed service.</li>
 *   <li>Return computed invoices as JSON or produce a PDF stream.</li>
 * </ul>
 * </p>
 *
 * Endpoints:
 * <ul>
 *   <li>POST {@code /api/invoices/preview/{contractId}}</li>
 *   <li>POST {@code /api/invoices/finalize/{contractId}}</li>
 *   <li>POST {@code /api/invoices/generate-pdf/{contractId}}</li>
 * </ul>
 *
 * Error handling:
 * <ul>
 *   <li>Returns HTTP 400 (Bad Request) if the contract is not found.</li>
 * </ul>
 *
 * Thread-safety: This controller is stateless; injected services are managed by Spring.
 *
 * author Prasad Ganta
 * since 1.0
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceServiceDrools invoiceService;

    @Autowired
    private JasperInvoiceGenerator jasperGenerator;

    /**
     * Preview an invoice without persisting it. Useful for validating charges before finalization.
     *
     * @param ratesheetId the target contract identifier
     * @param req request containing warehouse, period, and activities to bill
     * @return 200 OK with the calculated {@link Invoice}; 400 if contract is not found
     */
    @PostMapping("/preview/{ratesheetId}")
    public ResponseEntity<Invoice> previewInvoice(@PathVariable @Valid  Long ratesheetId,
                                                  @RequestBody @Valid CreateInvoiceRequest req) {
        List<Activity> acts = mapActivities(req);

        Invoice invoice = invoiceService.generateInvoice(
                ratesheetId,
                req.getWarehouseId(),
                req.getPeriodStart(),
                req.getPeriodEnd(),
                acts,
                true
        );
        return ResponseEntity.ok(invoice);
    }

    /**
     * Finalize and persist an invoice using the same computation as preview.
     *
     * @param ratesheetId the target contract identifier
     * @param req request containing warehouse, period, and activities to bill
     * @return 200 OK with the persisted {@link Invoice}; 400 if contract is not found
     */
    @PostMapping("/finalize/{ratesheetId}")
    public ResponseEntity<Invoice> finalizeInvoice(@PathVariable @Valid Long ratesheetId,
                                                   @RequestBody @Valid CreateInvoiceRequest req) {
        List<Activity> acts = mapActivities(req);

        Invoice invoice = invoiceService.generateInvoice(
                ratesheetId,
                req.getWarehouseId(),
                req.getPeriodStart(),
                req.getPeriodEnd(),
                acts,
                false
        );
        return ResponseEntity.ok(invoice);
    }

    /**
     * Generate an invoice PDF for preview. The invoice is not persisted.
     *
     * @param ratesheetId the target contract identifier
     * @param req request containing warehouse, period, and activities to bill
     * @return 200 OK with a PDF byte stream of the invoice; 400 if contract is not found
     * @throws Exception if PDF generation fails
     */
    @PostMapping("/generate-pdf/{ratesheetId}")
    public ResponseEntity<byte[]> generatePdf(@PathVariable @Valid Long ratesheetId,
                                              @RequestBody @Valid CreateInvoiceRequest req) throws Exception {

        List<Activity> acts = mapActivities(req);

        Invoice invoice = invoiceService.generateInvoice(
                ratesheetId,
                req.getWarehouseId(),
                req.getPeriodStart(),
                req.getPeriodEnd(),
                acts,
                true
        );

        byte[] pdf = jasperGenerator.generateInvoicePdf(invoice);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice-" + invoice.getId() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
//
//    /**
//     * Ensure the contract exists; otherwise respond with HTTP 400 (Bad Request).
//     *
//     * @param rateSheetId the contract id to look up
//     * @return the resolved {@link RateSheet}
//     * @throws ResponseStatusException if the contract does not exist
//     */
//    private RateSheet requireContract(Long rateSheetId) {
//        var contract = contractLoader.getContract(contractId);
//        if (contract == null) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract not found: " + contractId);
//        }
//        return contract;
//    }

    /**
     * Map incoming request activities to domain {@link Activity} objects, enriching with
     * request-scope fields such as warehouse id.
     *
     * @param req invoice creation request containing activity DTOs
     * @return list of mapped {@link Activity} instances
     */
    private List<Activity> mapActivities(CreateInvoiceRequest req) {
        return req.getActivities().stream().map(dto -> {
            Activity a = new Activity();
            a.setType(dto.getType());
            a.setQuantity(dto.getQuantity());
            a.setCategory(ChargeCategory.valueOf(dto.getCategory()));
            a.setWarehouseId(req.getWarehouseId());
            return a;
        }).collect(Collectors.toList());
    }
}