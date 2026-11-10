package com.sobol.database;

import com.sobol.auth.application.AuthenticationService;
import com.sobol.auth.domain.Account;
import com.sobol.auth.infrastructure.Pbkdf2PasswordHasher;
import java.sql.*;
import java.util.Arrays;

/** Owner-only, test-classpath maintenance command. Never invoked by application startup. No DDL. */
public final class PostgresAccounts {
    private PostgresAccounts() { }
    public static void main(String[] args) {
        char[] password = null, confirmation = null;
        try {
            if (args.length != 1 || !java.util.Set.of("bootstrap-admin", "approve-member", "disable").contains(args[0])) throw PostgresDatabase.unavailable();
            var config = PostgresDatabase.configuration(System.getenv(), PostgresDatabase.Purpose.ACCOUNT_ADMIN);
            var console = System.console();
            if (console == null) throw PostgresDatabase.unavailable(); // Never fall back to echoed input.
            if (!"PROVISION sobol_dev/sobol".equals(console.readLine("Confirm reviewed account DML target: "))) throw PostgresDatabase.unavailable();
            String login = null, name = null, encoded = null;
            long id = 0;
            if ("bootstrap-admin".equals(args[0])) {
                login = Account.normalizeLogin(console.readLine("New administrator login email: "));
                name = Account.name(console.readLine("Display name: "));
                password = console.readPassword("New private passphrase (15-128 Unicode code points): ");
                confirmation = console.readPassword("Confirm passphrase: ");
                AuthenticationService.validateNewPassword(password);
                if (!Arrays.equals(password, confirmation)) throw PostgresDatabase.unavailable();
                encoded = new Pbkdf2PasswordHasher().hash(password);
            } else {
                id = Long.parseLong(console.readLine("Reviewed account ID: "));
                if (id <= 0) throw PostgresDatabase.unavailable();
            }
            try (Connection c = PostgresDatabase.open(config)) {
                verifyVersion(c); PostgresMigrations.validateDestination(c);
                PostgresMigrations.plan(config).validate(); // Read-only checksum validation only.
                if ("bootstrap-admin".equals(args[0])) bootstrap(c, login, name, encoded);
                else if ("approve-member".equals(args[0])) approve(c, id);
                else disable(c, id);
            }
            console.printf("Reviewed account operation confirmed. No credentials or account details printed.%n");
        } catch (Exception failure) {
            throw new IllegalStateException("Account operation could not be confirmed. Review target, approval, console, policy and state privately. Do not retry a create blindly.");
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
    static void verifyVersion(Connection c) throws SQLException {
        try (Statement p = c.createStatement(); ResultSet r = p.executeQuery(
                "SELECT version, success FROM sobol.flyway_schema_history WHERE type='SQL' ORDER BY installed_rank")) {
            if (!r.next() || !"1".equals(r.getString(1)) || !r.getBoolean(2) || !r.next() || !"2".equals(r.getString(1)) || !r.getBoolean(2) || r.next()) throw PostgresDatabase.unavailable();
        }
    }
    @FunctionalInterface private interface Operation { void run() throws SQLException; }
    private static void transaction(Connection c, Operation operation) throws SQLException {
        try {
            c.setAutoCommit(false);
            try {
                // Serializes first-admin decisions and last-active-admin protection with app inserts.
                try (Statement p = c.createStatement()) { p.execute("LOCK TABLE sobol.account IN SHARE ROW EXCLUSIVE MODE"); }
                operation.run(); c.commit();
            } catch (SQLException | RuntimeException failure) {
                try { c.rollback(); } catch (SQLException ignored) { }
                throw PostgresDatabase.unavailable();
            }
        } catch (SQLException | RuntimeException failure) { throw PostgresDatabase.unavailable(); }
    }
    static void bootstrap(Connection c, String login, String name, String encoded) throws SQLException {
        transaction(c, () -> {
            try (Statement p = c.createStatement(); ResultSet r = p.executeQuery("SELECT count(*) FROM sobol.account WHERE role='ADMIN'")) {
                if (!r.next() || r.getLong(1) != 0 || r.next()) throw PostgresDatabase.unavailable();
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO sobol.account(login,display_name,password_hash,status,role) VALUES (?,?,?,'ACTIVE','ADMIN')")) {
                p.setString(1, login); p.setString(2, name); p.setString(3, encoded);
                if (p.executeUpdate() != 1) throw PostgresDatabase.unavailable();
            }
        });
    }
    static void approve(Connection c, long id) throws SQLException {
        transaction(c, () -> {
            if (id <= 0) throw PostgresDatabase.unavailable();
            try (PreparedStatement p = c.prepareStatement("UPDATE sobol.account SET status='ACTIVE',auth_version=auth_version+1,updated_at=CURRENT_TIMESTAMP WHERE account_id=? AND status='PENDING' AND role='MEMBER'")) {
                p.setLong(1, id); if (p.executeUpdate() != 1) throw PostgresDatabase.unavailable();
            }
        });
    }
    static void disable(Connection c, long id) throws SQLException {
        transaction(c, () -> {
            if (id <= 0) throw PostgresDatabase.unavailable();
            try (PreparedStatement p = c.prepareStatement("""
                    UPDATE sobol.account SET status='DISABLED',auth_version=auth_version+1,updated_at=CURRENT_TIMESTAMP
                    WHERE account_id=? AND status<>'DISABLED' AND
                    (role<>'ADMIN' OR EXISTS (SELECT 1 FROM sobol.account WHERE role='ADMIN' AND status='ACTIVE' AND account_id<>?))
                    """)) {
                p.setLong(1, id); p.setLong(2, id); if (p.executeUpdate() != 1) throw PostgresDatabase.unavailable();
            }
        });
    }
}
