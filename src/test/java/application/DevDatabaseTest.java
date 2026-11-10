package application;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class DevDatabaseTest {
    private Map<String, String> env() {
        return new HashMap<>(Map.of("SOBOL_DB_ENABLED", "true", "SOBOL_DB_URL", "jdbc:mysql://127.0.0.1/sobol_dev",
                "SOBOL_DB_USER", "sobol_app", "SOBOL_DB_PASSWORD", "synthetic-application-value",
                "SOBOL_CREDENTIAL_ROTATION_CONFIRMED", "true", "SOBOL_IT_CONFIRM", "sobol_dev",
                "SOBOL_DB_SETUP_CONFIRM", "sobol_dev", "SOBOL_DB_SETUP_USER", "sobol_setup",
                "SOBOL_DB_SETUP_PASSWORD", "synthetic-setup-value"));
    }

    @Test void setupAndIntegrationRequireExplicitConfirmationsBeforeConnecting() throws Exception {
        assertNotNull(DevDatabase.configuration(env(), true));
        assertNotNull(DevDatabase.configuration(env(), false));
        for (boolean setup : List.of(true, false)) {
            for (String key : List.of("SOBOL_CREDENTIAL_ROTATION_CONFIRMED",
                    setup ? "SOBOL_DB_SETUP_CONFIRM" : "SOBOL_IT_CONFIRM", "SOBOL_DB_ENABLED",
                    "SOBOL_DB_URL", "SOBOL_DB_USER", "SOBOL_DB_PASSWORD")) {
                var e = env(); e.remove(key);
                assertThrows(SQLException.class, () -> DevDatabase.configuration(e, setup), key);
            }
        }
    }

    @Test void administrativeAndApplicationCredentialsMustBeSeparate() {
        for (String key : List.of("SOBOL_DB_SETUP_USER", "SOBOL_DB_SETUP_PASSWORD")) {
            var e = env(); e.remove(key);
            assertThrows(SQLException.class, () -> DevDatabase.configuration(e, true));
            e.put(key, e.get(key.replace("_SETUP", "")));
            assertThrows(SQLException.class, () -> DevDatabase.configuration(e, true));
        }
        var e = env(); e.put("SOBOL_DB_USER", "root");
        assertThrows(SQLException.class, () -> DevDatabase.configuration(e, false));
    }

    @Test void inheritedTargetBoundaryRejectsOriginalRemoteAndCredentialBearingUrls() {
        for (String url : List.of("jdbc:mysql://localhost/accounts", "jdbc:mysql://example.invalid/sobol_dev",
                "jdbc:mysql://127.0.0.1/sobol_dev?allowMultiQueries=true", "jdbc:mysql://127.0.0.1/other")) {
            var e = env(); e.put("SOBOL_DB_URL", url);
            assertThrows(SQLException.class, () -> DevDatabase.configuration(e, false));
            assertThrows(SQLException.class, () -> DevDatabase.configuration(e, true));
        }
    }

    @Test void refusesGlobalDdlRoleWildcardAndGrantOptionPrivileges() throws Exception {
        String usage = "GRANT USAGE ON *.* TO `sobol_app`@`localhost`";
        String crud = "GRANT SELECT, INSERT, UPDATE, DELETE ON `sobol\\_dev`.* TO `sobol_app`@`localhost`";
        DevDatabase.validateGrants(List.of(usage, crud));
        for (List<String> grants : List.of(List.of(usage),
                List.of(crud.replace("sobol\\_dev", "sobol_dev")),
                List.of(crud.replace("sobol\\_dev", "accounts")),
                List.of(crud + " WITH GRANT OPTION"),
                List.of(crud.replace("SELECT, INSERT, UPDATE, DELETE", "ALL PRIVILEGES")),
                List.of(crud, "GRANT `administrator`@`localhost` TO `sobol_app`@`localhost`"),
                List.of(crud.replace("SELECT, INSERT, UPDATE, DELETE", "SELECT, INSERT, UPDATE, DELETE, CREATE")))) {
            assertThrows(SQLException.class, () -> DevDatabase.validateGrants(grants));
        }
    }

    @Test void versionedDdlIsCreationOnlyOrderedQualifiedAndMatchesReviewedDigest() throws Exception {
        String sql = DevDatabase.migration();
        assertEquals(14, DevDatabase.statements(sql).size());
        assertFalse(sql.toLowerCase(Locale.ROOT).contains("accounts"));
        assertFalse(sql.contains("AUTO_INCREMENT="));
        String manifest = Files.readString(Path.of("docs/schema-provenance.json"));
        assertTrue(manifest.contains("\"sha256\": \"" + DevDatabase.checksum() + "\""));
        assertThrows(IllegalArgumentException.class, () -> DevDatabase.statements(sql + "\nDELETE FROM sobol_dev.product;"));
        assertThrows(IllegalArgumentException.class, () -> DevDatabase.statements(sql.replace("CREATE TABLE `sobol_dev`.`branch`", "CREATE TABLE `other`.`branch`")));
        assertThrows(IllegalArgumentException.class, () -> DevDatabase.statements(sql.replace("REFERENCES `sobol_dev`.`employee`", "REFERENCES `sobol_dev`.`not_created`")));
    }

    @Test void unavailableConnectionFailureIsSanitizedWithoutNetworkAccess() throws Exception {
        Driver failing = new Driver() {
            public boolean acceptsURL(String url) { return url.equals("jdbc:sobol-unavailable"); }
            public Connection connect(String url, Properties p) throws SQLException {
                if (!acceptsURL(url)) return null;
                assertEquals("false", p.getProperty("allowMultiQueries"));
                assertEquals("5000", p.getProperty("connectTimeout"));
                throw new SQLException("private synthetic driver diagnostic", "08001", 123);
            }
            public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) { return new DriverPropertyInfo[0]; }
            public int getMajorVersion() { return 1; }
            public int getMinorVersion() { return 0; }
            public boolean jdbcCompliant() { return false; }
            public Logger getParentLogger() { return Logger.getGlobal(); }
        };
        DriverManager.registerDriver(failing);
        try {
            SQLException failure = assertThrows(SQLException.class,
                    () -> DevDatabase.open("jdbc:sobol-unavailable", "synthetic", "synthetic"));
            assertEquals("08001", failure.getSQLState());
            assertEquals(123, failure.getErrorCode());
            assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains("private synthetic"));
        } finally { DriverManager.deregisterDriver(failing); }
    }
}
