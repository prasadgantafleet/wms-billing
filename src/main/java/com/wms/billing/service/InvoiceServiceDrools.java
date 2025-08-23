package com.wms.billing.service;

import com.wms.billing.domain.*;
import com.wms.billing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that generates invoices by evaluating domain facts (contract, activities, charges)
 * through a Drools KIE session. Populates invoice lines based on rule outcomes and calculates totals.
 *
 * <p>Workflow:</p>
 * <ol>
 *   <li>Load the contract by id.</li>
 *   <li>Resolve charges for the requested warehouse.</li>
 *   <li>Insert facts (contract, activities, charges) into the KIE session.</li>
 *   <li>Fire all rules to produce invoice lines and amounts.</li>
 *   <li>Aggregate totals and optionally persist the invoice.</li>
 * </ol>
 *
 * <p>Rules use a global collection <code>invoiceLines</code> to collect results.</p>
 *
 * @author Prasad Ganta
 */
@Service
@RequiredArgsConstructor
public class InvoiceServiceDrools {

    @Autowired
    private ContractYamlLoader contractLoader;

    @Autowired
    private InvoiceRepository invoiceRepo;

    @Autowired
    private KieContainer kieContainer;

    /**
     * Generates an invoice from provided activities and contract configuration within a period.
     *
     *
     * @param contractId contract identifier
     * @param warehouseId warehouse where activities occurred
     * @param start billing period start date (inclusive)
     * @param end billing period end date (inclusive)
     * @param activities list of activities to bill
     * @param preview whether to only preview (true) or persist the final invoice (false)
     * @return built {@link Invoice} including computed lines and total
     * @throws RuntimeException if the contract cannot be found
     */
    public Invoice generateInvoice(Long contractId, String warehouseId,
                                   LocalDate start, LocalDate end, List<Activity> activities,
                                   boolean preview) {

        Contract contract = contractLoader.getContract(contractId);
        if (contract == null) throw new RuntimeException("Contract not found: " + contractId);

        List<Charge> charges = resolveCharges(contract, warehouseId);

        Invoice invoice = new Invoice();
        invoice.setCustomerId(contract.getCustomerId());
        invoice.setContractId(contractId);
        invoice.setWarehouseId(warehouseId);
        invoice.setPeriodStart(start);
        invoice.setPeriodEnd(end);
        invoice.setStatus(preview ? "PREVIEW" : "FINAL");

        KieSession kieSession = kieContainer.newKieSession("billingKS");
        List<InvoiceLine> invoiceLines = new ArrayList<>();
        kieSession.setGlobal("invoiceLines", invoiceLines);

        // insert domain objects into KIE session
        kieSession.insert(contract);
        activities.forEach(kieSession::insert);
        charges.forEach(kieSession::insert);

        kieSession.fireAllRules();
        kieSession.dispose();

        invoice.setLines(invoiceLines);
        BigDecimal total = invoiceLines.stream()
                .map(InvoiceLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        invoice.setTotalAmount(total);

        if (!preview) invoiceRepo.save(invoice);
        return invoice;
    }

    /**
     * Resolves the list of charges applicable to a given warehouse under the provided contract.
     *
     * @param contract the contract that may contain warehouse-level charge definitions
     * @param warehouseId the target warehouse identifier
     * @return the list of charges for the warehouse, or an empty list if not found
     */
    private List<Charge> resolveCharges(
            com.wms.billing.domain.Contract contract,
            String warehouseId
    ) {
        if (contract == null || contract.getWarehouses() == null || warehouseId == null) {
            return java.util.List.of();
        }
        return contract.getWarehouses().stream()
                .filter(wc -> warehouseId.equals(wc.getWarehouseId()))
                .findFirst()
                .map(com.wms.billing.domain.WarehouseCharges::getCharges)
                .orElse(java.util.List.of());
    }
}