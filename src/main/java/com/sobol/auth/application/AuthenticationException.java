package com.sobol.auth.application;

/** Safe outcomes only: never attach driver diagnostics, submitted credentials or causes. */
public final class AuthenticationException extends RuntimeException {
    public enum Reason { INVALID_CREDENTIALS, INVALID_REGISTRATION, REGISTRATION_CLOSED, DUPLICATE_LOGIN, THROTTLED, AUTHENTICATION_REQUIRED, STORAGE_FAILURE }
    private final Reason reason;
    public AuthenticationException(Reason reason) {
        super(switch (reason) {
            case INVALID_CREDENTIALS -> "Unable to sign in with these credentials.";
            case INVALID_REGISTRATION -> "Use a valid email, a name up to 160 characters, an optional phone up to 60 characters, and a password of 15 to 128 characters.";
            case REGISTRATION_CLOSED -> "Registration is closed. Contact the application administrator.";
            case DUPLICATE_LOGIN -> "The account request could not be created.";
            case THROTTLED -> "Too many attempts. Please try again later.";
            case AUTHENTICATION_REQUIRED -> "Please sign in again to continue.";
            case STORAGE_FAILURE -> "Account storage is unavailable. The operation could not be confirmed.";
        });
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
