package com.sobol.database;

import java.net.URI;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Explicit, local-only development boundary. Never creates databases, schemas or accounts. */
public final class PostgresDatabase {
    private PostgresDatabase() { }
    enum Purpose { APPLICATION, MIGRATION, INTEGRATION }

    public static Connection get() throws SQLException {
        return open(configuration(System.getenv(), Purpose.APPLICATION));
    }

    static Configuration configuration(Map<String, String> env, Purpose purpose) throws SQLException {
        if (!"true".equals(env.get("SOBOL_PG_ENABLED"))) throw unavailable();
        String url = required(env, "SOBOL_PG_URL");
        try {
            if (!url.startsWith("jdbc:postgresql://")) throw unavailable();
            URI uri = URI.create(url.substring(5));
            if (!"postgresql".equals(uri.getScheme()) || !"127.0.0.1".equals(uri.getHost())
                    || !uri.getRawAuthority().equals("127.0.0.1:" + uri.getPort())
                    || uri.getPort() < 1 || uri.getPort() > 65535
                    || !"/sobol_dev".equals(uri.getRawPath()) || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || uri.getRawUserInfo() != null) throw unavailable();
        } catch (IllegalArgumentException failure) { throw unavailable(); }
        String appUser = required(env, "SOBOL_PG_USER");
        String appSecret = required(env, "SOBOL_PG_PASSWORD");
        if (!"sobol_app".equals(appUser)) throw unavailable();
        String certificate = required(env, "SOBOL_PG_SSLROOTCERT");
        try { if (!Path.of(certificate).isAbsolute()) throw unavailable(); }
        catch (InvalidPathException failure) { throw unavailable(); }
        if (purpose == Purpose.INTEGRATION && !"sobol_dev/sobol".equals(env.get("SOBOL_PG_IT_APPROVED")))
            throw unavailable();
        String user = appUser, secret = appSecret;
        if (purpose == Purpose.MIGRATION) {
            if (!"sobol_dev/sobol".equals(env.get("SOBOL_PG_MIGRATION_APPROVED"))) throw unavailable();
            user = required(env, "SOBOL_PG_MIGRATION_USER");
            secret = required(env, "SOBOL_PG_MIGRATION_PASSWORD");
            if (!"sobol_migrator".equals(user) || secret.equals(appSecret)) throw unavailable();
        }
        return new Configuration(url, user, secret, certificate, purpose);
    }

    // No generated toString or public credential accessors.
    static final class Configuration {
        final String url, user, secret, certificate;
        final Purpose purpose;
        Configuration(String url, String user, String secret, String certificate, Purpose purpose) {
            this.url = url; this.user = user; this.secret = secret; this.certificate = certificate; this.purpose = purpose;
        }
        Properties properties() {
            Properties p = new Properties();
            p.setProperty("user", user); p.setProperty("password", secret);
            p.setProperty("sslmode", "verify-full"); p.setProperty("sslrootcert", certificate);
            p.setProperty("currentSchema", "sobol"); p.setProperty("ApplicationName", "sobol-development");
            p.setProperty("connectTimeout", "5"); p.setProperty("socketTimeout", "15");
            p.setProperty("options", "-c statement_timeout=10000 -c lock_timeout=5000 -c timezone=UTC");
            return p;
        }
    }

    @FunctionalInterface interface Connector { Connection connect(String url, Properties properties) throws SQLException; }
    static Connection open(Configuration config) throws SQLException {
        return open(config, (url, properties) -> new org.postgresql.Driver().connect(url, properties));
    }
    static Connection open(Configuration config, Connector connector) throws SQLException {
        Connection c = null;
        try {
            if (!Files.isRegularFile(Path.of(config.certificate))) throw unavailable();
            c = connector.connect(config.url, config.properties());
            if (c == null) throw unavailable();
            verify(c, config);
            return c;
        } catch (SQLException | RuntimeException failure) {
            if (c != null) try { c.close(); } catch (SQLException ignored) { }
            throw unavailable(); // Never retain driver causes, suppressed errors, URLs or identities.
        }
    }

    static void verify(Connection c, Configuration config) throws SQLException {
        String sql = """
                SELECT current_database(), current_schema(), current_user, session_user,
                       inet_server_addr()::text,
                       r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls,
                       has_database_privilege(current_user, current_database(), 'CREATE'),
                       has_schema_privilege(current_user, 'sobol', 'CREATE'),
                       EXISTS (SELECT 1 FROM pg_catalog.pg_auth_members WHERE member = r.oid),
                       (SELECT ssl FROM pg_catalog.pg_stat_ssl WHERE pid = pg_backend_pid()),
                       has_database_privilege(current_user, current_database(), 'TEMP'),
                       EXISTS (
                           SELECT 1 FROM pg_catalog.pg_namespace n
                           WHERE n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'
                             AND n.nspname <> 'sobol' AND has_schema_privilege(current_user, n.oid, 'CREATE')
                       ),
                       EXISTS (
                           SELECT 1 FROM pg_catalog.pg_class o
                           JOIN pg_catalog.pg_namespace n ON n.oid=o.relnamespace
                           WHERE n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'
                             AND ((current_user='sobol_app' AND o.relowner=r.oid)
                               OR (n.nspname <> 'sobol' AND CASE WHEN o.relkind IN ('r','p','v','m','f')
                                 THEN has_table_privilege(current_user, o.oid, 'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER') ELSE false END)
                               OR (current_user='sobol_app' AND n.nspname='sobol' AND CASE WHEN o.relkind IN ('r','p','v','m','f')
                                 THEN has_table_privilege(current_user, o.oid, 'TRUNCATE,REFERENCES,TRIGGER') ELSE false END))
                       )
                FROM pg_catalog.pg_roles r WHERE r.rolname = current_user
                """;
        try (Statement p = c.createStatement(); ResultSet r = p.executeQuery(sql)) {
            if (!r.next() || !"sobol_dev".equals(r.getString(1)) || !"sobol".equals(r.getString(2))
                    || !config.user.equals(r.getString(3)) || !config.user.equals(r.getString(4))
                    || !"127.0.0.1/32".equals(r.getString(5)) && !"127.0.0.1".equals(r.getString(5))
                    || r.getBoolean(6) || r.getBoolean(7) || r.getBoolean(9) || !r.getBoolean(10)
                    || r.getBoolean(11) || r.getBoolean(12) || r.getBoolean(13)
                    || r.getBoolean(8) != (config.purpose == Purpose.MIGRATION) || r.next()) throw unavailable();
        }
    }

    private static String required(Map<String, String> env, String key) throws SQLException {
        String value = env.get(key);
        if (value == null || value.isBlank()) throw unavailable();
        return value;
    }
    static SQLException unavailable() {
        return new SQLException("PostgreSQL development access is unavailable. Check explicit target, approval, TLS and dedicated privileges privately.");
    }
}
