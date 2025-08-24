package com.wms.billing.exception;

public class YamlSerializationException extends RuntimeException {
    public YamlSerializationException(String message) { super(message); }
    public YamlSerializationException(String message, Throwable cause) { super(message, cause); }
}