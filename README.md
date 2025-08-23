# wms-billing

A Spring Boot service for calculating warehouse billing invoices and generating PDF invoices using templated reports.

- Compute invoices from a contract and a list of activities.
- Preview, finalize (persist), and export invoices as PDF.
- Manage and assign invoice templates to customers.

## Requirements

- Java 17 and Above version 
- Maven (wrapper scripts recommended)
- JasperReports templates (.jrxml or .jasper) reachable by the running service
- Contract YAML files on the classpath

## Getting started

1) Clone and configure
- Ensure contract YAML files are available under `src/main/resources/contracts` so they are loaded from the classpath at `contracts/*.yaml`.
- Prepare at least one JasperReports template file (.jrxml to compile at runtime or precompiled .jasper).
- Make sure the application can access the template files via an absolute or valid path when creating templates.

2) Run locally
- Maven:
```bash
./mvnw spring-boot:run
```

3) Default base URL
```bash
http://localhost:8080
```

You can change the port via standard Spring Boot configuration (e.g., `server.port`).

## Configuration

### Contracts (classpath YAML)

Place contract definition files in:
```text
src/main/resources/contracts/
```

These are loaded from the classpath pattern:
```text
classpath*:contracts/*.yaml
```

Each contract must define an ID and customer reference, plus any warehouse-specific charge configuration. If a contract is not found for a requested ID, invoice endpoints will return HTTP 400.

Example skeleton:
```yaml
contracts:
  - contractId: 1001
    customerId: CUST-001
    warehouses:
      - warehouseId: WH001
        charges:
          - category: STORAGE
            type: STORAGE_PALLET
            rate: 2.50
            unit: PALLET
```

### Invoice templates (JasperReports)

- A template must be created and (optionally) marked active.
- Assign a template to a customer before generating PDFs.
- The template file path must point to a valid `.jrxml` or `.jasper` file accessible by the service process.

Notes:
- `.jrxml` files will be compiled on-the-fly (convenient but slower).
- `.jasper` files are precompiled and load faster.

## REST API

Base path:
```text
/
```

All endpoints are JSON unless otherwise noted.

### Template management

Base path:
```text
/api/templates
```

- Create a template
  - Method: POST
  - Path: `/api/templates`
  - Body: JSON representing your template model. Adjust field names to match your `InvoiceTemplate` class.
  - Example payload (adjust to your model):
```json
{
  "name": "Default Invoice",
  "filePath": "/absolute/path/to/invoice-template.jasper",
  "active": true,
  "description": "Primary invoice layout"
}
```
  - cURL:
```bash
curl -X POST "http://localhost:8080/api/templates" \
  -H "Content-Type: application/json" \
  -d '{
        "name":"Default Invoice",
        "filePath":"/absolute/path/to/invoice-template.jasper",
        "active":true,
        "description":"Primary invoice layout"
      }'
```

- List active templates
  - Method: GET
  - Path: `/api/templates/active`
  - cURL:
```bash
curl -X GET "http://localhost:8080/api/templates/active"
```

- Assign a template to a customer
  - Method: POST
  - Path: `/api/templates/assign/{customerId}/{templateId}`
  - Path params:
    - `customerId`: your customer identifier (e.g., `CUST-001`)
    - `templateId`: numeric ID of the template to assign
  - cURL:
```bash
curl -X POST "http://localhost:8080/api/templates/assign/CUST-001/1"
```

### Invoice operations

Base path:
```text
/api/invoices
```

Request body type: CreateInvoiceRequest (adjust property names to your DTO). A typical shape:

```json
{
  "warehouseId": "WH001",
  "periodStart": "2024-07-01",
  "periodEnd": "2024-07-31",
  "activities": [
    { "type": "STORAGE_PALLET", "quantity": 120, "category": "STORAGE" },
    { "type": "INBOUND_CASE", "quantity": 500, "category": "INBOUND" }
  ]
}
```

- Preview invoice (no persistence)
  - Method: POST
  - Path: `/api/invoices/preview/{contractId}`
  - Path params:
    - `contractId`: numeric contract identifier
  - Response: computed Invoice (JSON)
  - cURL:
```bash
curl -X POST "http://localhost:8080/api/invoices/preview/1001" \
  -H "Content-Type: application/json" \
  -d '{
        "warehouseId":"WH001",
        "periodStart":"2024-07-01",
        "periodEnd":"2024-07-31",
        "activities":[
          {"type":"STORAGE_PALLET","quantity":120,"category":"STORAGE"},
          {"type":"INBOUND_CASE","quantity":500,"category":"INBOUND"}
        ]
      }'
```

- Finalize invoice (persist)
  - Method: POST
  - Path: `/api/invoices/finalize/{contractId}`
  - Path params:
    - `contractId`: numeric contract identifier
  - Response: persisted Invoice (JSON)
  - cURL:
```bash
curl -X POST "http://localhost:8080/api/invoices/finalize/1001" \
  -H "Content-Type: application/json" \
  -d '{
        "warehouseId":"WH001",
        "periodStart":"2024-07-01",
        "periodEnd":"2024-07-31",
        "activities":[
          {"type":"STORAGE_PALLET","quantity":120,"category":"STORAGE"},
          {"type":"INBOUND_CASE","quantity":500,"category":"INBOUND"}
        ]
      }'
```

- Generate invoice PDF (no persistence)
  - Method: POST
  - Path: `/api/invoices/generate-pdf/{contractId}`
  - Path params:
    - `contractId`: numeric contract identifier
  - Response: PDF stream (`application/pdf`)
  - Tip: Ensure a template is assigned for the invoice’s customer and its file path is reachable.
  - cURL (save to file):
```bash
curl -X POST "http://localhost:8080/api/invoices/generate-pdf/1001" \
  -H "Content-Type: application/json" \
  -d '{
        "warehouseId":"WH001",
        "periodStart":"2024-07-01",
        "periodEnd":"2024-07-31",
        "activities":[
          {"type":"STORAGE_PALLET","quantity":120,"category":"STORAGE"},
          {"type":"INBOUND_CASE","quantity":500,"category":"INBOUND"}
        ]
      }' \
  --output invoice-1001.pdf
```


## Drools rules engine

Billing calculations are performed by Drools (KIE).

Typical flow:
1) Build or load a KIE base containing DRL rule files (packaged on the classpath).
2) Create a session and insert facts (contract, warehouse charges, activities).
3) Optionally set globals (e.g., a list to collect computed invoice lines).
4) Fire rules; rules compute invoice line items and amounts based on activity type/category and contract pricing.
5) Aggregate totals and return the computed invoice.

Guidelines:
- Keep rule conditions specific (activity type, category, warehouse).
- Use BigDecimal for monetary calculations; be explicit about scale/rounding.
- Organize rules into packages by domain area; prefer small, focused rules.
- Performance: reuse KIE bases; batch insert facts; fire rules once per invoice.
- Testing: write unit tests feeding minimal facts and asserting expected results for each rule path.

Deployment:
- Package DRL files in the app resources for deterministic behavior.
- Changing rules typically requires a rebuild/redeploy unless you add dynamic loading.

## Responses and error handling

- 200 OK: Successful preview/finalize/PDF generation.
- 400 Bad Request:
  - Contract not found for the given `contractId`.
  - Invalid or missing template path; unsupported template file type.
  - Template file path does not exist or is not accessible.
- 415 Unsupported Media Type: If `Content-Type: application/json` is missing on JSON requests.
- 500 Internal Server Error: Unexpected server error.

## Tips and troubleshooting

- If invoice preview/finalize returns 400 for contract not found, confirm your contract YAML file is on the classpath and contains the requested `contractId`.
- If PDF generation fails:
  - Verify a template is assigned to the invoice’s customer.
  - Confirm the `filePath` points to a valid `.jrxml` or `.jasper` that the server can read.
  - Prefer `.jasper` (precompiled) for better performance under load.
- Dates should be ISO-8601 strings (`YYYY-MM-DD`) unless your DTO specifies otherwise.
- Quantities should match the numeric type expected by your DTO (e.g., integer vs decimal).

## Health check

If actuator is enabled in your setup, you can use:
```bash
curl -s "http://localhost:8080/actuator/health"
```

Otherwise, use one of the GET endpoints (e.g., list active templates) to verify the service is up.

---
```