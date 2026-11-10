package com.sobol.auth.application;

import com.sobol.auth.domain.Account;
import java.time.*;
import java.util.*;
import static com.sobol.auth.application.AuthenticationException.Reason.*;

/** One application-instance session. Handles are unforgeable objects and never contain credentials. */
public final class SessionManager {
    public record Identity(long accountId, String displayName, Account.Role role) {
        @Override public String toString() { return "Authenticated identity [redacted]"; }
    }
    public static final class Handle {
        private Handle() { }
        @Override public String toString() { return "Session handle [redacted]"; }
    }
    private final AccountRepository repository;
    private final Clock clock;
    private long generation;
    private Identity identity;
    private long authVersion;
    private Handle handle;
    private Instant started, lastUsed;
    public SessionManager(AccountRepository repository, Clock clock) { this.repository = Objects.requireNonNull(repository); this.clock = Objects.requireNonNull(clock); }
    synchronized long beginAuthentication() { logout(); return generation; }
    synchronized Handle establish(long attempt, Account verified) {
        if (attempt != generation || verified.status() != Account.Status.ACTIVE) throw new AuthenticationException(AUTHENTICATION_REQUIRED);
        identity = new Identity(verified.id(), verified.displayName(), verified.role()); authVersion = verified.authVersion();
        handle = new Handle(); started = lastUsed = clock.instant(); return handle;
    }
    public synchronized void logout() { generation++; identity = null; authVersion = 0; handle = null; started = lastUsed = null; }
    public synchronized void invalidate(Handle expected) { if (handle == expected) logout(); }
    public synchronized Handle capture() { validate(handle, true); return handle; }
    public synchronized Identity require(Handle expected) { return validate(expected, true); }
    /** Polls revocation/expiry without extending idle lifetime. */
    public synchronized Identity inspect(Handle expected) { return validate(expected, false); }
    private Identity validate(Handle expected, boolean touch) {
        if (expected == null || handle != expected || identity == null) throw new AuthenticationException(AUTHENTICATION_REQUIRED);
        Instant now = clock.instant();
        if (!now.isBefore(started.plus(Duration.ofHours(8))) || !now.isBefore(lastUsed.plus(Duration.ofMinutes(15)))) {
            logout(); throw new AuthenticationException(AUTHENTICATION_REQUIRED);
        }
        Account current;
        try { current = repository.findById(identity.accountId()).orElse(null); }
        catch (RuntimeException failure) { logout(); throw new AuthenticationException(STORAGE_FAILURE); }
        if (current == null || current.status() != Account.Status.ACTIVE || current.role() != identity.role()
                || current.authVersion() != authVersion) {
            logout(); throw new AuthenticationException(AUTHENTICATION_REQUIRED);
        }
        if (touch) lastUsed = now;
        // Do not retain email, phone or the credential object in session state.
        return identity;
    }
}
