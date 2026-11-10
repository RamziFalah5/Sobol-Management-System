package application;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

/** Test-tooling only: never packaged in the desktop application. No dump importer. */
public final class DevDatabase {
    static final String RESOURCE = "/db/migration/V001__historical_schema.sql";
    static final String HISTORY = "_sobol_dev_schema_history";
    static final List<String> TABLES = List.of("branch", "category", "customer", "supplier",
            "user_account", "user_database", "vehicle", "warehouse", "employee", "product",
            "attendance", "import_shipment", "sales_order", "sales_order_item");
    private DevDatabase() { }

    static void require(Map<String, String> env, String key, String expected) throws SQLException {
        if (!expected.equals(env.get(key))) throw new SQLException("Required safety confirmation: " + key);
    }

    static DBAuth.DbConfiguration configuration(Map<String, String> env, boolean setup) throws SQLException {
        require(env, "SOBOL_CREDENTIAL_ROTATION_CONFIRMED", "true");
        require(env, setup ? "SOBOL_DB_SETUP_CONFIRM" : "SOBOL_IT_CONFIRM", "sobol_dev");
        var config = DBAuth.configuration(env); // Preserve the production boundary unchanged.
        if (!"127.0.0.1".equals(URI.create(config.url().substring(5)).getHost()))
            throw new SQLException("Database tooling requires the numeric loopback address 127.0.0.1.");
        if (!"sobol_app".equals(config.user()))
            throw new SQLException("Only the dedicated sobol_app application identity is accepted.");
        if (setup) {
            String user = env.get("SOBOL_DB_SETUP_USER");
            String secret = env.get("SOBOL_DB_SETUP_PASSWORD");
            if (user == null || user.isBlank() || secret == null || secret.isBlank()
                    || user.equals(config.user()) || secret.equals(config.password()))
                throw new SQLException("Setup requires a separate identity and separate environment-only credential.");
        }
        return config;
    }

    static Connection open(String url, String user, String secret) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", secret);
        properties.setProperty("connectTimeout", "5000");
        properties.setProperty("socketTimeout", "15000");
        properties.setProperty("allowMultiQueries", "false");
        properties.setProperty("allowLoadLocalInfile", "false");
        try { return DriverManager.getConnection(url, properties); }
        catch (SQLException failure) {
            throw new SQLException("Isolated MySQL connection unavailable; inspect private configuration locally.",
                    failure.getSQLState(), failure.getErrorCode()); // No driver message, cause or URL.
        }
    }

    static Connection application(Map<String, String> env) throws Exception {
        var config = configuration(env, false);
        Connection c = open(config.url(), config.user(), config.password());
        try {
            verifyApplication(c);
            verifyVersion(c);
            return c;
        } catch (Exception failure) {
            c.close();
            throw failure;
        }
    }

    static void verifyApplication(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT DATABASE(), CURRENT_USER()")) {
            if (!rs.next() || !"sobol_dev".equals(rs.getString(1))
                    || !Set.of("sobol_app@localhost", "sobol_app@127.0.0.1").contains(rs.getString(2)))
                throw new SQLException("Unexpected development schema or authenticated account.");
        }
        List<String> grants = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SHOW GRANTS")) {
            while (rs.next()) grants.add(rs.getString(1)); // Never log: server may include private metadata.
        }
        validateGrants(grants);
    }

    static void validateGrants(List<String> grants) throws SQLException {
        boolean crud = false;
        for (String original : grants) {
            String grant = original.replace("`", "");
            if (grant.matches("GRANT USAGE ON \\*\\.\\* TO sobol_app@(localhost|127\\.0\\.0\\.1)")) continue;
            // Escaped underscore is mandatory: MySQL schema grants can otherwise match sobolXdev.
            String[] pieces = grant.split(" ON ", 2);
            if (pieces.length != 2 || !pieces[1].matches("sobol\\\\_dev\\.\\* TO sobol_app@(localhost|127\\.0\\.0\\.1)"))
                throw new SQLException("Application grants must be limited to the literal sobol_dev schema; no roles or global privileges.");
            Set<String> privileges = new HashSet<>(Arrays.asList(pieces[0].replaceFirst("^GRANT ", "").split(", ")));
            if (!privileges.equals(Set.of("SELECT", "INSERT", "UPDATE", "DELETE")))
                throw new SQLException("Application identity must have exactly SELECT, INSERT, UPDATE, DELETE.");
            crud = true;
        }
        if (!crud) throw new SQLException("Required development CRUD grant is missing.");
    }

    static String migration() throws Exception {
        try (var in = DevDatabase.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Versioned development schema resource is missing.");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String checksum() throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(migration().getBytes(StandardCharsets.UTF_8)));
    }

    static List<String> statements(String sql) {
        String ddl = sql.replaceAll("(?m)^--[^\\r\\n]*", "");
        List<String> statements = Arrays.stream(ddl.split(";")).map(String::trim).filter(v -> !v.isEmpty()).toList();
        if (statements.size() != TABLES.size()) throw new IllegalArgumentException("Expected 14 historical CREATE TABLE statements.");
        Set<String> created = new HashSet<>();
        for (int i = 0; i < statements.size(); i++) {
            String statement = statements.get(i);
            if (!statement.startsWith("CREATE TABLE `sobol_dev`.`" + TABLES.get(i) + "` ("))
                throw new IllegalArgumentException("Unexpected schema statement or table order.");
            if (Pattern.compile("(?i)\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|USE|GRANT|REVOKE|CALL|LOAD|SET)\\b(?! CASCADE| RESTRICT| NULL)")
                    .matcher(statement.replace("ON DELETE CASCADE", "").replace("ON DELETE RESTRICT", "")
                            .replace("ON UPDATE CASCADE", "")).find())
                throw new IllegalArgumentException("Non-creation SQL is forbidden in V001.");
            var references = Pattern.compile("REFERENCES `sobol_dev`\\.`([^`]+)`").matcher(statement);
            if (Pattern.compile("REFERENCES (?!`sobol_dev`\\.)").matcher(statement).find())
                throw new IllegalArgumentException("Foreign key target must stay inside sobol_dev.");
            while (references.find()) if (!created.contains(references.group(1)))
                throw new IllegalArgumentException("Foreign key creation order is invalid.");
            if (statement.replaceAll("`sobol_dev`\\.`[a-z_]+`", "").contains("`sobol_dev`"))
                throw new IllegalArgumentException("Unexpected qualified identifier.");
            created.add(TABLES.get(i));
        }
        return statements;
    }

    static Set<String> existingTables(Connection c) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (PreparedStatement p = c.prepareStatement("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='sobol_dev'" );
             ResultSet r = p.executeQuery()) { while (r.next()) tables.add(r.getString(1)); }
        return tables;
    }

    static void verifyVersion(Connection c) throws Exception {
        Set<String> expected = new HashSet<>(TABLES); expected.add(HISTORY);
        if (!expected.equals(existingTables(c))) throw new SQLException("Development schema is missing, partial or contains unrecognized tables; run the guarded setup.");
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(
                "SELECT version, checksum FROM sobol_dev._sobol_dev_schema_history")) {
            if (!rs.next() || rs.getInt(1) != 1 || !checksum().equals(rs.getString(2)) || rs.next())
                throw new SQLException("Development schema version/checksum mismatch; no automatic repair is permitted.");
        }
    }

    static void lock(Connection c, String name) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT GET_LOCK(?, 0)")) {
            p.setString(1, name);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || r.getInt(1) != 1) throw new SQLException("Another Sobol development operation holds the safety lock.");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        configuration(System.getenv(), true); // Missing confirmations are actionable, sanitized errors.
        try { setup(System.getenv()); }
        catch (Exception failure) {
            // Keep driver/account/password diagnostics out of Maven's exception report.
            throw new IllegalStateException("Development setup stopped. Check environment confirmations, separate credentials, permissions and schema version privately.");
        }
    }

    static void setup(Map<String, String> env) throws Exception {
        var config = configuration(env, true); // All confirmation checks occur before any connection or DDL.
        var ddl = statements(migration());
        URI target = URI.create(config.url().substring(5));
        String server = "jdbc:mysql://" + target.getRawAuthority() + "/"
                + (target.getRawQuery() == null ? "" : "?" + target.getRawQuery());
        try (Connection c = open(server, env.get("SOBOL_DB_SETUP_USER"), env.get("SOBOL_DB_SETUP_PASSWORD"))) {
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT DATABASE(), CURRENT_USER()")) {
                if (!r.next() || r.getString(1) != null
                        || !r.getString(2).startsWith(env.get("SOBOL_DB_SETUP_USER") + "@"))
                    throw new SQLException("Setup must authenticate its explicit account without a default schema.");
            }
            lock(c, "sobol_dev.foundation");
            if (!existingTables(c).isEmpty()) {
                verifyVersion(c); // Fail closed on partial DDL, unknown tables or version/checksum mismatch.
            } else {
                try (Statement s = c.createStatement()) {
                    s.executeUpdate("CREATE DATABASE IF NOT EXISTS `sobol_dev` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
                    for (String statement : ddl) s.executeUpdate(statement);
                    s.executeUpdate("CREATE TABLE sobol_dev._sobol_dev_schema_history (version INT PRIMARY KEY, checksum CHAR(64) NOT NULL, installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO sobol_dev._sobol_dev_schema_history(version, checksum) VALUES(1, ?)")) {
                    p.setString(1, checksum()); p.executeUpdate();
                }
                verifyVersion(c);
            }
            try (Connection app = open(config.url(), config.user(), config.password())) { verifyApplication(app); }
        }
        System.out.println("Verified sobol_dev V001 and limited application access. No historical records imported.");
    }
}
