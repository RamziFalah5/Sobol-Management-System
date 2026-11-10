package com.sobol.database;

import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostgresDatabaseTest {
    static Map<String, String> env() {
        return new HashMap<>(Map.of("SOBOL_PG_ENABLED", "true",
                "SOBOL_PG_URL", "jdbc:postgresql://127.0.0.1:5432/sobol_dev", "SOBOL_PG_USER", "sobol_app",
                "SOBOL_PG_PASSWORD", "synthetic-app-value", "SOBOL_PG_SSLROOTCERT", Path.of("pom.xml").toAbsolutePath().toString(),
                "SOBOL_PG_MIGRATION_USER", "sobol_migrator", "SOBOL_PG_MIGRATION_PASSWORD", "synthetic-migration-value",
                "SOBOL_PG_MIGRATION_APPROVED", "sobol_dev/sobol", "SOBOL_PG_IT_APPROVED", "sobol_dev/sobol"));
    }
    @Test void everyApplicationSettingIsExplicitAndDisabledByDefault() throws Exception {
        for (String key : List.of("SOBOL_PG_ENABLED", "SOBOL_PG_URL", "SOBOL_PG_USER", "SOBOL_PG_PASSWORD", "SOBOL_PG_SSLROOTCERT")) {
            var e = env(); e.remove(key);
            safe(assertThrows(SQLException.class, () -> PostgresDatabase.configuration(e, PostgresDatabase.Purpose.APPLICATION)));
        }
        assertThrows(SQLException.class, PostgresDatabase::get);
        assertEquals("false", System.getenv("SOBOL_PG_ENABLED"));
    }
    @Test void rejectsAlternativeHostsTargetsSchemesAndUrlOptionsBeforeConnecting() {
        for (String url : List.of("jdbc:mysql://127.0.0.1:5432/sobol_dev", "jdbc:postgresql://localhost:5432/sobol_dev",
                "jdbc:postgresql://example.invalid:5432/sobol_dev", "jdbc:postgresql://[::1]:5432/sobol_dev",
                "jdbc:postgresql://127.0.0.1/sobol_dev", "jdbc:postgresql://127.0.0.1:0/sobol_dev",
                "jdbc:postgresql://127.0.0.1:65536/sobol_dev", "jdbc:postgresql://127.0.0.1:5432/accounts",
                "jdbc:postgresql://127.0.0.1:5432/postgres", "jdbc:postgresql://127.0.0.1:5432/%73obol_dev",
                "jdbc:postgresql://127.0.0.1:5432/sobol_dev?sslmode=disable", "jdbc:postgresql://127.0.0.1:5432/sobol_dev#x",
                "jdbc:postgresql://synthetic@127.0.0.1:5432/sobol_dev", "jdbc:postgresql://127.0.0.1:5432,example.invalid/sobol_dev")) {
            var e = env(); e.put("SOBOL_PG_URL", url);
            safe(assertThrows(SQLException.class, () -> PostgresDatabase.configuration(e, PostgresDatabase.Purpose.APPLICATION)));
        }
    }
    @Test void requiresDedicatedRoleAbsoluteCertificateAndSeparateOperationApprovals() {
        for (String user : List.of("postgres", "root", "sobol_migrator", "other")) {
            var e = env(); e.put("SOBOL_PG_USER", user);
            assertThrows(SQLException.class, () -> PostgresDatabase.configuration(e, PostgresDatabase.Purpose.APPLICATION));
        }
        var relative = env(); relative.put("SOBOL_PG_SSLROOTCERT", "relative.crt");
        assertThrows(SQLException.class, () -> PostgresDatabase.configuration(relative, PostgresDatabase.Purpose.APPLICATION));
        for (var purpose : List.of(PostgresDatabase.Purpose.MIGRATION, PostgresDatabase.Purpose.INTEGRATION)) {
            var e = env(); e.remove(purpose == PostgresDatabase.Purpose.MIGRATION ? "SOBOL_PG_MIGRATION_APPROVED" : "SOBOL_PG_IT_APPROVED");
            assertThrows(SQLException.class, () -> PostgresDatabase.configuration(e, purpose));
        }
        var same = env(); same.put("SOBOL_PG_MIGRATION_PASSWORD", same.get("SOBOL_PG_PASSWORD"));
        assertThrows(SQLException.class, () -> PostgresDatabase.configuration(same, PostgresDatabase.Purpose.MIGRATION));
        var admin = env(); admin.put("SOBOL_PG_MIGRATION_USER", "postgres");
        assertThrows(SQLException.class, () -> PostgresDatabase.configuration(admin, PostgresDatabase.Purpose.MIGRATION));
    }
    @Test void transportPropertiesCannotBeWeakenedAndConfigurationDoesNotPrintSecrets() throws Exception {
        var config = PostgresDatabase.configuration(env(), PostgresDatabase.Purpose.APPLICATION);
        var p = config.properties();
        assertEquals("verify-full", p.getProperty("sslmode")); assertEquals("sobol", p.getProperty("currentSchema"));
        assertEquals("5", p.getProperty("connectTimeout")); assertEquals("15", p.getProperty("socketTimeout"));
        assertFalse(config.toString().contains("synthetic"));
    }
    @Test void acceptsOnlyVerifiedDedicatedEncryptedSessionAndClosesMetadataResources() throws Exception {
        var session = new Session(); var config = PostgresDatabase.configuration(env(), PostgresDatabase.Purpose.APPLICATION);
        assertSame(session.connection(), PostgresDatabase.open(config, (url, p) -> {
            assertEquals(config.url, url); assertEquals("verify-full", p.getProperty("sslmode")); return session.connection();
        }));
        assertTrue(session.statementClosed); assertTrue(session.resultClosed); assertFalse(session.closed);
    }
    @Test void refusesWrongTargetIdentityPrivilegeTransportOrRoleMembershipAndClosesConnection() throws Exception {
        for (int column : List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)) {
            var session = new Session(); session.values.put(column, column <= 5 ? "unexpected" : column != 10);
            var config = PostgresDatabase.configuration(env(), PostgresDatabase.Purpose.APPLICATION);
            safe(assertThrows(SQLException.class, () -> PostgresDatabase.open(config, (u, p) -> session.connection())));
            assertTrue(session.closed); assertTrue(session.statementClosed); assertTrue(session.resultClosed);
        }
    }
    @Test void driverAndResourceCloseFailuresAreSanitized() throws Exception {
        var config = PostgresDatabase.configuration(env(), PostgresDatabase.Purpose.APPLICATION);
        safe(assertThrows(SQLException.class, () -> PostgresDatabase.open(config, (u, p) -> { throw new SQLException("private synthetic diagnostic"); })));
        for (boolean metadata : List.of(false, true)) {
            var session = new Session(); session.failMetadata = metadata; session.failClose = true;
            if (!metadata) session.values.put(1, "unexpected");
            safe(assertThrows(SQLException.class, () -> PostgresDatabase.open(config, (u, p) -> session.connection())));
            assertTrue(session.closed);
        }
    }
    @Test void missingCertificateStopsBeforeDriverIsInvoked() throws Exception {
        var e = env(); e.put("SOBOL_PG_SSLROOTCERT", Path.of("target/nonexistent-certificate.crt").toAbsolutePath().toString());
        var config = PostgresDatabase.configuration(e, PostgresDatabase.Purpose.APPLICATION);
        safe(assertThrows(SQLException.class, () -> PostgresDatabase.open(config, (u, p) -> { fail("Must not connect"); return null; })));
    }
    @Test void migrationIdentityMustHaveOnlyTheExpectedSchemaCreationCapability() throws Exception {
        var config = PostgresDatabase.configuration(env(), PostgresDatabase.Purpose.MIGRATION);
        var session = new Session(); session.values.put(3, "sobol_migrator"); session.values.put(4, "sobol_migrator"); session.values.put(8, true);
        assertSame(session.connection(), PostgresDatabase.open(config, (u, p) -> session.connection()));
        var rejected = new Session();
        assertThrows(SQLException.class, () -> PostgresDatabase.open(config, (u, p) -> rejected.connection()));
        assertTrue(rejected.closed);
    }
    @Test void accountAdministrationRequiresItsOwnApprovalAndDedicatedOwnerIdentity() throws Exception {
        var e = env();
        assertThrows(SQLException.class, () -> PostgresDatabase.configuration(e, PostgresDatabase.Purpose.ACCOUNT_ADMIN));
        e.put("SOBOL_PG_ACCOUNT_ADMIN_APPROVED", "sobol_dev/sobol");
        e.remove("SOBOL_PG_MIGRATION_APPROVED");
        var config = PostgresDatabase.configuration(e, PostgresDatabase.Purpose.ACCOUNT_ADMIN);
        var session = new Session(); session.values.put(3, "sobol_migrator"); session.values.put(4, "sobol_migrator"); session.values.put(8, true);
        assertSame(session.connection(), PostgresDatabase.open(config, (u, p) -> session.connection()));
        e.put("SOBOL_PG_MIGRATION_USER", "sobol_app");
        assertThrows(SQLException.class, () -> PostgresDatabase.configuration(e, PostgresDatabase.Purpose.ACCOUNT_ADMIN));
    }
    private static void safe(SQLException failure) {
        assertNull(failure.getCause()); assertEquals(0, failure.getSuppressed().length);
        assertFalse(failure.getMessage().contains("private" + " synthetic"));
        assertFalse(failure.getMessage().contains("synthetic-app-value"));
    }
    static class Session {
        final Map<Integer, Object> values = new HashMap<>(Map.of(1, "sobol_dev", 2, "sobol", 3, "sobol_app", 4, "sobol_app", 5, "127.0.0.1", 6, false, 7, false, 8, false, 9, false, 10, true));
        { values.put(11, false); values.put(12, false); values.put(13, false); values.put(14, false); }
        boolean closed, statementClosed, resultClosed, failClose, failMetadata;
        final Connection connection = proxy(Connection.class, (o, m, a) -> switch (m.getName()) {
            case "createStatement" -> proxy(Statement.class, (s, method, args) -> switch (method.getName()) {
                case "executeQuery" -> {
                    if (failMetadata) throw new SQLException("private synthetic metadata");
                    int[] row = {0};
                    yield proxy(ResultSet.class, (r, rm, ra) -> switch (rm.getName()) {
                        case "next" -> ++row[0] == 1;
                        case "getString", "getBoolean" -> values.get(ra[0]);
                        case "close" -> { resultClosed = true; yield null; }
                        default -> throw new AssertionError(rm.getName());
                    });
                }
                case "close" -> { statementClosed = true; yield null; }
                default -> throw new AssertionError(method.getName());
            });
            case "close" -> { closed = true; if (failClose) throw new SQLException("private synthetic close"); yield null; }
            default -> throw new AssertionError(m.getName());
        });
        Connection connection() { return connection; }
    }
    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
