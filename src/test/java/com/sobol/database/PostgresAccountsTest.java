package com.sobol.database;

import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PostgresAccountsTest {
    @Test void bootstrapSerializesEmptyAdminDecisionAndCreatesOnlyExplicitActiveAdmin() throws Exception {
        var s = new Script(); PostgresAccounts.bootstrap(s.connection(), "synthetic@example.invalid", "Synthetic", "opaque-test-only");
        assertEquals(List.of("begin", "lock", "read", "write", "commit"), s.events);
        assertEquals(List.of("synthetic@example.invalid", "Synthetic", "opaque-test-only"), new ArrayList<>(s.bindings.values()));
        assertTrue(s.sql.contains("'ACTIVE','ADMIN'")); assertTrue(s.closed >= 4);
    }
    @Test void anyExistingAdministratorIncludingDisabledPreventsBootstrap() {
        var s = new Script(); s.count = 1;
        safe(assertThrows(SQLException.class, () -> PostgresAccounts.bootstrap(s.connection(), "synthetic@example.invalid", "Synthetic", "opaque-test-only")));
        assertEquals(List.of("begin", "lock", "read", "rollback"), s.events); assertNull(s.sql);
    }
    @Test void approvalIsIdBoundPendingMemberOnlyAndIncrementsSecurityVersion() throws Exception {
        var s = new Script(); PostgresAccounts.approve(s.connection(), 42);
        assertEquals(42L, s.bindings.get(1));
        assertTrue(s.sql.contains("status='PENDING' AND role='MEMBER'")); assertTrue(s.sql.contains("auth_version=auth_version+1")); assertTrue(s.sql.contains("updated_at=CURRENT_TIMESTAMP"));
        assertTrue(s.events.contains("commit"));
    }
    @Test void disableCannotRemoveLastActiveAdminAndIsIdBound() throws Exception {
        var s = new Script(); PostgresAccounts.disable(s.connection(), 42);
        assertEquals(Map.of(1, 42L, 2, 42L), s.bindings);
        assertTrue(s.sql.contains("role<>'ADMIN' OR EXISTS")); assertTrue(s.sql.contains("status='ACTIVE' AND account_id<>?"));
    }
    @Test void missingIdentityInvalidIdentityAndWriteCloseOrCommitFailureRollbackWithoutLeakingDiagnostics() {
        for (int kind = 0; kind < 5; kind++) {
            var s = new Script(); if (kind == 0) s.affected = 0; if (kind == 2) s.writeFailure = true; if (kind == 3) s.closeFailure = true; if (kind == 4) s.commitFailure = true;
            long id = kind == 1 ? -1 : 42;
            safe(assertThrows(SQLException.class, () -> PostgresAccounts.approve(s.connection(), id)));
            assertTrue(s.events.contains("rollback")); if (kind != 4) assertFalse(s.events.contains("commit"));
        }
    }
    @Test void rollbackFailureRemainsGenericAndNeverRetriesCreate() {
        var s = new Script(); s.writeFailure = true; s.rollbackFailure = true;
        safe(assertThrows(SQLException.class, () -> PostgresAccounts.bootstrap(s.connection(), "synthetic@example.invalid", "Synthetic", "opaque-test-only")));
        assertEquals(1, s.events.stream().filter("write"::equals).count()); assertFalse(s.events.contains("commit"));
    }
    private static void safe(SQLException e) { assertNull(e.getCause()); assertEquals(0, e.getSuppressed().length); assertFalse(e.getMessage().contains("private diagnostic")); }
    private static final class Script {
        final List<String> events = new ArrayList<>(); final Map<Integer,Object> bindings = new LinkedHashMap<>();
        long count; int affected = 1, closed; String sql; boolean writeFailure, closeFailure, commitFailure, rollbackFailure;
        SQLException failure() { return new SQLException("private diagnostic"); }
        Connection connection() {
            return proxy(Connection.class, (o,m,a) -> switch (m.getName()) {
                case "setAutoCommit" -> { assertEquals(false, a[0]); events.add("begin"); yield null; }
                case "createStatement" -> proxy(Statement.class, (p,method,args) -> switch (method.getName()) {
                    case "execute" -> { assertEquals("LOCK TABLE sobol.account IN SHARE ROW EXCLUSIVE MODE", args[0]); events.add("lock"); yield false; }
                    case "executeQuery" -> { assertEquals("SELECT count(*) FROM sobol.account WHERE role='ADMIN'", args[0]); events.add("read"); int[] row = {0}; yield proxy(ResultSet.class, (r,rm,ra) -> switch (rm.getName()) { case "next" -> ++row[0] == 1; case "getLong" -> count; case "close" -> { closed++; yield null; } default -> throw new AssertionError(rm.getName()); }); }
                    case "close" -> { closed++; yield null; }
                    default -> throw new AssertionError(method.getName());
                });
                case "prepareStatement" -> { sql = (String)a[0]; yield proxy(PreparedStatement.class, (p,method,args) -> switch (method.getName()) {
                    case "setString", "setLong" -> { bindings.put((Integer)args[0], args[1]); yield null; }
                    case "executeUpdate" -> { events.add("write"); if (writeFailure) throw failure(); yield affected; }
                    case "close" -> { closed++; if (closeFailure) throw failure(); yield null; }
                    default -> throw new AssertionError(method.getName());
                }); }
                case "commit" -> { events.add("commit"); if (commitFailure) throw failure(); yield null; }
                case "rollback" -> { events.add("rollback"); if (rollbackFailure) throw failure(); yield null; }
                default -> throw new AssertionError(m.getName());
            });
        }
    }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) { return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, handler)); }
}
