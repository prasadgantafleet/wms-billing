package com.wms.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.wms.billing.domain.Charge;
import com.wms.billing.domain.Contract;
import com.wms.billing.domain.WarehouseCharges;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * Loads contract definitions from classpath YAML files and provides lookup by contractId.
 */
@Slf4j
@Service
public class ContractYamlLoader implements InitializingBean {

    @Value("classpath*:contracts/*.yaml")
    private Resource[] contractResources;

    private final YAMLMapper yamlMapper = new YAMLMapper();

    // Typed index by contractId for fast lookup
    private final Map<Long, Contract> contractsById = new LinkedHashMap<>();

    // Backward-compatible view (id as String -> Contract as Object)
    private Map<String, Object> contracts = Collections.emptyMap();

    /**
     * Find a single contract by its ID.
     */
    public Contract getContract(Long contractId) {
        if (contractId == null) return null;
        return contractsById.get(contractId);
    }

    /**
     * Backward-compatible method returning all contracts as a map.
     * Keys are the contractId as String, values are Contract objects.
     */
    public Map<String, Object> getAllContracts() {
        return contracts;
    }

    /**
     * Optional: strongly-typed accessor for all contracts by ID.
     */
    public Map<Long, Contract> getAllContractsById() {
        return Collections.unmodifiableMap(contractsById);
    }

    /**
     * Find charges for a specific warehouse in a given contract.
     */
    public List<Charge> getChargesForWarehouse(Contract contract, String warehouseId) {
        if (contract == null || contract.getWarehouses() == null || warehouseId == null) {
            return List.of();
        }
        return contract.getWarehouses().stream()
                .filter(w -> warehouseId.equals(w.getWarehouseId()))
                .findFirst()
                .map(WarehouseCharges::getCharges)
                .orElse(List.of());
    }

    @Override
    public void afterPropertiesSet() {
        if (contractResources == null || contractResources.length == 0) {
            throw new IllegalStateException("No contract YAML resources found at 'classpath*:contracts/*.yaml'. " +
                    "Ensure the files are placed under src/main/resources/contracts and included in the jar.");
        }

        try {
            int added = 0;
            for (Resource res : contractResources) {
                if (!res.exists()) {
                    throw new IllegalStateException("Contract resource not found: " + res.getDescription());
                }
                try (InputStream in = res.getInputStream()) {
                    JsonNode root = yamlMapper.readTree(in);
                    if (root == null || root.isNull()) {
                        log.warn("Empty or null YAML in resource {}", res.getFilename());
                        continue;
                    }

                    if (root.isArray()) {
                        // Top-level array: [ {contract...}, ... ]
                        for (JsonNode node : root) {
                            added += addContractFromNode(node, null, res);
                        }
                    } else if (root.isObject()) {
                        // Either { contracts: [ ... ] } or { "123": {contract...}, ... }
                        if (root.has("contracts") && root.get("contracts").isArray()) {
                            for (JsonNode node : root.get("contracts")) {
                                added += addContractFromNode(node, null, res);
                            }
                        } else {
                            var fields = root.fields();
                            while (fields.hasNext()) {
                                Map.Entry<String, JsonNode> e = fields.next();
                                added += addContractFromNode(e.getValue(), e.getKey(), res);
                            }
                        }
                    } else {
                        log.warn("Unsupported YAML structure in resource {}. Root is neither array nor object.", res.getFilename());
                    }
                }
            }

            if (contractsById.isEmpty()) {
                throw new IllegalStateException("Loaded contract resources but parsed content is empty. " +
                        "Verify the YAML structure matches the expected Contract schema.");
            }

            // Build backward-compatible map (String id -> Contract as Object)
            Map<String, Object> compat = new LinkedHashMap<>();
            contractsById.forEach((id, c) -> compat.put(String.valueOf(id), c));
            this.contracts = Collections.unmodifiableMap(compat);

            log.info("Loaded {} contract(s) from {} resource(s).", contractsById.size(), contractResources.length);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load contract YAML. " +
                    "Check classpath location, YAML syntax, and expected schema. Root cause: " + ex.getMessage(), ex);
        }
    }

    /**
     * Convert a YAML node to a Contract and add it to the index.
     * If idKey is provided, it can be used as a fallback for contractId if missing in the YAML.
     */
    private int addContractFromNode(JsonNode node, String idKey, Resource source) {
        if (node == null || node.isNull()) return 0;

        Contract contract = yamlMapper.convertValue(node, Contract.class);

        // If contractId is missing in the YAML, try to derive it from the key name
        if (contract.getContractId() == null && idKey != null) {
            try {
                contract.setContractId(Long.parseLong(idKey));
            } catch (NumberFormatException nfe) {
                throw new IllegalStateException(
                        "Contract in " + source.getFilename() + " is missing 'contractId' and key '" + idKey +
                        "' is not a valid numeric id.", nfe);
            }
        }

        if (contract.getContractId() == null) {
            throw new IllegalStateException("Contract in " + source.getFilename() + " is missing required field 'contractId'.");
        }

        // Validate and normalize to avoid nulls and bad data
        validateAndNormalize(contract, source);

        Contract previous = contractsById.put(contract.getContractId(), contract);
        if (previous != null) {
            log.warn("Overriding contract with id {} from resource {} (previously defined elsewhere).",
                    contract.getContractId(), source.getFilename());
        }
        return 1;
    }

    private void validateAndNormalize(Contract contract, Resource source) {
        if (contract.getCustomerId() == null || contract.getCustomerId().isBlank()) {
            throw new IllegalStateException("Contract " + contract.getContractId() + " in " + source.getFilename()
                    + " is missing 'customerId'.");
        }

        if (contract.getWarehouses() == null) {
            contract.setWarehouses(new ArrayList<>());
        }

        for (WarehouseCharges wc : contract.getWarehouses()) {
            if (wc.getWarehouseId() == null || wc.getWarehouseId().isBlank()) {
                throw new IllegalStateException("Contract " + contract.getContractId() + " in " + source.getFilename()
                        + " has a warehouse without 'warehouseId'.");
            }
            if (wc.getCharges() == null) {
                wc.setCharges(new ArrayList<>());
            }
        }
    }
}