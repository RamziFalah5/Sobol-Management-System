package com.sobol.supplier.infrastructure;

import com.sobol.supplier.application.SupplierException;
import com.sobol.supplier.domain.Supplier;
import org.junit.jupiter.api.Test;
import static com.sobol.database.PostgresErrorsTest.conflict;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static com.sobol.supplier.application.SupplierException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

/** Scripted JDBC contracts, not a SQL engine or evidence of live PostgreSQL execution. */
class JdbcSupplierRepositoryTest {
    private static final String LOCK = "SELECT supplier_id FROM sobol.supplier WHERE supplier_id=? FOR UPDATE";
    private static final String UPDATE = "UPDATE sobol.supplier SET name=?, phone=?, email=?, city=?, address=?, contact_person=?, notes=? WHERE supplier_id=?";
    private static final String INSERT = "INSERT INTO sobol.supplier(name, phone, email, city, address, contact_person, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

    @Test void listsDomainValuesAndClosesReadResources() {
        var jdbc = new Script();
        jdbc.query("SELECT supplier_id, name, phone, email, city, address, contact_person, notes FROM sobol.supplier ORDER BY supplier_id DESC", List.of(row("supplier_id", 42, "name", "Synthetic", "phone", "000")));
        assertEquals(List.of(new Supplier(42, details("Synthetic"))), jdbc.repository().findAll());
        jdbc.finished(); assertEquals(1, jdbc.closedResults); assertEquals(1, jdbc.closedStatements);
    }

    @Test void insertionReturnsGeneratedIdentityOnlyAfterCommit() {
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 42)));
        assertEquals(new Supplier(42, details("Synthetic")), jdbc.repository().insert(details("Synthetic")));
        assertEquals(parameters("Synthetic", false), jdbc.bindings.getFirst());
        jdbc.finished(); assertTrue(jdbc.committed); assertFalse(jdbc.rolledBack);
    }

    @Test void missingGeneratedKeyRollsBackRatherThanReportingCreation() {
        var jdbc = new Script(); jdbc.insert(1, List.of());
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert(details("Synthetic")));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void missingTargetReturnsEmptyWithoutExecutingUpdate() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of());
        assertTrue(jdbc.repository().update(42, details("Synthetic")).isEmpty());
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        assertEquals(List.of(Map.of(1, 42)), jdbc.bindings);
    }

    @Test void zeroRowsAfterLockIsAnUnexpectedPostgresFailure() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("supplier_id", 42))); jdbc.write(UPDATE, 0);
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, details("Synthetic")));
        jdbc.finished(); assertFalse(jdbc.committed); assertTrue(jdbc.rolledBack);
        assertEquals(List.of("begin", "query", "write", "rollback", "close"), jdbc.events);
    }

    @Test void oneMatchedOrChangedRowSucceedsAndBindsTheRequestedId() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("supplier_id", 42))); jdbc.write(UPDATE, 1);
        assertEquals(42, jdbc.repository().update(42, details("Updated")).orElseThrow().id());
        assertEquals(parameters("Updated", true), jdbc.bindings.get(1));
        jdbc.finished(); assertTrue(jdbc.committed);
    }

    @Test void duplicateUpdateIsRolledBackAndTranslatedWithoutDriverDiagnostics() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("supplier_id", 42)));
        jdbc.fail(UPDATE, conflict("sobol", "supplier", "uq_supplier_name", "23505"));
        SupplierException failure = assertReason(DUPLICATE_NAME, () -> jdbc.repository().update(42, details("Duplicate")));
        assertNull(failure.getCause()); assertFalse(failure.getMessage().contains("private synthetic"));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void duplicateInsertIsNotReportedAsAnInfrastructureOutage() {
        var jdbc = new Script(); jdbc.fail(INSERT, conflict("sobol", "supplier", "uq_supplier_name", "23505"));
        assertReason(DUPLICATE_NAME, () -> jdbc.repository().insert(details("Duplicate")));
        jdbc.finished(); assertTrue(jdbc.rolledBack);
    }

    @Test void connectionFailureCannotBecomeASuccessOrAnEmptyList() {
        var repo = new JdbcSupplierRepository(() -> { throw new SQLException("private synthetic connection diagnostic", "08001", 0); });
        SupplierException failure = assertReason(STORAGE_FAILURE, repo::findAll);
        assertNull(failure.getCause()); assertFalse(failure.getMessage().contains("private synthetic"));
        assertReason(STORAGE_FAILURE, () -> repo.update(42, details("Synthetic")));
        assertReason(STORAGE_FAILURE, () -> repo.insert(details("Synthetic")));
    }

    @Test void commitFailureIsReportedAsUnconfirmedAndRollbackIsAttempted() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("supplier_id", 42))); jdbc.write(UPDATE, 1); jdbc.failCommit = true;
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, details("Synthetic")));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void rollbackFailureOverridesAConflictWithAnInfrastructureFailure() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("supplier_id", 42)));
        jdbc.fail(UPDATE, conflict("sobol", "supplier", "uq_supplier_name", "23505")); jdbc.failRollback = true;
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, details("Synthetic")));
        jdbc.finished(); assertFalse(jdbc.committed);
    }

    @Test void unexpectedUpdateCountsAndInvalidPersistedDataFailClosed() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("supplier_id", 42))); jdbc.write(UPDATE, 2);
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, details("Synthetic")));
        jdbc.finished(); assertTrue(jdbc.rolledBack);
        var corrupt = new Script();
        corrupt.query("SELECT supplier_id, name, phone, email, city, address, contact_person, notes FROM sobol.supplier ORDER BY supplier_id DESC", List.of(row("supplier_id", 0, "name", "Synthetic", "phone", "000")));
        assertReason(STORAGE_FAILURE, () -> corrupt.repository().findAll()); corrupt.finished();
    }

    private static Supplier.Details details(String name) {
        return new Supplier.Details(name, "000", null, null, null, null, null);
    }

    private static Map<Integer, Object> parameters(String name, boolean update) {
        Map<Integer, Object> result = new HashMap<>();
        result.put(1, name); result.put(2, "000");
        for (int i = 3; i <= 7; i++) result.put(i, null);
        if (update) result.put(8, 42);
        return result;
    }

    @Test void populatedOptionalFieldsMapToPreparedParameters() {
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 42)));
        var d = new Supplier.Details("Synthetic", "000", "test@example.invalid", "City", "Address", "Contact", "Notes");
        assertEquals(new Supplier(42, d), jdbc.repository().insert(d));
        assertEquals(Map.of(1, "Synthetic", 2, "000", 3, "test@example.invalid", 4, "City", 5, "Address", 6, "Contact", 7, "Notes"), jdbc.bindings.getFirst());
        jdbc.finished(); assertEquals(1, jdbc.closedResults); assertEquals(1, jdbc.closedStatements);
    }

    @Test void populatedReadMappingAndReadFailureAreExplicit() {
        var jdbc = new Script();
        jdbc.query("SELECT supplier_id, name, phone, email, city, address, contact_person, notes FROM sobol.supplier ORDER BY supplier_id DESC",
                List.of(row("supplier_id", 42, "name", "Synthetic", "phone", "000", "email", "test@example.invalid", "city", "City", "address", "Address", "contact_person", "Contact", "notes", "Notes")));
        var d = new Supplier.Details("Synthetic", "000", "test@example.invalid", "City", "Address", "Contact", "Notes");
        assertEquals(List.of(new Supplier(42, d)), jdbc.repository().findAll()); jdbc.finished();
        var failed = new Script();
        failed.fail("SELECT supplier_id, name, phone, email, city, address, contact_person, notes FROM sobol.supplier ORDER BY supplier_id DESC",
                new SQLException("private synthetic diagnostic", "08000", 0));
        assertReason(STORAGE_FAILURE, failed.repository()::findAll); failed.finished();
    }

    @Test void invalidGeneratedIdentityAndUnexpectedInsertCountsRollBack() {
        for (int count : List.of(0, 2)) {
            var jdbc = new Script(); jdbc.insert(count, List.of(row("1", 42)));
            assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert(details("Synthetic")));
            jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        }
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 0)));
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert(details("Synthetic")));
        jdbc.finished(); assertTrue(jdbc.rolledBack);
    }

    @Test void insertCommitFailureIsUnconfirmedAndAttemptsRollback() {
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 42))); jdbc.failCommit = true;
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert(details("Synthetic")));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }


    @Test void conflictWithConnectionCloseFailureDoesNotLeakSuppressedDiagnostics() {
        for (boolean update : List.of(false, true)) {
            var jdbc = new Script(); if (update) jdbc.query(LOCK, List.of(row("supplier_id", 42)));
            jdbc.fail(update ? UPDATE : INSERT, conflict("sobol", "supplier", "uq_supplier_name", "23505"));
            jdbc.failConnectionClose = true;
            var failure = assertThrows(SupplierException.class, () -> { if (update) jdbc.repository().update(42, details("Synthetic")); else jdbc.repository().insert(details("Synthetic")); });
            assertSanitized(failure); assertEquals(STORAGE_FAILURE, failure.reason());
            jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        }
    }

    @Test void rollbackAndCloseFailuresCannotRetainDriverDiagnostics() {
        var jdbc = new Script(); jdbc.fail(INSERT, conflict("sobol", "supplier", "uq_supplier_name", "23505"));
        jdbc.failRollback = true; jdbc.failConnectionClose = true;
        var failure = assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert(details("Synthetic")));
        assertSanitized(failure); jdbc.finished(); assertFalse(jdbc.committed);
    }

    @Test void readAndReadCloseErrorsRemainStorageFailures() {
        var read = new Script(); read.fail("SELECT supplier_id, name, phone, email, city, address, contact_person, notes FROM sobol.supplier ORDER BY supplier_id DESC", conflict("sobol", "supplier", "uq_supplier_name", "23505"));
        assertSanitized(assertReason(STORAGE_FAILURE, read.repository()::findAll)); read.finished();
        var close = new Script(); close.query("SELECT supplier_id, name, phone, email, city, address, contact_person, notes FROM sobol.supplier ORDER BY supplier_id DESC", List.of()); close.failConnectionClose = true;
        assertSanitized(assertReason(STORAGE_FAILURE, close.repository()::findAll)); close.finished();
    }

    @Test void closeFailureAfterCommitIsUnconfirmedRatherThanDuplicateOrSuccess() {
        for (boolean update : List.of(false, true)) {
            var jdbc = new Script(); if (update) { jdbc.query(LOCK, List.of(row("supplier_id", 42))); jdbc.write(UPDATE, 1); }
            else jdbc.insert(1, List.of(row("1", 42)));
            jdbc.failConnectionClose = true;
            var failure = assertReason(STORAGE_FAILURE, () -> { if (update) jdbc.repository().update(42, details("Synthetic")); else jdbc.repository().insert(details("Synthetic")); });
            assertSanitized(failure); jdbc.finished(); assertTrue(jdbc.committed); assertFalse(jdbc.rolledBack);
        }
    }

    @Test void commitFailureBeforeOrAfterRecordingCommitIsAlwaysUnconfirmed() {
        for (boolean update : List.of(false, true)) for (boolean committed : List.of(false, true)) {
            var jdbc = new Script(); if (update) { jdbc.query(LOCK, List.of(row("supplier_id", 42))); jdbc.write(UPDATE, 1); }
            else jdbc.insert(1, List.of(row("1", 42)));
            jdbc.commitFailure = conflict("sobol", "supplier", "uq_supplier_name", "23505");
            jdbc.recordCommitBeforeFailure = committed;
            var failure = assertReason(STORAGE_FAILURE, () -> { if (update) jdbc.repository().update(42, details("Synthetic")); else jdbc.repository().insert(details("Synthetic")); });
            assertSanitized(failure); assertTrue(failure.getMessage().contains("could not be confirmed"));
            jdbc.finished(); assertEquals(committed, jdbc.committed); assertTrue(jdbc.rolledBack);
            assertEquals(1, Collections.frequency(jdbc.events, "write")); // No automatic retry.
        }
    }

    @Test void statementCloseErrorsAreNotMisclassifiedAsWriteConflicts() {
        for (boolean update : List.of(false, true)) {
            var jdbc = new Script(); if (update) { jdbc.query(LOCK, List.of(row("supplier_id", 42))); jdbc.write(UPDATE, 1); }
            else jdbc.insert(1, List.of(row("1", 42)));
            jdbc.failStatementClose = true;
            assertSanitized(assertReason(STORAGE_FAILURE, () -> { if (update) jdbc.repository().update(42, details("Synthetic")); else jdbc.repository().insert(details("Synthetic")); }));
            jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        }
    }

    @Test void generatedKeyCloseFailureCannotConfirmCreation() {
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 42))); jdbc.failResultClose = true;
        assertSanitized(assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert(details("Synthetic"))));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void missingRowReadCloseFailureDoesNotBecomeMissingOrConflict() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of()); jdbc.failResultClose = true;
        assertSanitized(assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, details("Synthetic"))));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        assertFalse(jdbc.events.contains("write"));
    }

    @Test void onlyTheExactSupplierNameConstraintClassifiesAsDuplicate() {
        for (SQLException failure : List.of(conflict("sobol", "supplier", "unknown", "23505"),
                conflict("sobol", "customer", "uq_supplier_name", "23505"),
                conflict("other", "supplier", "uq_supplier_name", "23505"),
                conflict("sobol", "supplier", "uq_supplier_name", "23503"),
                new SQLException("uq_supplier_name", "23505"), new SQLException("synthetic", "23000", 1062))) {
            var jdbc = new Script(); jdbc.fail(INSERT, failure);
            assertSanitized(assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert(details("Synthetic"))));
            jdbc.finished(); assertTrue(jdbc.rolledBack);
        }
    }

    private void assertSanitized(SupplierException failure) {
        assertNull(failure.getCause()); assertEquals(0, failure.getSuppressed().length);
        assertFalse(failure.getMessage().contains("private synthetic"));
    }

    private SupplierException assertReason(SupplierException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(SupplierException.class, action); assertEquals(reason, failure.reason()); return failure;
    }

    private static Map<String, Object> row(Object... values) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) row.put((String) values[i], values[i + 1]);
        return row;
    }

    private record Step(String sql, List<Map<String, Object>> rows, int affected, List<Map<String, Object>> keys, SQLException failure) { }

    private static class Script {
        final Queue<Step> steps = new ArrayDeque<>();
        final List<Map<Integer, Object>> bindings = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        boolean committed, rolledBack, closed, failCommit, failRollback, transaction;
        int closedStatements, closedResults;
        boolean failConnectionClose, failStatementClose, failResultClose, recordCommitBeforeFailure;
        SQLException commitFailure;
        void query(String sql, List<Map<String, Object>> rows) { steps.add(new Step(sql, rows, 0, List.of(), null)); }
        void write(String sql, int count) { steps.add(new Step(sql, List.of(), count, List.of(), null)); }
        void insert(int count, List<Map<String, Object>> keys) { steps.add(new Step(INSERT, List.of(), count, keys, null)); }
        void fail(String sql, SQLException failure) { steps.add(new Step(sql, List.of(), 0, List.of(), failure)); }
        JdbcSupplierRepository repository() { return new JdbcSupplierRepository(this::connection); }
        void finished() { assertTrue(steps.isEmpty(), "Missing JDBC call"); assertTrue(closed); }

        Connection connection() {
            return proxy(Connection.class, (object, method, args) -> switch (method.getName()) {
                case "setAutoCommit" -> { assertEquals(false, args[0]); transaction = true; events.add("begin"); yield null; }
                case "prepareStatement" -> {
                    Step step = steps.remove(); assertEquals(step.sql(), args[0]);
                    if (step.sql().equals(INSERT)) { assertEquals(2, args.length); assertArrayEquals(new String[]{"supplier_id"}, (String[]) args[1]); }
                    else assertEquals(1, args.length);
                    yield statement(step);
                }
                case "commit" -> {
                    assertTrue(transaction); events.add("commit");
                    if (commitFailure != null) {
                        committed = recordCommitBeforeFailure; throw commitFailure;
                    }
                    if (failCommit) throw new SQLException("synthetic commit failure");
                    committed = true; yield null;
                }
                case "rollback" -> {
                    assertTrue(transaction); events.add("rollback");
                    if (failRollback) throw new SQLException("synthetic rollback failure");
                    rolledBack = true; yield null;
                }
                case "close" -> {
                    closed = true; events.add("close");
                    if (failConnectionClose) throw conflict("sobol", "supplier", "uq_supplier_name", "23505");
                    yield null;
                }
                default -> throw new AssertionError("Unexpected connection call: " + method.getName());
            });
        }

        PreparedStatement statement(Step step) {
            Map<Integer, Object> parameters = new HashMap<>();
            return proxy(PreparedStatement.class, (object, method, args) -> switch (method.getName()) {
                case "setNull" -> { assertEquals(Types.VARCHAR, args[1]); parameters.put((Integer) args[0], null); yield null; }
                case "setInt", "setString" -> { parameters.put((Integer) args[0], args[1]); yield null; }
                case "executeQuery", "executeUpdate" -> {
                    bindings.add(new HashMap<>(parameters));
                    boolean query = method.getName().equals("executeQuery"); events.add(query ? "query" : "write");
                    if (step.failure() != null) throw step.failure();
                    yield query ? result(step.rows()) : step.affected();
                }
                case "getGeneratedKeys" -> result(step.keys());
                case "close" -> {
                    closedStatements++;
                    if (failStatementClose && !step.sql().equals(LOCK)) throw conflict("sobol", "supplier", "uq_supplier_name", "23505");
                    yield null;
                }
                default -> throw new AssertionError("Unexpected statement call: " + method.getName());
            });
        }

        ResultSet result(List<Map<String, Object>> rows) {
            int[] cursor = {-1};
            return proxy(ResultSet.class, (object, method, args) -> switch (method.getName()) {
                case "next" -> ++cursor[0] < rows.size();
                case "getInt" -> ((Number) rows.get(cursor[0]).get(String.valueOf(args[0]))).intValue();
                case "getString" -> (String) rows.get(cursor[0]).get(String.valueOf(args[0]));
                case "close" -> {
                    closedResults++;
                    if (failResultClose) throw conflict("sobol", "supplier", "uq_supplier_name", "23505");
                    yield null;
                }
                default -> throw new AssertionError("Unexpected result call: " + method.getName());
            });
        }

        private <T> T proxy(Class<T> type, InvocationHandler handler) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
        }
    }
}
