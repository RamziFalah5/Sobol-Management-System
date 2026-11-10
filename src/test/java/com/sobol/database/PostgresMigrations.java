package com.sobol.database;

import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/** Thin, explicit Flyway entry point. No database/schema/account creation, repair or clean command. */
public final class PostgresMigrations {
    private PostgresMigrations() { }
    static Flyway plan(PostgresDatabase.Configuration config) {
        return Flyway.configure().dataSource(new GuardedSource(config))
                .locations("classpath:db/postgresql").schemas("sobol").defaultSchema("sobol")
                .createSchemas(false).cleanDisabled(true).baselineOnMigrate(false).outOfOrder(false)
                .validateMigrationNaming(true).validateOnMigrate(true).target("2")
                .loggers(SafeLogCreator.class.getName()).load();
    }
    static void validateDestination(Connection c) throws SQLException {
        Set<String> objects = new HashSet<>();
        try (Statement p = c.createStatement(); ResultSet r = p.executeQuery("""
                SELECT c.relname FROM pg_catalog.pg_class c
                JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
                WHERE n.nspname='sobol' AND c.relkind IN ('r','p','v','m','f','S')
                UNION ALL
                SELECT 'routine:' || p.proname FROM pg_catalog.pg_proc p
                JOIN pg_catalog.pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='sobol'
                UNION ALL
                SELECT 'trigger:' || t.tgname FROM pg_catalog.pg_trigger t
                JOIN pg_catalog.pg_class c ON c.oid=t.tgrelid
                JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
                WHERE n.nspname='sobol' AND NOT t.tgisinternal
                """)) {
            while (r.next()) objects.add(r.getString(1));
        }
        validateObjectNames(objects);
    }
    static void validateObjectNames(Set<String> objects) throws SQLException {
        Set<String> v1 = Set.of("category", "supplier", "customer", "flyway_schema_history",
                "category_category_id_seq", "supplier_supplier_id_seq", "customer_customer_id_seq");
        Set<String> v2 = new HashSet<>(v1); v2.add("account"); v2.add("account_account_id_seq");
        if (!objects.isEmpty() && !objects.equals(v1) && !objects.equals(v2)) throw PostgresDatabase.unavailable();
    }
    public static void main(String[] args) {
        try {
            var config = PostgresDatabase.configuration(System.getenv(), PostgresDatabase.Purpose.MIGRATION);
            try (Connection c = PostgresDatabase.open(config)) { validateDestination(c); }
            plan(config).migrate();
            System.out.println("Verified PostgreSQL sobol_dev/sobol migrations through V2. No historical records imported.");
        } catch (Exception failure) {
            throw new IllegalStateException("PostgreSQL migration stopped. Review approvals, TLS, dedicated privileges and target state privately; no automatic reset or repair is permitted.");
        }
    }
    /** Flyway supports a LogCreator class name. Raw driver messages must not reach Maven logs. */
    public static final class SafeLogCreator implements org.flywaydb.core.api.logging.LogCreator {
        public SafeLogCreator() { }
        @Override public org.flywaydb.core.api.logging.Log createLogger(Class<?> type) {
            return new org.flywaydb.core.api.logging.Log() {
                public void debug(String message) { }
                public void info(String message) { }
                public void warn(String message) { }
                public void error(String message) { }
                public void error(String message, Exception failure) { }
                public void notice(String message) { }
            };
        }
    }
    private record GuardedSource(PostgresDatabase.Configuration config) implements DataSource {
        @Override public Connection getConnection() throws SQLException { return PostgresDatabase.open(config); }
        @Override public Connection getConnection(String user, String secret) throws SQLException { throw PostgresDatabase.unavailable(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter writer) { throw new UnsupportedOperationException(); }
        @Override public void setLoginTimeout(int seconds) { throw new UnsupportedOperationException(); }
        @Override public int getLoginTimeout() { return 5; }
        @Override public Logger getParentLogger() { return Logger.getLogger("sobol-migrations"); }
        @Override public <T> T unwrap(Class<T> type) throws SQLException { throw PostgresDatabase.unavailable(); }
        @Override public boolean isWrapperFor(Class<?> type) { return false; }
        @Override public String toString() { return "Guarded PostgreSQL migration source"; }
    }
}
