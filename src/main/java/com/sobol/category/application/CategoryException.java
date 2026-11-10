package com.sobol.category.application;

/** Stable, safe application outcomes. Never carries driver messages, credentials or SQL causes. */
public final class CategoryException extends RuntimeException {
    public enum Reason { INVALID_NAME, INVALID_ID, DUPLICATE_NAME, NOT_FOUND, STORAGE_FAILURE }
    private final Reason reason;

    public CategoryException(Reason reason) {
        super(switch (reason) {
            case INVALID_NAME -> "A category name of 1 to 150 characters is required.";
            case INVALID_ID -> "A valid selected category is required.";
            case DUPLICATE_NAME -> "Another category already uses this name.";
            case NOT_FOUND -> "The category no longer exists. Reload categories before editing.";
            case STORAGE_FAILURE -> "Category storage is unavailable. The operation could not be confirmed.";
        });
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
