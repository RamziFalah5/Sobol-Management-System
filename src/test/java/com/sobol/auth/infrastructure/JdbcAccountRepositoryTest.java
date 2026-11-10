package com.sobol.auth.infrastructure;

import com.sobol.auth.application.*;
import com.sobol.auth.domain.Account;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static com.sobol.database.PostgresErrorsTest.conflict;
import static com.sobol.auth.application.AuthenticationException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

class JdbcAccountRepositoryTest {
    static Map<String, Object> row(String status, String role) {
        return new HashMap<>(Map.of("account_id", 42L, "login", "synthetic@example.invalid", "display_name", "Synthetic",
                "status", status, "role", role, "auth_version", 1L, "password_hash", "opaque-synthetic-version"));
    }
    @Test void credentialAndSecurityReadsMapSeparatelyAndCloseResources() {
        var s = new Script(); s.rows = List.of(row("ACTIVE", "MEMBER"));
        var c = s.repository().findCredential("synthetic@example.invalid").orElseThrow();
        assertEquals(42, c.account().id()); assertFalse(c.toString().contains(c.encoded())); assertEquals(1, s.parameters.size()); s.closed();
        assertTrue(s.sql.contains(", password_hash"));
        s = new Script(); s.rows = List.of(row("ACTIVE", "ADMIN")); assertEquals(Account.Role.ADMIN, s.repository().findById(42).orElseThrow().role());
        assertFalse(s.sql.contains("password_hash")); assertEquals(42L, s.parameters.get(1)); s.closed();
    }
    @Test void absentRowsAreEmptyButReadOrMappingFailuresAreNot() {
        var s = new Script(); assertTrue(s.repository().findById(42).isEmpty()); s.closed();
        for (String status : List.of("unknown", "ACTIVE")) {
            var bad = new Script(); bad.rows = List.of(row(status, status.equals("ACTIVE") ? "UNKNOWN" : "MEMBER"));
            safe(STORAGE_FAILURE, () -> bad.repository().findCredential("synthetic@example.invalid")); bad.closed();
        }
        var bad = new Script(); bad.failure = conflict("sobol", "account", "uq_account_login", "23505");
        safe(STORAGE_FAILURE, () -> bad.repository().findCredential("synthetic@example.invalid")); bad.closed();
    }
    @Test void registrationBindsOnlyAllowedFieldsAndCommitsPendingMember() {
        var s = pending(); var value = create(s); assertEquals(Account.Status.PENDING, value.status()); assertEquals(Account.Role.MEMBER, value.role());
        assertTrue(s.sql.contains("RETURNING account_id")); assertFalse(s.sql.substring(0, s.sql.indexOf("VALUES")).contains("role"));
        assertEquals("synthetic@example.invalid", s.parameters.get(1)); assertEquals("Synthetic", s.parameters.get(2));
        assertTrue(s.parameters.containsKey(3)); assertNull(s.parameters.get(3)); assertEquals("opaque-synthetic-version", s.parameters.get(4));
        assertTrue(s.committed); assertFalse(s.rolledBack); s.closed();
    }
    @Test void onlyExactWriteConstraintClassifiesDuplicateAndUnknownDiagnosticsStaySafe() {
        var exact = pending(); exact.failure = conflict("sobol", "account", "uq_account_login", "23505");
        safe(DUPLICATE_LOGIN, () -> create(exact)); assertTrue(exact.rolledBack); exact.closed();
        for (SQLException failure : List.of(conflict("other", "account", "uq_account_login", "23505"),
                conflict("sobol", "customer", "uq_account_login", "23505"), conflict("sobol", "account", "unknown", "23505"),
                conflict("sobol", "account", "uq_account_login", "23514"), new SQLException("uq_account_login", "23505"))) {
            var s = pending(); s.failure = failure; safe(STORAGE_FAILURE, () -> create(s)); assertTrue(s.rolledBack); s.closed();
        }
    }
    @Test void unexpectedMissingExtraInvalidOrPrivilegedReturnedRowsRollBack() {
        var missing = pending(); missing.rows = List.of(); safe(STORAGE_FAILURE, () -> create(missing)); assertTrue(missing.rolledBack);
        for (String[] fields : new String[][]{{"ACTIVE", "MEMBER"}, {"PENDING", "ADMIN"}}) {
            var s = pending(); s.rows = List.of(row(fields[0], fields[1])); safe(STORAGE_FAILURE, () -> create(s)); assertTrue(s.rolledBack); s.closed();
        }
        var extra = pending(); extra.rows = List.of(row("PENDING", "MEMBER"), row("PENDING", "MEMBER"));
        safe(STORAGE_FAILURE, () -> create(extra)); assertTrue(extra.rolledBack);
        var invalid = pending(); var r = row("PENDING", "MEMBER"); r.put("account_id", 0L); invalid.rows = List.of(r);
        safe(STORAGE_FAILURE, () -> create(invalid)); assertTrue(invalid.rolledBack);
    }
    @Test void resultOrStatementCloseFailuresRollBackWithoutDriverDiagnostics() {
        for (boolean result : List.of(false, true)) {
            var s = pending(); s.resultCloseFailure = result; s.statementCloseFailure = !result;
            safe(STORAGE_FAILURE, () -> create(s)); assertTrue(s.rolledBack); assertFalse(s.committed); s.closed();
        }
    }
    @Test void suppressedConnectionAndRollbackCloseFailuresCannotLeak() {
        for (boolean rollback : List.of(false, true)) {
            var s = pending(); s.failure = conflict("sobol", "account", "uq_account_login", "23505");
            s.connectionCloseFailure = true; s.rollbackFailure = rollback;
            safe(STORAGE_FAILURE, () -> create(s)); s.closed();
        }
    }
    @Test void lostCommitResponseIsUnconfirmedBeforeOrAfterRecordedCommitAndNeverRetried() {
        for (boolean committed : List.of(false, true)) {
            var s = pending(); s.commitFailure = true; s.recordCommit = committed;
            safe(STORAGE_FAILURE, () -> create(s)); assertEquals(committed, s.committed); assertTrue(s.rolledBack);
            assertEquals(1, s.executions); s.closed();
        }
    }
    @Test void connectionCloseAfterCommitCannotReportSuccessOrDuplicate() {
        var s = pending(); s.connectionCloseFailure = true; safe(STORAGE_FAILURE, () -> create(s));
        assertTrue(s.committed); assertFalse(s.rolledBack); s.closed();
    }
    @Test void connectionFailuresDoNotReturnEmptyOrSuccess() {
        var repository = new JdbcAccountRepository(() -> { throw new SQLException("private synthetic connection"); });
        safe(STORAGE_FAILURE, () -> repository.findCredential("synthetic@example.invalid"));
        safe(STORAGE_FAILURE, () -> repository.findById(42));
        safe(STORAGE_FAILURE, () -> repository.createPending("synthetic@example.invalid", "Synthetic", null, "opaque-synthetic-version"));
    }
    static Script pending() { var s = new Script(); s.rows = List.of(row("PENDING", "MEMBER")); return s; }
    static Account create(Script s) { return s.repository().createPending("synthetic@example.invalid", "Synthetic", null, "opaque-synthetic-version"); }
    static void safe(AuthenticationException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(AuthenticationException.class, action); assertEquals(reason, failure.reason());
        assertNull(failure.getCause()); assertEquals(0, failure.getSuppressed().length); assertFalse(failure.getMessage().contains("private synthetic"));
    }
    static class Script {
        List<Map<String, Object>> rows = List.of(); final Map<Integer, Object> parameters = new HashMap<>(); String sql;
        boolean committed, rolledBack, connectionClosed, statementClosed, resultClosed;
        boolean resultCloseFailure, statementCloseFailure, connectionCloseFailure, rollbackFailure, commitFailure, recordCommit;
        SQLException failure; int executions;
        JdbcAccountRepository repository() { return new JdbcAccountRepository(this::connection); }
        void closed() { assertTrue(connectionClosed); assertTrue(statementClosed); assertTrue(resultClosed); }
        Connection connection() {
            return proxy(Connection.class, (o,m,a) -> switch (m.getName()) {
                case "setAutoCommit" -> { assertEquals(false, a[0]); yield null; }
                case "prepareStatement" -> { sql = (String) a[0]; assertEquals(1, a.length); yield statement(); }
                case "commit" -> { committed = !commitFailure || recordCommit; if (commitFailure) throw diagnostic(); yield null; }
                case "rollback" -> { if (rollbackFailure) throw diagnostic(); rolledBack = true; yield null; }
                case "close" -> { connectionClosed = true; if (connectionCloseFailure) throw diagnostic(); yield null; }
                default -> throw new AssertionError(m.getName());
            });
        }
        PreparedStatement statement() {
            return proxy(PreparedStatement.class, (o,m,a) -> switch (m.getName()) {
                case "setString", "setLong" -> { parameters.put((Integer) a[0], a[1]); yield null; }
                case "setNull" -> { assertEquals(Types.VARCHAR, a[1]); parameters.put((Integer) a[0], null); yield null; }
                case "executeQuery" -> { executions++; if (failure != null) { resultClosed = true; throw failure; } yield result(); }
                case "close" -> { statementClosed = true; if (statementCloseFailure) throw diagnostic(); yield null; }
                default -> throw new AssertionError(m.getName());
            });
        }
        ResultSet result() {
            int[] position = {-1};
            return proxy(ResultSet.class, (o,m,a) -> switch (m.getName()) {
                case "next" -> ++position[0] < rows.size();
                case "getString", "getLong" -> rows.get(position[0]).get(a[0]);
                case "close" -> { resultClosed = true; if (resultCloseFailure) throw diagnostic(); yield null; }
                default -> throw new AssertionError(m.getName());
            });
        }
        SQLException diagnostic() { return conflict("sobol", "account", "uq_account_login", "23505"); }
        <T> T proxy(Class<T> type, InvocationHandler handler) { return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler)); }
    }
}
