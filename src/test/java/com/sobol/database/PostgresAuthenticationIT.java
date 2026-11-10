package com.sobol.database;

import com.sobol.auth.application.*;
import com.sobol.auth.domain.Account;
import com.sobol.auth.infrastructure.*;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** NOT executed offline. Separate owner-account-DML and synthetic-IT approvals are both mandatory. */
class PostgresAuthenticationIT {
    private static PostgresDatabase.Configuration app, owner;
    private static Connection lock;
    private static Pbkdf2PasswordHasher hasher;
    private final Set<Long> owned = new LinkedHashSet<>();
    private static char[] secret() { return "synthetic integration passphrase".toCharArray(); }
    private static Connection app() throws SQLException { return PostgresDatabase.open(app); }
    private static Connection owner() throws SQLException { return PostgresDatabase.open(owner); }
    private JdbcAccountRepository repository() { return new JdbcAccountRepository(PostgresAuthenticationIT::app); }
    private static String login() { return "synthetic-" + UUID.randomUUID() + "@example.invalid"; }
    private Account fixture() {
        var a = repository().createPending(login(), "Synthetic integration", null, hasher.hash(secret())); owned.add(a.id()); return a;
    }
    @BeforeAll static void ready() {
        try {
            // Validate BOTH approvals before the first connection. Neither gate implies the other.
            app = PostgresDatabase.configuration(System.getenv(), PostgresDatabase.Purpose.INTEGRATION);
            owner = PostgresDatabase.configuration(System.getenv(), PostgresDatabase.Purpose.ACCOUNT_ADMIN);
            if (!app.url.equals(owner.url) || !app.certificate.equals(owner.certificate)) throw PostgresDatabase.unavailable();
            lock = app();
            try (Statement p = lock.createStatement(); ResultSet r = p.executeQuery("SELECT pg_try_advisory_lock(736265, 6)")) {
                if (!r.next() || !r.getBoolean(1)) throw PostgresDatabase.unavailable();
            }
            PostgresMigrations.validateDestination(lock); PostgresAccounts.verifyVersion(lock);
            PostgresMigrations.plan(app).validate();
            try (Connection c = owner()) { PostgresAccounts.verifyVersion(c); }
            hasher = new Pbkdf2PasswordHasher();
        } catch (Exception failure) {
            try { finish(); } catch (SQLException ignored) { }
            throw new IllegalStateException("Authentication IT preflight stopped. Review both approvals, target, V2, TLS and least privileges privately.");
        }
    }
    @AfterAll static void finish() throws SQLException {
        if (lock != null) { try { lock.close(); } catch (SQLException failure) { throw PostgresDatabase.unavailable(); } finally { lock = null; } }
    }
    @AfterEach void cleanupOnlyOwnedAccountIds() throws SQLException {
        if (owned.isEmpty()) return;
        try (Connection c = owner()) {
            for (long id : owned) {
                try (PreparedStatement p = c.prepareStatement("DELETE FROM sobol.account WHERE account_id=?")) { p.setLong(1,id); p.executeUpdate(); }
                try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM sobol.account WHERE account_id=?")) { p.setLong(1,id); try (ResultSet r = p.executeQuery()) { assertFalse(r.next(), "Owned account cleanup was not confirmed"); } }
            }
        } catch (SQLException failure) { throw new SQLException("Owned synthetic account cleanup could not be confirmed. Inspect this approved run privately; do not broaden deletion."); }
    }
    @Test void pendingDefaultsDuplicateAndClosedRegistrationDoNotGrantAccess() {
        var a = fixture(); assertEquals(Account.Status.PENDING, a.status()); assertEquals(Account.Role.MEMBER, a.role());
        assertEquals(AuthenticationException.Reason.DUPLICATE_LOGIN, assertThrows(AuthenticationException.class, () -> repository().createPending(a.login(), "Other", null, hasher.hash(secret()))).reason());
        var sessions = new SessionManager(repository(), Clock.systemUTC());
        var service = new AuthenticationService(repository(), hasher, sessions, new LoginThrottle(Clock.systemUTC()), () -> false);
        assertEquals(AuthenticationException.Reason.REGISTRATION_CLOSED, assertThrows(AuthenticationException.class, () -> service.register(login(), "Synthetic", null, secret())).reason());
        assertEquals(AuthenticationException.Reason.INVALID_CREDENTIALS, assertThrows(AuthenticationException.class, () -> service.login(a.login(), secret())).reason());
        assertEquals(a, repository().findById(a.id()).orElseThrow());
    }
    @Test void ownerApprovalRealKdfLoginWrongPasswordAndLogout() throws SQLException {
        var a = fixture();
        try (Connection c = owner()) { PostgresAccounts.approve(c, a.id()); } catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
        var sessions = new SessionManager(repository(), Clock.systemUTC());
        var service = new AuthenticationService(repository(), hasher, sessions, new LoginThrottle(Clock.systemUTC()), () -> false);
        var handle = service.login(a.login().toUpperCase(Locale.ROOT), secret()); assertEquals(a.id(), sessions.require(handle).accountId());
        sessions.logout(); assertThrows(AuthenticationException.class, () -> sessions.require(handle));
        assertEquals(AuthenticationException.Reason.INVALID_CREDENTIALS, assertThrows(AuthenticationException.class, () -> service.login(a.login(), "wrong synthetic value".toCharArray())).reason());
    }
    @Test void ownerStatusRoleAndVersionChangesRevokeExistingSessions() throws SQLException {
        var a = fixture();
        try (Connection c = owner()) { PostgresAccounts.approve(c, a.id()); } catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
        var sessions = new SessionManager(repository(), Clock.systemUTC());
        var service = new AuthenticationService(repository(), hasher, sessions, new LoginThrottle(Clock.systemUTC()), () -> false);
        for (String change : List.of("auth_version=auth_version+1", "role='ADMIN'", "status='DISABLED'")) {
            var handle = service.login(a.login(), secret());
            try (Connection c = owner(); PreparedStatement p = c.prepareStatement("UPDATE sobol.account SET " + change + ",updated_at=CURRENT_TIMESTAMP WHERE account_id=?")) {
                p.setLong(1,a.id()); assertEquals(1,p.executeUpdate());
            } catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
            assertThrows(AuthenticationException.class, () -> sessions.require(handle));
        }
    }
    @Test void applicationRoleCannotPromoteActivateModifyOrDeleteAccounts() throws SQLException {
        var a = fixture();
        for (String sql : List.of("UPDATE sobol.account SET status='ACTIVE' WHERE account_id=?", "UPDATE sobol.account SET role='ADMIN' WHERE account_id=?", "DELETE FROM sobol.account WHERE account_id=?")) {
            try (Connection c = app()) { rejected(c, sql, "42501", p -> p.setLong(1,a.id())); } catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
        }
        try (Connection c = app()) { rejected(c, "INSERT INTO sobol.account(login,display_name,password_hash,role,status) VALUES (?,'Synthetic',?,'ADMIN','ACTIVE')", "42501", p -> { p.setString(1,login()); p.setString(2,hasher.hash(secret())); }); }
        catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
        assertEquals(a, repository().findById(a.id()).orElseThrow());
    }
    @Test void databaseRejectsNoncanonicalLoginLegacyHashAndInvalidSecurityState() throws SQLException {
        String encoded = hasher.hash(secret());
        for (String[] values : new String[][]{{"UPPER@example.invalid",encoded,"PENDING","MEMBER"}, {login(),"legacy-not-accepted","PENDING","MEMBER"}, {login(),encoded,"UNKNOWN","MEMBER"}, {login(),encoded,"PENDING","ROOT"}}) {
            try (Connection c = owner()) { rejected(c, "INSERT INTO sobol.account(login,display_name,password_hash,status,role) VALUES (?,'Synthetic',?,?,?)", "23514", p -> { for(int i=0;i<4;i++) p.setString(i+1,values[i]); }); }
            catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
        }
    }
    @Test void accountInsertRollbackAndTimestampTypesAreVerifiedByPostgres() throws SQLException {
        String identifier = login();
        try (Connection c = app()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO sobol.account(login,display_name,password_hash) VALUES (?,'Synthetic',?) RETURNING account_id,created_at,updated_at")) {
                p.setString(1,identifier); p.setString(2,hasher.hash(secret()));
                try (ResultSet r = p.executeQuery()) { assertTrue(r.next()); assertTrue(r.getLong(1)>0); assertNotNull(r.getObject(2,java.time.OffsetDateTime.class)); assertNotNull(r.getObject(3,java.time.OffsetDateTime.class)); assertFalse(r.next()); }
            } finally { c.rollback(); }
        } catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
        assertTrue(repository().findCredential(identifier).isEmpty());
    }
    @FunctionalInterface private interface Bind { void apply(PreparedStatement p) throws SQLException; }
    private static void rejected(Connection c, String sql, String expectedState, Bind bind) throws SQLException {
        c.setAutoCommit(false);
        try {
            String state = null;
            try (PreparedStatement p = c.prepareStatement(sql)) { bind.apply(p); p.executeUpdate(); }
            catch (SQLException failure) { state = failure.getSQLState(); }
            assertEquals(expectedState,state,"Expected database rejection");
        } finally { c.rollback(); } // Even an unexpectedly accepted statement never commits.
    }
}
