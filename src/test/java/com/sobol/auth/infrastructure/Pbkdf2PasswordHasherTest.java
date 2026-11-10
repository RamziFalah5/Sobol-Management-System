package com.sobol.auth.infrastructure;

import org.junit.jupiter.api.Test;
import static com.sobol.auth.application.AuthTestSupport.input;
import static org.junit.jupiter.api.Assertions.*;

class Pbkdf2PasswordHasherTest {
    private static final Pbkdf2PasswordHasher HASHER = new Pbkdf2PasswordHasher();
    @Test void eachPasswordHasAFreshSaltAndVerifiesWithoutPlaintextPersistence() {
        String first = HASHER.hash(input()), second = HASHER.hash(input());
        assertNotEquals(first, second); assertTrue(first.startsWith("pbkdf2-sha256$v1$600000$"));
        assertTrue(HASHER.verify(input(), first)); assertTrue(HASHER.verify(input(), second));
        assertFalse(first.contains(new String(input()))); assertFalse(HASHER.verify("wrong synthetic value".toCharArray(), first));
    }
    @Test void passwordWhitespaceIsSignificantAndUnicodeIsSupported() {
        char[] value = "  synthetic Unicode \u03bb passphrase  ".toCharArray();
        String encoded = HASHER.hash(value); assertTrue(HASHER.verify(value, encoded));
        assertFalse(HASHER.verify(new String(value).trim().toCharArray(), encoded));
    }
    @Test void legacyMalformedUnknownVersionAndUnboundedWorkFactorsAreRejected() {
        String valid = HASHER.hash(input());
        for (String invalid : new String[]{"a".repeat(64), "invalid", valid.replace("$v1$", "$v2$"), valid.replace("600000", "1"),
                valid.replace("600000", "999999999"), valid + "$extra", "x".repeat(257)})
            assertFalse(HASHER.verify(input(), invalid));
        assertFalse(HASHER.verify(null, valid)); assertFalse(HASHER.verify(new char[129], valid));
        assertFalse(HASHER.verify(input(), null));
    }
}
