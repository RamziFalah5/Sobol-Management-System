package com.sobol.database;

import com.sobol.category.application.*;
import com.sobol.category.infrastructure.JdbcCategoryRepository;
import com.sobol.supplier.application.*;
import com.sobol.supplier.infrastructure.JdbcSupplierRepository;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Approved synthetic DML only. Never creates/migrates/resets a schema. NOT run by ordinary verify. */
class PostgresPersistenceIT {
    private static PostgresDatabase.Configuration config;
    private static Connection suiteLock;
    private final Map<String, Set<Integer>> owned = new LinkedHashMap<>();
    @BeforeAll static void ready() throws Exception {
        try {
            config = PostgresDatabase.configuration(System.getenv(), PostgresDatabase.Purpose.INTEGRATION);
            suiteLock = connection();
            try (Statement p = suiteLock.createStatement(); ResultSet r = p.executeQuery("SELECT pg_try_advisory_lock(736265, 6)")) {
                if (!r.next() || !r.getBoolean(1)) throw PostgresDatabase.unavailable();
            }
            PostgresMigrations.validateDestination(suiteLock);
            PostgresMigrations.plan(config).validate(); // Read-only migration validation, no migrate call.
            try (Statement p = suiteLock.createStatement(); ResultSet r = p.executeQuery(
                    "SELECT version, success FROM sobol.flyway_schema_history WHERE type='SQL'")) {
                if (!r.next() || !"1".equals(r.getString(1)) || !r.getBoolean(2) || r.next()) throw PostgresDatabase.unavailable();
            }
        } catch (Exception failure) {
            closeLock();
            throw new IllegalStateException("PostgreSQL integration preflight stopped; review approved target, version, TLS and grants privately.");
        }
    }
    @AfterAll static void finished() throws SQLException { closeLock(); }
    private static void closeLock() throws SQLException {
        if (suiteLock != null) {
            try { suiteLock.close(); } catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
            finally { suiteLock = null; }
        }
    }
    private static Connection connection() throws SQLException { return PostgresDatabase.open(config); }
    private static String unique() { return "SYNTHETIC-" + UUID.randomUUID(); }
    private <T> T remember(String table, int id, T value) {
        if (!Set.of("category", "supplier", "customer").contains(table) || id <= 0) throw new AssertionError("Invalid fixture ownership");
        owned.computeIfAbsent(table, ignored -> new LinkedHashSet<>()).add(id); return value;
    }
    @AfterEach void cleanupOnlyOwnedIdentities() throws SQLException {
        try (Connection c = connection()) {
            for (var entry : owned.entrySet()) for (int id : entry.getValue()) {
                String column = entry.getKey() + "_id"; // Table is an internal allowlisted constant.
                try (PreparedStatement p = c.prepareStatement("DELETE FROM sobol." + entry.getKey() + " WHERE " + column + "=?")) {
                    p.setInt(1, id); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM sobol." + entry.getKey() + " WHERE " + column + "=?")) {
                    p.setInt(1, id); try (ResultSet r = p.executeQuery()) { assertFalse(r.next(), "Owned fixture cleanup did not complete"); }
                }
            }
        } catch (SQLException failure) { throw new SQLException("Owned PostgreSQL fixture cleanup failed; inspect only this approved synthetic run privately."); }
    }
    private CategoryService categories() { return new CategoryService(new JdbcCategoryRepository(PostgresPersistenceIT::connection)); }
    private com.sobol.category.domain.Category category(String name) {
        var value = categories().createCategory(name, "Synthetic description"); return remember("category", value.id(), value);
    }
    @Test void schemaAndIdentityMatchOnlyTheImplementedMasterData() throws SQLException {
        try (Connection c = connection(); Statement p = c.createStatement(); ResultSet r = p.executeQuery(
                "SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname='sobol'")) {
            Set<String> tables = new HashSet<>(); while (r.next()) tables.add(r.getString(1));
            assertEquals(Set.of("category", "supplier", "customer", "flyway_schema_history"), tables);
        } catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
    }
    @Test void categoryCreateReadUpdateAndUnchangedValuesUseTheRealRepository() {
        var created = category(unique());
        assertTrue(categories().listCategories().contains(created));
        assertEquals(created, categories().updateCategory(created.id(), created.name(), created.description()));
        var updated = categories().updateCategory(created.id(), unique(), "Updated");
        assertEquals(created.id(), updated.id()); assertTrue(categories().listCategories().contains(updated));
    }
    @Test void categoryExactDuplicateRollsBackButCaseVariantsRemainDistinct() {
        String name = unique(); var first = category(name); var second = category(name.toLowerCase(Locale.ROOT));
        assertNotEquals(first.id(), second.id());
        assertEquals(CategoryException.Reason.DUPLICATE_NAME,
                assertThrows(CategoryException.class, () -> categories().createCategory(name, "")).reason());
        assertEquals(CategoryException.Reason.DUPLICATE_NAME,
                assertThrows(CategoryException.class, () -> categories().updateCategory(second.id(), name, "Rejected")).reason());
        assertTrue(categories().listCategories().contains(second));
    }
    @Test void deletedCategoryUpdateReportsMissingWithoutCreatingAnotherRow() throws SQLException {
        var value = category(unique());
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("DELETE FROM sobol.category WHERE category_id=?")) {
            p.setInt(1, value.id()); assertEquals(1, p.executeUpdate());
        } catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
        assertEquals(CategoryException.Reason.NOT_FOUND,
                assertThrows(CategoryException.class, () -> categories().updateCategory(value.id(), unique(), "")).reason());
    }
    private SupplierService suppliers() { return new SupplierService(new JdbcSupplierRepository(PostgresPersistenceIT::connection)); }
    private com.sobol.supplier.domain.Supplier supplier(String name) {
        var value = suppliers().createSupplier(name, "000", null, null, null, null, null);
        return remember("supplier", value.id(), value);
    }
    @Test void supplierCrudUnchangedAndNullFieldsUseTheRealRepository() {
        var value = supplier(unique());
        assertTrue(suppliers().listSuppliers().contains(value)); assertNull(value.details().email());
        assertEquals(value, suppliers().updateSupplier(value.id(), value.details().name(), "000", null, null, null, null, null));
        var updated = suppliers().updateSupplier(value.id(), unique(), "111", "synthetic@example.invalid", "City", "Address", "Contact", "Notes");
        assertTrue(suppliers().listSuppliers().contains(updated)); assertEquals(value.id(), updated.id());
    }
    @Test void supplierUniqueNameRejectsConflictsWithoutAddingContactUniqueness() {
        String name = unique(); var first = supplier(name); var second = supplier(name.toLowerCase(Locale.ROOT));
        assertNotEquals(first.id(), second.id());
        assertEquals(SupplierException.Reason.DUPLICATE_NAME,
                assertThrows(SupplierException.class, () -> suppliers().createSupplier(name, "000", null, null, null, null, null)).reason());
        assertEquals(SupplierException.Reason.DUPLICATE_NAME,
                assertThrows(SupplierException.class, () -> suppliers().updateSupplier(second.id(), name, "000", null, null, null, null, null)).reason());
        assertTrue(suppliers().listSuppliers().contains(second));
        var sharedEmail = unique().toLowerCase(Locale.ROOT) + "@example.invalid";
        suppliers().updateSupplier(first.id(), name, "000", sharedEmail, null, null, null, null);
        suppliers().updateSupplier(second.id(), second.details().name(), "000", sharedEmail, null, null, null, null);
    }
    @Test void deletedSupplierUpdateReportsMissing() throws SQLException {
        var value = supplier(unique());
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("DELETE FROM sobol.supplier WHERE supplier_id=?")) {
            p.setInt(1, value.id()); assertEquals(1, p.executeUpdate());
        } catch (SQLException failure) { throw PostgresDatabase.unavailable(); }
        assertEquals(SupplierException.Reason.NOT_FOUND,
                assertThrows(SupplierException.class, () -> suppliers().updateSupplier(value.id(), unique(), "000", null, null, null, null, null)).reason());
    }
}
