package com.wms.billing.dto;

/**
 * Enumerates common warehouse accessorial services that can be billed in addition to
 * core storage, inbound, and outbound activities.
 *
 * Purpose
 * - Standardization: Provides a controlled vocabulary for add-on services so pricing,
 *   validation, and reporting are consistent across the system.
 * - Billing rules: Acts as a discriminator to select the correct rate in rules/logic
 *   when ChargeCategory is ACCESSORIAL.
 * - Data integrity: Prevents free‑text mismatches in requests, contracts, and invoices.
 *
 * Grouping overview
 * - Handling / Palletization: Pallet work (build/rebuild), wrapping, and strapping.
 * - Labeling & Documentation: New/relabel operations, barcode application, and compliance docs.
 * - Storage Exceptions: Special storage needs (oversized, HAZMAT, temperature-controlled).
 * - Order Processing Add-ons: Value‑added services like kitting and repacking.
 * - Inventory Adjustments: Fees for cycle counts, shrinkage, and special audits.
 * - Loading / Unloading: Extra services during dock operations (e.g., liftgate, inside delivery).
 * - Accessorial Surcharges: Time‑based or urgency‑based surcharges (weekend, holiday, rush).
 *
 * Usage recommendations
 * - Pair with ChargeCategory.ACCESSORIAL when defining rates.
 * - Use these values in contracts/rate sheets and in requests to precisely reference the service.
 * - When adding new accessorials, extend this enum and align your pricing/rule configuration accordingly.
 * author Prasad Ganta
 *
 */
public enum AccessorialType {
    // Handling / Palletization

    /**
     * Pallet build/rebuild or general palletization work.
     */
    PALLETIZATION,

    /**
     * Stretch wrapping or shrink wrapping pallets/cases.
     */
    SHRINK_WRAPPING,

    /**
     * Banding or strapping items/pallets for securement.
     */
    STRAPPING,

    // Labeling & Documentation

    /**
     * Applying labels or documentation to items/cases/pallets.
     */
    LABELING,

    /**
     * Replacing or correcting existing labels.
     */
    RELABELING,

    /**
     * Printing and applying barcodes (e.g., SKU, license plate).
     */
    BARCODE_PRINTING,

    /**
     * Special documentation or compliance labeling requirements.
     */
    SPECIAL_DOCUMENTATION,

    // Storage Exceptions

    /**
     * Storage for oversized items that require extra space/handling.
     */
    OVERSIZED_STORAGE,

    /**
     * Hazardous materials (HAZMAT) storage and handling.
     */
    HAZMAT_STORAGE,

    /**
     * Temperature-controlled storage (refrigerated, frozen, or specific ranges).
     */
    TEMP_CONTROLLED_STORAGE,

    // Order Processing Add-ons

    /**
     * Kitting or light assembly operations.
     */
    KITTING,

    /**
     * Repacking or reboxing items/orders.
     */
    REPACKING,

    /**
     * Return processing (reverse logistics).
     */
    RETURNS_PROCESSING,

    // Inventory Adjustments

    /**
     * Cycle counting fee for inventory verification.
     */
    CYCLE_COUNT,

    /**
     * Shrinkage fee related to loss or damage adjustments.
     */
    SHRINKAGE,

    /**
     * Special or ad-hoc inventory audits.
     */
    INVENTORY_AUDIT,

    // Loading / Unloading

    /**
     * Liftgate service during pickup or delivery.
     */
    LIFTGATE_SERVICE,

    /**
     * Inside delivery beyond dock/threshold.
     */
    INSIDE_DELIVERY,

    /**
     * Cross-docking handling fee.
     */
    CROSS_DOCKING,

    // Accessorial Surcharges

    /**
     * Weekend handling surcharge.
     */
    WEEKEND_HANDLING,

    /**
     * Holiday handling surcharge.
     */
    HOLIDAY_HANDLING,

    /**
     * After-hours receiving or shipping surcharge.
     */
    AFTER_HOURS_HANDLING,

    /**
     * Rush/expedited order processing surcharge.
     */
    RUSH_ORDER ,

    SPECIAL_HANDLING
}