package com.sobol.customer.application;

/** Typed UI-safe outcomes without SQL diagnostics, submitted data or driver causes. */
public final class CustomerException extends RuntimeException {
    public enum Reason {
        INVALID_NAME, INVALID_PHONE, INVALID_EMAIL, INVALID_CITY, INVALID_ADDRESS,
        INVALID_NOTES, INVALID_ID, DUPLICATE_NAME, DUPLICATE_EMAIL, NOT_FOUND, STORAGE_FAILURE
    }
    private final Reason reason;
    public CustomerException(Reason reason) {
        super(switch (reason) {
            case INVALID_NAME -> "A customer name of 1 to 150 characters is required.";
            case INVALID_PHONE -> "A phone number of 1 to 30 characters is required.";
            case INVALID_EMAIL -> "Email must match the supported format and contain at most 180 characters.";
            case INVALID_CITY -> "City cannot exceed 100 characters.";
            case INVALID_ADDRESS -> "Address cannot exceed 220 characters.";
            case INVALID_NOTES -> "Notes cannot exceed 400 characters.";
            case INVALID_ID -> "A valid selected customer is required.";
            case DUPLICATE_NAME -> "Another customer already uses this name.";
            case DUPLICATE_EMAIL -> "Another customer already uses this email.";
            case NOT_FOUND -> "The customer no longer exists. Reload customers before editing.";
            case STORAGE_FAILURE -> "Customer storage is unavailable. The operation could not be confirmed.";
        });
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
