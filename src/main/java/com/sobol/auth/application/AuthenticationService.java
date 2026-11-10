package com.sobol.auth.application;

import com.sobol.auth.domain.Account;
import java.util.*;
import java.util.function.BooleanSupplier;
import static com.sobol.auth.application.AuthenticationException.Reason.*;

public final class AuthenticationService {
    private final AccountRepository repository;
    private final PasswordHasher hasher;
    private final SessionManager sessions;
    private final LoginThrottle throttle;
    private final BooleanSupplier registration;
    public AuthenticationService(AccountRepository repository, PasswordHasher hasher, SessionManager sessions,
                                 LoginThrottle throttle, BooleanSupplier registration) {
        this.repository = Objects.requireNonNull(repository); this.hasher = Objects.requireNonNull(hasher);
        this.sessions = Objects.requireNonNull(sessions); this.throttle = Objects.requireNonNull(throttle); this.registration = Objects.requireNonNull(registration);
    }
    /** Always consumes/wipes the supplied character array, including failures. */
    public SessionManager.Handle login(String identifier, char[] password) {
        long attempt = sessions.beginAuthentication();
        try {
            String login;
            try { login = Account.normalizeLogin(identifier); }
            catch (IllegalArgumentException invalid) { throttle.acquire("invalid"); throw new AuthenticationException(INVALID_CREDENTIALS); }
            throttle.acquire(login);
            if (password == null || password.length == 0 || password.length > 256) throw new AuthenticationException(INVALID_CREDENTIALS);
            var credential = repository.findCredential(login);
            boolean matched;
            if (credential.isEmpty()) { hasher.verifyUnknown(password); matched = false; }
            else matched = hasher.verify(password, credential.get().encoded());
            if (!matched || credential.orElseThrow().account().status() != Account.Status.ACTIVE) throw new AuthenticationException(INVALID_CREDENTIALS);
            // Re-read security state after the KDF, so a concurrent disable/role/password version change cannot establish stale identity.
            var verified = credential.get().account();
            var current = repository.findById(verified.id()).orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));
            if (current.status() != Account.Status.ACTIVE || current.role() != verified.role() || current.authVersion() != verified.authVersion())
                throw new AuthenticationException(INVALID_CREDENTIALS);
            return sessions.establish(attempt, current);
        } catch (AuthenticationException failure) { throw new AuthenticationException(failure.reason()); }
        catch (RuntimeException failure) { throw new AuthenticationException(STORAGE_FAILURE); }
        finally { if (password != null) Arrays.fill(password, '\0'); }
    }
    public void register(String identifier, String displayName, String phone, char[] password) {
        sessions.logout();
        try {
            if (!registration.getAsBoolean()) throw new AuthenticationException(REGISTRATION_CLOSED);
            String login, name, contact;
            try {
                login = Account.normalizeLogin(identifier); name = Account.name(displayName);
                contact = phone == null || phone.trim().isBlank() ? null : phone.trim();
                if (contact != null && contact.codePointCount(0, contact.length()) > 60) throw new IllegalArgumentException();
                validateNewPassword(password);
            } catch (IllegalArgumentException invalid) { throw new AuthenticationException(INVALID_REGISTRATION); }
            throttle.acquire(login);
            String encoded = hasher.hash(password);
            try { repository.createPending(login, name, contact, encoded); }
            catch (AuthenticationException failure) { if (failure.reason() != DUPLICATE_LOGIN) throw failure; }
            // The same response covers an existing identifier. Registration never starts a session.
        } catch (AuthenticationException failure) { throw new AuthenticationException(failure.reason()); }
        catch (RuntimeException failure) { throw new AuthenticationException(STORAGE_FAILURE); }
        finally { if (password != null) Arrays.fill(password, '\0'); }
    }
    public static void validateNewPassword(char[] password) {
        if (password == null || Character.codePointCount(password, 0, password.length) < 15 || Character.codePointCount(password, 0, password.length) > 128) throw new IllegalArgumentException("Invalid password length.");
        boolean content = false;
        for (char c : password) if (!Character.isWhitespace(c)) content = true;
        if (!content) throw new IllegalArgumentException("Invalid password.");
    }
}
