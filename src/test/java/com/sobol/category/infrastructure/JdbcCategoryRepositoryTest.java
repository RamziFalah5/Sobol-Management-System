package com.sobol.category.infrastructure;

import com.sobol.category.application.CategoryException;
import com.sobol.category.domain.Category;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static com.sobol.category.application.CategoryException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

/** Scripted JDBC contracts, not a SQL engine or evidence of live MySQL execution. */
class JdbcCategoryRepositoryTest {
    private static final String LOCK = "SELECT category_id FROM category WHERE category_id=? FOR UPDATE";
    private static final String UPDATE = "UPDATE category SET name=?, description=? WHERE category_id=?";
    private static final String INSERT = "INSERT INTO category(name, description) VALUES (?, ?)";

    @Test void listsDomainValuesAndClosesReadResources() {
        var jdbc = new Script();
        jdbc.query("SELECT category_id, name, description FROM category", List.of(row("category_id", 42, "name", "Synthetic", "description", null)));
        assertEquals(List.of(new Category(42, "Synthetic", "")), jdbc.repository().findAll());
        jdbc.finished(); assertEquals(1, jdbc.closedResults); assertEquals(1, jdbc.closedStatements);
    }

    @Test void insertionReturnsGeneratedIdentityOnlyAfterCommit() {
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 42)));
        assertEquals(new Category(42, "Synthetic", "Description"), jdbc.repository().insert("Synthetic", "Description"));
        assertEquals(Map.of(1, "Synthetic", 2, "Description"), jdbc.bindings.getFirst());
        jdbc.finished(); assertTrue(jdbc.committed); assertFalse(jdbc.rolledBack);
    }

    @Test void missingGeneratedKeyRollsBackRatherThanReportingCreation() {
        var jdbc = new Script(); jdbc.insert(1, List.of());
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert("Synthetic", ""));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void missingTargetReturnsEmptyWithoutExecutingUpdate() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of());
        assertTrue(jdbc.repository().update(42, "Synthetic", "").isEmpty());
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        assertEquals(List.of(Map.of(1, 42)), jdbc.bindings);
    }

    @Test void zeroChangedRowsOnALockedExistingTargetStillSucceeds() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("category_id", 42))); jdbc.write(UPDATE, 0);
        assertEquals(Optional.of(new Category(42, "Synthetic", "Description")), jdbc.repository().update(42, "Synthetic", "Description"));
        jdbc.finished(); assertTrue(jdbc.committed); assertFalse(jdbc.rolledBack);
        assertEquals(List.of("begin", "query", "write", "commit", "close"), jdbc.events);
    }

    @Test void oneMatchedOrChangedRowSucceedsAndBindsTheRequestedId() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("category_id", 42))); jdbc.write(UPDATE, 1);
        assertEquals(42, jdbc.repository().update(42, "Updated", "Description").orElseThrow().id());
        assertEquals(Map.of(1, "Updated", 2, "Description", 3, 42), jdbc.bindings.get(1));
        jdbc.finished(); assertTrue(jdbc.committed);
    }

    @Test void duplicateUpdateIsRolledBackAndTranslatedWithoutDriverDiagnostics() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("category_id", 42)));
        jdbc.fail(UPDATE, new SQLException("private synthetic diagnostic", "23000", 1062));
        CategoryException failure = assertReason(DUPLICATE_NAME, () -> jdbc.repository().update(42, "Duplicate", ""));
        assertNull(failure.getCause()); assertFalse(failure.getMessage().contains("private synthetic"));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void duplicateInsertIsNotReportedAsAnInfrastructureOutage() {
        var jdbc = new Script(); jdbc.fail(INSERT, new SQLException("synthetic conflict", "23000", 1062));
        assertReason(DUPLICATE_NAME, () -> jdbc.repository().insert("Duplicate", ""));
        jdbc.finished(); assertTrue(jdbc.rolledBack);
    }

    @Test void connectionFailureCannotBecomeASuccessOrAnEmptyList() {
        var repo = new JdbcCategoryRepository(() -> { throw new SQLException("private synthetic connection diagnostic", "08001", 0); });
        CategoryException failure = assertReason(STORAGE_FAILURE, repo::findAll);
        assertNull(failure.getCause()); assertFalse(failure.getMessage().contains("private synthetic"));
        assertReason(STORAGE_FAILURE, () -> repo.update(42, "Synthetic", ""));
        assertReason(STORAGE_FAILURE, () -> repo.insert("Synthetic", ""));
    }

    @Test void commitFailureIsReportedAsUnconfirmedAndRollbackIsAttempted() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("category_id", 42))); jdbc.write(UPDATE, 1); jdbc.failCommit = true;
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, "Synthetic", ""));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void rollbackFailureOverridesAConflictWithAnInfrastructureFailure() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("category_id", 42)));
        jdbc.fail(UPDATE, new SQLException("synthetic conflict", "23000", 1062)); jdbc.failRollback = true;
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, "Synthetic", ""));
        jdbc.finished(); assertFalse(jdbc.committed);
    }

    @Test void unexpectedUpdateCountsAndInvalidPersistedDataFailClosed() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("category_id", 42))); jdbc.write(UPDATE, 2);
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, "Synthetic", ""));
        jdbc.finished(); assertTrue(jdbc.rolledBack);
        var corrupt = new Script();
        corrupt.query("SELECT category_id, name, description FROM category", List.of(row("category_id", 0, "name", "Synthetic", "description", "")));
        assertReason(STORAGE_FAILURE, () -> corrupt.repository().findAll()); corrupt.finished();
    }


    @Test void conflictWithConnectionCloseFailureDoesNotLeakSuppressedDiagnostics() {
        for (boolean update : List.of(false, true)) {
            var jdbc = new Script(); if (update) jdbc.query(LOCK, List.of(row("category_id", 42)));
            jdbc.fail(update ? UPDATE : INSERT, new SQLException("private synthetic duplicate", "23000", 1062));
            jdbc.failConnectionClose = true;
            var failure = assertThrows(CategoryException.class, () -> { if (update) jdbc.repository().update(42, "Synthetic", ""); else jdbc.repository().insert("Synthetic", ""); });
            assertSanitized(failure); assertEquals(STORAGE_FAILURE, failure.reason());
            jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        }
    }

    @Test void rollbackAndCloseFailuresCannotRetainDriverDiagnostics() {
        var jdbc = new Script(); jdbc.fail(INSERT, new SQLException("private synthetic duplicate", "23000", 1062));
        jdbc.failRollback = true; jdbc.failConnectionClose = true;
        var failure = assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert("Synthetic", ""));
        assertSanitized(failure); jdbc.finished(); assertFalse(jdbc.committed);
    }

    @Test void readAndReadCloseErrorsRemainStorageFailures() {
        var read = new Script(); read.fail("SELECT category_id, name, description FROM category", new SQLException("private synthetic read", "23000", 1062));
        assertSanitized(assertReason(STORAGE_FAILURE, read.repository()::findAll)); read.finished();
        var close = new Script(); close.query("SELECT category_id, name, description FROM category", List.of()); close.failConnectionClose = true;
        assertSanitized(assertReason(STORAGE_FAILURE, close.repository()::findAll)); close.finished();
    }

    @Test void closeFailureAfterCommitIsUnconfirmedRatherThanDuplicateOrSuccess() {
        for (boolean update : List.of(false, true)) {
            var jdbc = new Script(); if (update) { jdbc.query(LOCK, List.of(row("category_id", 42))); jdbc.write(UPDATE, 1); }
            else jdbc.insert(1, List.of(row("1", 42)));
            jdbc.failConnectionClose = true;
            var failure = assertReason(STORAGE_FAILURE, () -> { if (update) jdbc.repository().update(42, "Synthetic", ""); else jdbc.repository().insert("Synthetic", ""); });
            assertSanitized(failure); jdbc.finished(); assertTrue(jdbc.committed); assertFalse(jdbc.rolledBack);
        }
    }

    @Test void commitFailureBeforeOrAfterRecordingCommitIsAlwaysUnconfirmed() {
        for (boolean update : List.of(false, true)) for (boolean committed : List.of(false, true)) {
            var jdbc = new Script(); if (update) { jdbc.query(LOCK, List.of(row("category_id", 42))); jdbc.write(UPDATE, 1); }
            else jdbc.insert(1, List.of(row("1", 42)));
            jdbc.commitFailure = new SQLException("private synthetic lost commit response", "23000", 1062);
            jdbc.recordCommitBeforeFailure = committed;
            var failure = assertReason(STORAGE_FAILURE, () -> { if (update) jdbc.repository().update(42, "Synthetic", ""); else jdbc.repository().insert("Synthetic", ""); });
            assertSanitized(failure); assertTrue(failure.getMessage().contains("could not be confirmed"));
            jdbc.finished(); assertEquals(committed, jdbc.committed); assertTrue(jdbc.rolledBack);
            assertEquals(1, Collections.frequency(jdbc.events, "write")); // No automatic retry.
        }
    }

    @Test void statementCloseErrorsAreNotMisclassifiedAsWriteConflicts() {
        for (boolean update : List.of(false, true)) {
            var jdbc = new Script(); if (update) { jdbc.query(LOCK, List.of(row("category_id", 42))); jdbc.write(UPDATE, 1); }
            else jdbc.insert(1, List.of(row("1", 42)));
            jdbc.failStatementClose = true;
            assertSanitized(assertReason(STORAGE_FAILURE, () -> { if (update) jdbc.repository().update(42, "Synthetic", ""); else jdbc.repository().insert("Synthetic", ""); }));
            jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        }
    }

    @Test void generatedKeyCloseFailureCannotConfirmCreation() {
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 42))); jdbc.failResultClose = true;
        assertSanitized(assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert("Synthetic", "")));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void missingRowReadCloseFailureDoesNotBecomeMissingOrConflict() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of()); jdbc.failResultClose = true;
        assertSanitized(assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, "Synthetic", "")));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        assertFalse(jdbc.events.contains("write"));
    }

    private void assertSanitized(CategoryException failure) {
        assertNull(failure.getCause()); assertEquals(0, failure.getSuppressed().length);
        assertFalse(failure.getMessage().contains("private synthetic"));
    }

    private CategoryException assertReason(CategoryException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(CategoryException.class, action); assertEquals(reason, failure.reason()); return failure;
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
        JdbcCategoryRepository repository() { return new JdbcCategoryRepository(this::connection); }
        void finished() { assertTrue(steps.isEmpty(), "Missing JDBC call"); assertTrue(closed); }

        Connection connection() {
            return proxy(Connection.class, (object, method, args) -> switch (method.getName()) {
                case "setAutoCommit" -> { assertEquals(false, args[0]); transaction = true; events.add("begin"); yield null; }
                case "prepareStatement" -> {
                    Step step = steps.remove(); assertEquals(step.sql(), args[0]); yield statement(step);
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
                    if (failConnectionClose) throw new SQLException("private synthetic connection-close", "23000", 1062);
                    yield null;
                }
                default -> throw new AssertionError("Unexpected connection call: " + method.getName());
            });
        }

        PreparedStatement statement(Step step) {
            Map<Integer, Object> parameters = new HashMap<>();
            return proxy(PreparedStatement.class, (object, method, args) -> switch (method.getName()) {
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
                    if (failStatementClose && !step.sql().equals(LOCK)) throw new SQLException("private synthetic statement-close", "23000", 1062);
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
                    if (failResultClose) throw new SQLException("private synthetic result-close", "23000", 1062);
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
