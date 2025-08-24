package com.wms.billing.exception;

public class RateSheetNotFoundException extends RuntimeException {
    public RateSheetNotFoundException(String message) {
        super(message);
    }
}