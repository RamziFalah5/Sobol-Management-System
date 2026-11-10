package com.sobol.database;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostgresMigrationPlanTest {
    @Test void unknownOrPartialDestinationIsRejectedWithoutReset() throws Exception {
        PostgresMigrations.validateObjectNames(java.util.Set.of());
        PostgresMigrations.validateObjectNames(java.util.Set.of("category", "supplier", "customer", "flyway_schema_history",
                "category_category_id_seq", "supplier_supplier_id_seq", "customer_customer_id_seq"));
        for (var objects : java.util.List.of(java.util.Set.of("category"), java.util.Set.of("unexpected"),
                java.util.Set.of("routine:unknown"), java.util.Set.of("trigger:unknown"), java.util.Set.of("unknown_seq")))
            assertThrows(java.sql.SQLException.class, () -> PostgresMigrations.validateObjectNames(objects));
    }
    @Test void completeV2DestinationIsAcceptedButPartialAccountObjectsAreRejected() throws Exception {
        var objects = new java.util.HashSet<>(java.util.Set.of("category", "supplier", "customer", "flyway_schema_history",
                "category_category_id_seq", "supplier_supplier_id_seq", "customer_customer_id_seq"));
        objects.add("account"); assertThrows(java.sql.SQLException.class, () -> PostgresMigrations.validateObjectNames(objects));
        objects.add("account_account_id_seq"); PostgresMigrations.validateObjectNames(objects);
        objects.add("unexpected"); assertThrows(java.sql.SQLException.class, () -> PostgresMigrations.validateObjectNames(objects));
    }
    @Test void flywayPlanIsExplicitCreationDisabledAndHasNoAutomaticRepairOrClean() throws Exception {
        var config = PostgresDatabase.configuration(PostgresDatabaseTest.env(), PostgresDatabase.Purpose.MIGRATION);
        var flyway = PostgresMigrations.plan(config).getConfiguration(); // Load configuration only; no SQL/DDL.
        assertTrue(flyway.isCleanDisabled()); assertFalse(flyway.isCreateSchemas());
        assertFalse(flyway.isBaselineOnMigrate()); assertFalse(flyway.isOutOfOrder());
        assertEquals("sobol", flyway.getDefaultSchema()); assertArrayEquals(new String[]{"sobol"}, flyway.getSchemas());
        assertEquals("2", flyway.getTarget().toString()); assertEquals("classpath:db/postgresql", flyway.getLocations()[0].getDescriptor());
    }
    @Test void historicalDdlAndRunnerAreOutsideExecutableResourcesAndMySqlProfilesAreRetired() throws Exception {
        assertNull(getClass().getResource("/db/migration/V001__historical_schema.sql"));
        assertTrue(Files.exists(Path.of("docs/reference/mysql/V001__historical_schema.sql.txt")));
        String pom = Files.readString(Path.of("pom.xml"));
        assertFalse(pom.contains("mysql-connector-j")); assertFalse(pom.contains("<id>mysql-it</id>"));
        assertFalse(pom.contains("<id>db-setup</id>")); assertTrue(pom.contains("<SOBOL_PG_ENABLED>false</SOBOL_PG_ENABLED>"));
    }
    @Test void migrationContainsOnlyTheImplementedReferenceSlicesAndNoCustomerNameUniqueness() throws Exception {
        var resource = getClass().getResourceAsStream("/db/postgresql/V1__reference_data.sql");
        assertNotNull(resource);
        String sql;
        try (resource) { sql = new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); }
        assertEquals(3, sql.split("CREATE TABLE ", -1).length - 1);
        assertEquals(3, sql.split("GENERATED ALWAYS AS IDENTITY", -1).length - 1);
        assertTrue(sql.contains("uq_customer_email")); assertFalse(sql.contains("uq_customer_name"));
        for (String forbidden : new String[]{"AUTO_INCREMENT", "CREATE DATABASE", "CREATE SCHEMA", "INSERT INTO", "DROP ", "TRUNCATE "})
            assertFalse(sql.contains(forbidden), forbidden);
    }
}
