package com.sobol.supplier.application;

/** Typed UI-safe outcomes without SQL diagnostics, submitted data or driver causes. */
public final class SupplierException extends RuntimeException {
    public enum Reason {
        INVALID_NAME, INVALID_PHONE, INVALID_EMAIL, INVALID_CITY, INVALID_ADDRESS,
        INVALID_CONTACT_PERSON, INVALID_NOTES, INVALID_ID, DUPLICATE_NAME, NOT_FOUND, STORAGE_FAILURE
    }
    private final Reason reason;
    public SupplierException(Reason reason) {
        super(switch (reason) {
            case INVALID_NAME -> "A supplier name of 1 to 150 characters is required.";
            case INVALID_PHONE -> "A phone number of 1 to 30 characters is required.";
            case INVALID_EMAIL -> "Email must match the supported format and contain at most 150 characters.";
            case INVALID_CITY -> "City cannot exceed 100 characters.";
            case INVALID_ADDRESS -> "Address cannot exceed 255 characters.";
            case INVALID_CONTACT_PERSON -> "Contact person cannot exceed 150 characters.";
            case INVALID_NOTES -> "Notes cannot exceed 400 characters.";
            case INVALID_ID -> "A valid selected supplier is required.";
            case DUPLICATE_NAME -> "Another supplier already uses this name.";
            case NOT_FOUND -> "The supplier no longer exists. Reload suppliers before editing.";
            case STORAGE_FAILURE -> "Supplier storage is unavailable. The operation could not be confirmed.";
        });
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
