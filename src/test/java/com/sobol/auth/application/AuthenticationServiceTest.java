package com.sobol.auth.application;

import com.sobol.auth.domain.Account;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static com.sobol.auth.application.AuthTestSupport.*;
import static com.sobol.auth.application.AuthenticationException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {
    final Memory rows = new Memory(); final Time time = new Time(); final Hasher hasher = new Hasher();
    final SessionManager sessions = new SessionManager(rows, time);
    AuthenticationService service(boolean registration) { return AuthTestSupport.service(rows, hasher, sessions, time, registration); }
    @Test void successfulLoginNormalizesIdentifierAndWipesPassword() {
        rows.add(1, "synthetic@example.invalid", Account.Status.ACTIVE, Account.Role.MEMBER);
        char[] value = input(); var handle = service(false).login(" SYNTHETIC@example.invalid ", value);
        assertEquals(1, sessions.require(handle).accountId()); assertEquals(Account.Role.MEMBER, sessions.require(handle).role());
        assertArrayEquals(new char[value.length], value); assertFalse(handle.toString().contains("synthetic"));
    }
    @Test void wrongUnknownPendingAndDisabledLoginsShareTheSameSafeOutcome() {
        for (Account.Status status : Account.Status.values()) {
            var r = new Memory(); var t = new Time(); var h = new Hasher(); var sm = new SessionManager(r, t);
            r.add(1, "synthetic@example.invalid", status, Account.Role.MEMBER);
            var s = AuthTestSupport.service(r, h, sm, t, false);
            if (status != Account.Status.ACTIVE) reason(INVALID_CREDENTIALS, () -> s.login("synthetic@example.invalid", input()));
            reason(INVALID_CREDENTIALS, () -> s.login("synthetic@example.invalid", "wrong synthetic value".toCharArray()));
            reason(INVALID_CREDENTIALS, () -> s.login("unknown@example.invalid", input()));
            assertEquals(1, h.unknown); assertThrows(AuthenticationException.class, sm::capture);
        }
    }
    @Test void badInputNeverOpensRepositoryAndFailedLoginClearsOldIdentity() {
        var s = service(false); rows.add(1, "synthetic@example.invalid", Account.Status.ACTIVE, Account.Role.ADMIN);
        var old = s.login("synthetic@example.invalid", input()); int before = rows.reads;
        reason(INVALID_CREDENTIALS, () -> s.login("invalid", input())); assertEquals(before, rows.reads);
        reason(AUTHENTICATION_REQUIRED, () -> sessions.require(old));
        reason(INVALID_CREDENTIALS, () -> s.login("synthetic@example.invalid", new char[129]));
    }
    @Test void closedRegistrationDoesNotHashWriteOrStartSessionAndWipesInput() {
        char[] value = input(); reason(REGISTRATION_CLOSED, () -> service(false).register("synthetic@example.invalid", "Synthetic", null, value));
        assertEquals(0, hasher.hashes); assertEquals(0, rows.writes); assertArrayEquals(new char[value.length], value);
        reason(AUTHENTICATION_REQUIRED, sessions::capture);
    }
    @Test void enabledRegistrationOnlyCreatesPendingMembersAndDuplicatesRemainNonEnumerating() {
        var s = service(true);
        s.register(" SYNTHETIC@example.invalid ", " Synthetic ", " ", input());
        var a = rows.rows.get(1L); assertEquals(Account.Status.PENDING, a.status()); assertEquals(Account.Role.MEMBER, a.role());
        assertEquals("Synthetic", a.displayName());
        assertDoesNotThrow(() -> s.register("synthetic@example.invalid", "Another", null, input()));
        assertEquals(1, rows.rows.size()); assertEquals(a, rows.rows.get(1L)); reason(AUTHENTICATION_REQUIRED, sessions::capture);
    }
    @Test void registrationValidatesLengthsAndPasswordWithoutChangingItsWhitespace() {
        var s = service(true);
        for (String login : List.of("invalid", "x".repeat(255) + "@example.invalid"))
            reason(INVALID_REGISTRATION, () -> s.register(login, "Synthetic", null, input()));
        reason(INVALID_REGISTRATION, () -> s.register("synthetic@example.invalid", " ", null, input()));
        reason(INVALID_REGISTRATION, () -> s.register("synthetic@example.invalid", "Synthetic", "0".repeat(61), input()));
        for (char[] value : List.of(new char[14], new char[129], " ".repeat(20).toCharArray()))
            reason(INVALID_REGISTRATION, () -> s.register("synthetic@example.invalid", "Synthetic", null, value));
        assertEquals(0, rows.writes);
    }
    @Test void passwordLengthCountsUnicodeCodePointsAndDoesNotAcceptShortSupplementaryPasswords() {
        String supplementary = new String(Character.toChars(0x1F642));
        assertThrows(IllegalArgumentException.class, () -> AuthenticationService.validateNewPassword(supplementary.repeat(8).toCharArray()));
        assertDoesNotThrow(() -> AuthenticationService.validateNewPassword(supplementary.repeat(15).toCharArray()));
        assertDoesNotThrow(() -> AuthenticationService.validateNewPassword(supplementary.repeat(128).toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> AuthenticationService.validateNewPassword(supplementary.repeat(129).toCharArray()));
    }
    @Test void repositoryFailuresCannotAuthenticateOrLookLikeRegistrationSuccess() {
        rows.unavailable = true;
        reason(STORAGE_FAILURE, () -> service(false).login("synthetic@example.invalid", input()));
        reason(STORAGE_FAILURE, () -> service(true).register("synthetic@example.invalid", "Synthetic", null, input()));
        reason(AUTHENTICATION_REQUIRED, sessions::capture);
    }
    @Test void securityStateIsRecheckedAfterPasswordVerification() {
        rows.add(1, "synthetic@example.invalid", Account.Status.ACTIVE, Account.Role.MEMBER);
        var h = new Hasher() { @Override public boolean verify(char[] p, String e) {
            rows.add(1, "synthetic@example.invalid", Account.Status.DISABLED, Account.Role.MEMBER); return true;
        } };
        var s = AuthTestSupport.service(rows, h, sessions, time, false);
        reason(INVALID_CREDENTIALS, () -> s.login("synthetic@example.invalid", input())); reason(AUTHENTICATION_REQUIRED, sessions::capture);
    }
    @Test void logoutDuringSlowAuthenticationCannotResurrectASession() throws Exception {
        rows.add(1, "synthetic@example.invalid", Account.Status.ACTIVE, Account.Role.MEMBER);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var h = new Hasher() { @Override public boolean verify(char[] p, String e) {
            entered.countDown(); try { assertTrue(release.await(10, TimeUnit.SECONDS)); } catch (InterruptedException ex) { throw new AssertionError(); } return true;
        } };
        var s = AuthTestSupport.service(rows, h, sessions, time, false);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> s.login("synthetic@example.invalid", input()));
            assertTrue(entered.await(10, TimeUnit.SECONDS)); sessions.logout(); release.countDown();
            assertInstanceOf(AuthenticationException.class, assertThrows(ExecutionException.class, future::get).getCause());
            reason(AUTHENTICATION_REQUIRED, sessions::capture);
        }
    }
    @Test void perIdentifierAndGlobalThrottleApplyToUnknownAccountsAndExpire() {
        var s = service(false);
        for (int i = 0; i < 5; i++) reason(INVALID_CREDENTIALS, () -> s.login("unknown@example.invalid", input()));
        reason(THROTTLED, () -> s.login("unknown@example.invalid", input()));
        for (int i = 0; i < 4; i++) { String id = "synthetic" + i + "@example.invalid"; reason(INVALID_CREDENTIALS, () -> s.login(id, input())); }
        reason(THROTTLED, () -> s.login("other@example.invalid", input()));
        time.advance(301); reason(INVALID_CREDENTIALS, () -> s.login("unknown@example.invalid", input()));
    }
    private void reason(AuthenticationException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(AuthenticationException.class, action); assertEquals(reason, failure.reason());
        assertNull(failure.getCause()); assertEquals(0, failure.getSuppressed().length);
    }
}
