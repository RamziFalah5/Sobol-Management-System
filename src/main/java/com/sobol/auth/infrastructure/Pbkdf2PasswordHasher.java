package com.sobol.auth.infrastructure;

import com.sobol.auth.application.PasswordHasher;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Standard JCE KDF: v1, PBKDF2-HMAC-SHA256, 600000 iterations, 128-bit random salt, 256-bit key. */
public final class Pbkdf2PasswordHasher implements PasswordHasher {
    private static final int ITERATIONS = 600_000;
    private final SecureRandom random = new SecureRandom();
    private final String dummy;
    public Pbkdf2PasswordHasher() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        char[] value = Base64.getEncoder().encodeToString(bytes).toCharArray();
        try { dummy = hash(value); } finally { Arrays.fill(value, '\0'); Arrays.fill(bytes, (byte) 0); }
    }
    @Override public String hash(char[] password) {
        if (password == null || password.length == 0 || password.length > 256) throw new IllegalArgumentException("Invalid password length.");
        byte[] salt = new byte[16]; random.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        try { return "pbkdf2-sha256$v1$600000$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derived); }
        finally { Arrays.fill(derived, (byte) 0); Arrays.fill(salt, (byte) 0); }
    }
    @Override public boolean verify(char[] password, String encoded) {
        if (password == null || password.length == 0 || password.length > 256 || encoded == null || encoded.length() > 256) return false;
        byte[] salt = null, expected = null, actual = null;
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 5 || !parts[0].equals("pbkdf2-sha256") || !parts[1].equals("v1") || !parts[2].equals("600000")) return false;
            salt = Base64.getDecoder().decode(parts[3]); expected = Base64.getDecoder().decode(parts[4]);
            if (salt.length != 16 || expected.length != 32) return false;
            actual = derive(password, salt, ITERATIONS);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException malformed) { return false; }
        finally {
            if (salt != null) Arrays.fill(salt, (byte) 0);
            if (expected != null) Arrays.fill(expected, (byte) 0);
            if (actual != null) Arrays.fill(actual, (byte) 0);
        }
    }
    @Override public void verifyUnknown(char[] password) { verify(password, dummy); }
    private byte[] derive(char[] password, byte[] salt, int iterations) {
        var spec = new PBEKeySpec(password, salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException failure) { throw new IllegalStateException("Password hashing is unavailable."); }
        finally { spec.clearPassword(); }
    }
}
