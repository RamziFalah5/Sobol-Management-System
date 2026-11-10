package com.sobol.auth.application;

public interface PasswordHasher {
    String hash(char[] password);
    boolean verify(char[] password, String encoded);
    /** Spend comparable KDF work for an unknown identifier, without accepting a dummy identity. */
    void verifyUnknown(char[] password);
}
