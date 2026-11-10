package com.sobol.auth.application;

import com.sobol.auth.domain.Account;
import java.time.*;
import java.util.*;
import static com.sobol.auth.application.AuthenticationException.Reason.*;

/** Synthetic identities and opaque fake KDF values for isolated policy/session tests only. */
public final class AuthTestSupport {
    private AuthTestSupport() { }
    public static char[] input() { return "synthetic passphrase for tests".toCharArray(); }
    public static final class Time extends Clock {
        public Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
        public void advance(long seconds) { now = now.plusSeconds(seconds); }
    }
    public static class Memory implements AccountRepository {
        public final Map<Long, Account> rows = new LinkedHashMap<>();
        public boolean unavailable;
        public int writes, reads;
        public void available() { if (unavailable) throw new AuthenticationException(STORAGE_FAILURE); }
        public Account add(long id, String login, Account.Status status, Account.Role role) {
            var a = new Account(id, login, "Synthetic " + id, status, role, 1); rows.put(id, a); return a;
        }
        public Optional<Credential> findCredential(String login) {
            reads++; available(); return rows.values().stream().filter(a -> a.login().equals(login)).findFirst().map(a -> new Credential(a, "opaque-synthetic-version"));
        }
        public Optional<Account> findById(long id) { reads++; available(); return Optional.ofNullable(rows.get(id)); }
        public Account createPending(String login, String name, String phone, String encoded) {
            available(); writes++;
            if (rows.values().stream().anyMatch(a -> a.login().equals(login))) throw new AuthenticationException(DUPLICATE_LOGIN);
            long id = rows.keySet().stream().mapToLong(Long::longValue).max().orElse(0) + 1;
            var a = new Account(id, login, name, Account.Status.PENDING, Account.Role.MEMBER, 1); rows.put(id, a); return a;
        }
    }
    public static class Hasher implements PasswordHasher {
        public int checks, unknown, hashes;
        public String hash(char[] value) { hashes++; return "opaque-synthetic-version"; }
        public boolean verify(char[] value, String encoded) { checks++; return Arrays.equals(input(), value); }
        public void verifyUnknown(char[] value) { unknown++; checks++; }
    }
    public static AuthenticationService service(Memory rows, PasswordHasher hasher, SessionManager sessions, Clock time, boolean registration) {
        return new AuthenticationService(rows, hasher, sessions, new LoginThrottle(time), () -> registration);
    }
}
