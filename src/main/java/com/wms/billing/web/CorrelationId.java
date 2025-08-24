package com.wms.billing.web;

import org.slf4j.MDC;

public final class CorrelationId {
    private CorrelationId() {}
    public static final String MDC_KEY = "correlationId";
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}