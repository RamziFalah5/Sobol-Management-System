package com.sobol.customer.infrastructure;

import com.sobol.customer.application.CustomerException;
import com.sobol.customer.domain.Customer;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static com.sobol.customer.application.CustomerException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

/** Scripted JDBC contracts, not a SQL engine or evidence of live MySQL execution. */
class JdbcCustomerRepositoryTest {
    private static final String LOCK = "SELECT customer_id FROM customer WHERE customer_id=? FOR UPDATE";
    private static final String UPDATE = "UPDATE customer SET full_name=?, phone=?, email=?, city=?, address=?, notes=? WHERE customer_id=?";
    private static final String INSERT = "INSERT INTO customer(full_name, phone, email, city, address, notes) VALUES (?, ?, ?, ?, ?, ?)";

    @Test void listsDomainValuesAndClosesReadResources() {
        var jdbc = new Script();
        jdbc.query("SELECT customer_id, full_name, phone, email, city, address, notes FROM customer ORDER BY customer_id DESC", List.of(row("customer_id", 42, "full_name", "Synthetic", "phone", "000")));
        assertEquals(List.of(new Customer(42, details("Synthetic"))), jdbc.repository().findAll());
        jdbc.finished(); assertEquals(1, jdbc.closedResults); assertEquals(1, jdbc.closedStatements);
    }

    @Test void insertionReturnsGeneratedIdentityOnlyAfterCommit() {
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 42)));
        assertEquals(new Customer(42, details("Synthetic")), jdbc.repository().insert(details("Synthetic")));
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

    @Test void zeroChangedRowsOnALockedExistingTargetStillSucceeds() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("customer_id", 42))); jdbc.write(UPDATE, 0);
        assertEquals(Optional.of(new Customer(42, details("Synthetic"))), jdbc.repository().update(42, details("Synthetic")));
        jdbc.finished(); assertTrue(jdbc.committed); assertFalse(jdbc.rolledBack);
        assertEquals(List.of("begin", "query", "write", "commit", "close"), jdbc.events);
    }

    @Test void oneMatchedOrChangedRowSucceedsAndBindsTheRequestedId() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("customer_id", 42))); jdbc.write(UPDATE, 1);
        assertEquals(42, jdbc.repository().update(42, details("Updated")).orElseThrow().id());
        assertEquals(parameters("Updated", true), jdbc.bindings.get(1));
        jdbc.finished(); assertTrue(jdbc.committed);
    }

    @Test void duplicateUpdateIsRolledBackAndTranslatedWithoutDriverDiagnostics() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("customer_id", 42)));
        jdbc.fail(UPDATE, new SQLException("Duplicate entry 'private synthetic diagnostic' for key 'customer.uq_customer_name'", "23000", 1062));
        CustomerException failure = assertReason(DUPLICATE_NAME, () -> jdbc.repository().update(42, details("Duplicate")));
        assertNull(failure.getCause()); assertFalse(failure.getMessage().contains("private synthetic"));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void duplicateInsertIsNotReportedAsAnInfrastructureOutage() {
        var jdbc = new Script(); jdbc.fail(INSERT, new SQLException("Duplicate entry 'synthetic conflict' for key 'customer.uq_customer_name'", "23000", 1062));
        assertReason(DUPLICATE_NAME, () -> jdbc.repository().insert(details("Duplicate")));
        jdbc.finished(); assertTrue(jdbc.rolledBack);
    }

    @Test void connectionFailureCannotBecomeASuccessOrAnEmptyList() {
        var repo = new JdbcCustomerRepository(() -> { throw new SQLException("private synthetic connection diagnostic", "08001", 0); });
        CustomerException failure = assertReason(STORAGE_FAILURE, repo::findAll);
        assertNull(failure.getCause()); assertFalse(failure.getMessage().contains("private synthetic"));
        assertReason(STORAGE_FAILURE, () -> repo.update(42, details("Synthetic")));
        assertReason(STORAGE_FAILURE, () -> repo.insert(details("Synthetic")));
    }

    @Test void commitFailureIsReportedAsUnconfirmedAndRollbackIsAttempted() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("customer_id", 42))); jdbc.write(UPDATE, 1); jdbc.failCommit = true;
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, details("Synthetic")));
        jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
    }

    @Test void rollbackFailureOverridesAConflictWithAnInfrastructureFailure() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("customer_id", 42)));
        jdbc.fail(UPDATE, new SQLException("Duplicate entry 'synthetic conflict' for key 'customer.uq_customer_name'", "23000", 1062)); jdbc.failRollback = true;
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, details("Synthetic")));
        jdbc.finished(); assertFalse(jdbc.committed);
    }

    @Test void unexpectedUpdateCountsAndInvalidPersistedDataFailClosed() {
        var jdbc = new Script(); jdbc.query(LOCK, List.of(row("customer_id", 42))); jdbc.write(UPDATE, 2);
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().update(42, details("Synthetic")));
        jdbc.finished(); assertTrue(jdbc.rolledBack);
        var corrupt = new Script();
        corrupt.query("SELECT customer_id, full_name, phone, email, city, address, notes FROM customer ORDER BY customer_id DESC", List.of(row("customer_id", 0, "full_name", "Synthetic", "phone", "000")));
        assertReason(STORAGE_FAILURE, () -> corrupt.repository().findAll()); corrupt.finished();
    }

    private static Customer.Details details(String name) {
        return new Customer.Details(name, "000", null, null, null, null);
    }

    private static Map<Integer, Object> parameters(String name, boolean update) {
        Map<Integer, Object> result = new HashMap<>();
        result.put(1, name); result.put(2, "000");
        for (int i = 3; i <= 6; i++) result.put(i, null);
        if (update) result.put(7, 42);
        return result;
    }

    @Test void populatedOptionalFieldsMapToPreparedParameters() {
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 42)));
        var d = new Customer.Details("Synthetic", "000", "test@example.invalid", "City", "Address", "Notes");
        assertEquals(new Customer(42, d), jdbc.repository().insert(d));
        assertEquals(Map.of(1, "Synthetic", 2, "000", 3, "test@example.invalid", 4, "City", 5, "Address", 6, "Notes"), jdbc.bindings.getFirst());
        jdbc.finished(); assertEquals(1, jdbc.closedResults); assertEquals(1, jdbc.closedStatements);
    }

    @Test void populatedReadMappingAndReadFailureAreExplicit() {
        var jdbc = new Script();
        jdbc.query("SELECT customer_id, full_name, phone, email, city, address, notes FROM customer ORDER BY customer_id DESC",
                List.of(row("customer_id", 42, "full_name", "Synthetic", "phone", "000", "email", "test@example.invalid", "city", "City", "address", "Address", "notes", "Notes")));
        var d = new Customer.Details("Synthetic", "000", "test@example.invalid", "City", "Address", "Notes");
        assertEquals(List.of(new Customer(42, d)), jdbc.repository().findAll()); jdbc.finished();
        var failed = new Script();
        failed.fail("SELECT customer_id, full_name, phone, email, city, address, notes FROM customer ORDER BY customer_id DESC",
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


    @Test void duplicateEmailInsertAndUpdateAreNotNameConflicts() {
        for (boolean update : List.of(false, true)) {
            var jdbc = new Script();
            if (update) jdbc.query(LOCK, List.of(row("customer_id", 42)));
            jdbc.fail(update ? UPDATE : INSERT, new SQLException("Duplicate entry 'synthetic@example.invalid' for key 'customer.uq_customer_email'", "23000", 1062));
            var failure = assertReason(DUPLICATE_EMAIL, () -> {
                if (update) jdbc.repository().update(42, details("Synthetic"));
                else jdbc.repository().insert(details("Synthetic"));
            });
            assertNull(failure.getCause()); assertFalse(failure.getMessage().contains("synthetic@example.invalid"));
            jdbc.finished(); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed);
        }
    }

    @Test void unqualifiedAndQualifiedKnownConstraintNamesAreRecognized() {
        for (String key : List.of("uq_customer_email", "customer.uq_customer_email", "sobol_dev.customer.uq_customer_email")) {
            var jdbc = new Script(); jdbc.fail(INSERT, new SQLException("Duplicate entry 'synthetic' for key '" + key + "'", "23000", 1062));
            assertReason(DUPLICATE_EMAIL, () -> jdbc.repository().insert(details("Synthetic"))); jdbc.finished();
        }
        var jdbc = new Script(); jdbc.fail(INSERT, new SQLException("Duplicate entry 'synthetic' for key `uq_customer_name`", "23000", 1062));
        assertReason(DUPLICATE_NAME, () -> jdbc.repository().insert(details("Synthetic"))); jdbc.finished();
    }

    @Test void unknownAndMisleadingDiagnosticsNeverGuessANameOrEmailConflict() {
        for (String diagnostic : Arrays.asList(null, "synthetic conflict", "Duplicate entry 'uq_customer_email' for key 'PRIMARY'",
                "Duplicate entry 'for key \\'uq_customer_name\\'' for key 'customer.unknown'",
                "Duplicate entry 'for key \\'uq_customer_email\\'' for key 'other_table.uq_customer_name'",
                "Duplicate entry 'x' for key 'uq_customer_email_extra'")) {
            var jdbc = new Script(); jdbc.fail(INSERT, new SQLException(diagnostic, "23000", 1062));
            var failure = assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert(details("Synthetic")));
            assertNull(failure.getCause()); jdbc.finished(); assertTrue(jdbc.rolledBack);
        }
    }

    @Test void knownKeyInDuplicateValueCannotOverrideTheActualTrailingKey() {
        var jdbc = new Script();
        jdbc.fail(INSERT, new SQLException("Duplicate entry 'synthetic uq_customer_email for key \\'uq_customer_email\\'' for key 'customer.uq_customer_name'", "23000", 1062));
        assertReason(DUPLICATE_NAME, () -> jdbc.repository().insert(details("Synthetic"))); jdbc.finished();
    }

    @Test void nonDuplicateSqlCodeWithKnownKeyStillMeansStorageFailure() {
        var jdbc = new Script(); jdbc.fail(INSERT, new SQLException("for key 'uq_customer_email'", "HY000", 1205));
        assertReason(STORAGE_FAILURE, () -> jdbc.repository().insert(details("Synthetic"))); jdbc.finished(); assertTrue(jdbc.rolledBack);
    }

    @Test void readAndCommitErrorsAreNotInterpretedAsWriteUniquenessConflicts() {
        var read = new Script();
        read.fail("SELECT customer_id, full_name, phone, email, city, address, notes FROM customer ORDER BY customer_id DESC",
                new SQLException("for key 'uq_customer_email'", "23000", 1062));
        assertReason(STORAGE_FAILURE, read.repository()::findAll); read.finished();
        var commit = new Script(); commit.insert(1, List.of(row("1", 42)));
        commit.commitFailure = new SQLException("for key 'uq_customer_email'", "23000", 1062);
        assertReason(STORAGE_FAILURE, () -> commit.repository().insert(details("Synthetic")));
        commit.finished(); assertTrue(commit.rolledBack);
    }

    @Test void optionalEmptyStringsAreNotChangedIntoSqlNull() {
        var jdbc = new Script(); jdbc.insert(1, List.of(row("1", 42)));
        var d = new Customer.Details("Synthetic", "000", "", "", "", "");
        jdbc.repository().insert(d);
        var expected = parameters("Synthetic", false);
        expected.put(4, ""); expected.put(5, ""); expected.put(6, "");
        assertEquals(expected, jdbc.bindings.getFirst()); jdbc.finished();
    }

    private CustomerException assertReason(CustomerException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(CustomerException.class, action); assertEquals(reason, failure.reason()); return failure;
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
        SQLException commitFailure;
        void query(String sql, List<Map<String, Object>> rows) { steps.add(new Step(sql, rows, 0, List.of(), null)); }
        void write(String sql, int count) { steps.add(new Step(sql, List.of(), count, List.of(), null)); }
        void insert(int count, List<Map<String, Object>> keys) { steps.add(new Step(INSERT, List.of(), count, keys, null)); }
        void fail(String sql, SQLException failure) { steps.add(new Step(sql, List.of(), 0, List.of(), failure)); }
        JdbcCustomerRepository repository() { return new JdbcCustomerRepository(this::connection); }
        void finished() { assertTrue(steps.isEmpty(), "Missing JDBC call"); assertTrue(closed); }

        Connection connection() {
            return proxy(Connection.class, (object, method, args) -> switch (method.getName()) {
                case "setAutoCommit" -> { assertEquals(false, args[0]); transaction = true; events.add("begin"); yield null; }
                case "prepareStatement" -> {
                    Step step = steps.remove(); assertEquals(step.sql(), args[0]);
                    if (step.sql().equals(INSERT)) { assertEquals(2, args.length); assertEquals(Statement.RETURN_GENERATED_KEYS, args[1]); }
                    else assertEquals(1, args.length);
                    yield statement(step);
                }
                case "commit" -> {
                    assertTrue(transaction); events.add("commit");
                    if (commitFailure != null) throw commitFailure;
                    if (failCommit) throw new SQLException("synthetic commit failure");
                    committed = true; yield null;
                }
                case "rollback" -> {
                    assertTrue(transaction); events.add("rollback");
                    if (failRollback) throw new SQLException("synthetic rollback failure");
                    rolledBack = true; yield null;
                }
                case "close" -> { closed = true; events.add("close"); yield null; }
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
                case "close" -> { closedStatements++; yield null; }
                default -> throw new AssertionError("Unexpected statement call: " + method.getName());
            });
        }

        ResultSet result(List<Map<String, Object>> rows) {
            int[] cursor = {-1};
            return proxy(ResultSet.class, (object, method, args) -> switch (method.getName()) {
                case "next" -> ++cursor[0] < rows.size();
                case "getInt" -> ((Number) rows.get(cursor[0]).get(String.valueOf(args[0]))).intValue();
                case "getString" -> (String) rows.get(cursor[0]).get(String.valueOf(args[0]));
                case "close" -> { closedResults++; yield null; }
                default -> throw new AssertionError("Unexpected result call: " + method.getName());
            });
        }

        private <T> T proxy(Class<T> type, InvocationHandler handler) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
        }
    }
}
