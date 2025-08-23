package com.wms.billing.service;

import com.wms.billing.domain.Invoice;
import com.wms.billing.domain.InvoiceTemplate;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring service that renders {@link Invoice} instances to PDF using JasperReports.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Resolve the customer's {@link InvoiceTemplate} via {@link InvoiceTemplateService}.</li>
 *   <li>Load a compiled Jasper template (.jasper) or compile a source template (.jrxml) on the fly.</li>
 *   <li>Bind invoice header parameters and line items to the report.</li>
 *   <li>Export the filled report to a PDF byte array.</li>
 * </ul>
 *
 * <p>Template contract:</p>
 * <ul>
 *   <li>Header parameters populated:
 *     <ul>
 *       <li><b>CUSTOMER_NAME</b> – currently set to the invoice's customerId. If a customer name is available in your domain, map it accordingly.</li>
 *       <li><b>PERIOD_START</b> – {@code invoice.getPeriodStart().toString()}.</li>
 *       <li><b>PERIOD_END</b> – {@code invoice.getPeriodEnd().toString()}.</li>
 *       <li><b>TOTAL_AMOUNT</b> – {@code invoice.getTotalAmount()}.</li>
 *     </ul>
 *   </li>
 *   <li>Detail dataset:
 *     <ul>
 *       <li>A {@link JRBeanCollectionDataSource} is created from {@code invoice.getLines()} (empty list if null).</li>
 *       <li>Field names in the template must match the bean properties of the line items.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Error handling:</p>
 * <ul>
 *   <li>Throws {@link ResponseStatusException} (400 Bad Request) when:
 *     <ul>
 *       <li>The template file path is missing or blank.</li>
 *       <li>The template file does not exist.</li>
 *       <li>The template file type is not supported (only .jrxml or .jasper).</li>
 *     </ul>
 *   </li>
 *   <li>May propagate JasperReports exceptions if compilation/filling/export fails.</li>
 * </ul>
 *
 * <p>Thread-safety:</p>
 * <ul>
 *   <li>This service is stateless and safe for concurrent use by multiple threads.</li>
 * </ul>
 *
 * <p>Performance note:</p>
 * <ul>
 *   <li>Compiling .jrxml templates at runtime is convenient but expensive. Consider pre-compiling templates to .jasper or adding a caching layer if this method is invoked frequently.</li>
 * </ul>
 *
 * @author Prasad Ganta
 * @since 1.0
 */
@Service
public class JasperInvoiceGenerator {

    private final InvoiceTemplateService templateService;

    /**
     * Create a new generator with the required template lookup service.
     *
     * @param templateService service used to resolve a customer's active invoice template
     */
    public JasperInvoiceGenerator(InvoiceTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Generate a PDF representation of the given invoice using a per-customer Jasper template.
     *
     * <p>Behavior:</p>
     * <ol>
     *   <li>Loads the customer's template path from {@link InvoiceTemplateService}.</li>
     *   <li>Loads a .jasper file or compiles a .jrxml file as a {@link JasperReport}.</li>
     *   <li>Passes header parameters and a {@link JRBeanCollectionDataSource} of invoice lines to {@link JasperFillManager}.</li>
     *   <li>Exports the resulting {@link JasperPrint} to PDF bytes.</li>
     * </ol>
     *
     * @param invoice the fully computed invoice to render
     * @return a byte array containing the generated PDF
     * @throws Exception if the template cannot be loaded/compiled, the report cannot be filled, or the export fails
     * @throws ResponseStatusException with status 400 if the template path is missing, the file is not found, or the file type is unsupported
     */
    public byte[] generateInvoicePdf(Invoice invoice) throws Exception {
        InvoiceTemplate template = templateService.getCustomerTemplate(invoice.getCustomerId());
        String path = template.getFilePath();
        if (path == null || path.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template file path is not configured.");
        }

        File f = new File(path);
        if (!f.exists()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template file not found: " + path);
        }

        JasperReport jasperReport;
        String lower = path.toLowerCase();
        if (lower.endsWith(".jrxml")) {
            jasperReport = JasperCompileManager.compileReport(f.getAbsolutePath());
        } else if (lower.endsWith(".jasper")) {
            jasperReport = (JasperReport) JRLoader.loadObject(f);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported template file type: " + path);
        }

        Map<String, Object> params = new HashMap<>();
        // Note: CUSTOMER_NAME currently uses customerId as provided by Invoice.
        // If a customer name is available in your domain, map it here instead.
        params.put("CUSTOMER_NAME", invoice.getCustomerId());
        params.put("PERIOD_START", invoice.getPeriodStart().toString());
        params.put("PERIOD_END", invoice.getPeriodEnd().toString());
        params.put("TOTAL_AMOUNT", invoice.getTotalAmount());

        List<?> lines = (invoice.getLines() == null) ? List.of() : invoice.getLines();
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(lines);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, ds);

        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}