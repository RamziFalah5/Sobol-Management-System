package application;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DBAuthTest {
    private Map<String, String> development() {
        return new HashMap<>(Map.of("SOBOL_DB_ENABLED", "true",
                "SOBOL_DB_URL", "jdbc:mysql://127.0.0.1:3306/sobol_dev?sslMode=DISABLED&serverTimezone=UTC",
                "SOBOL_DB_USER", "synthetic-test-user", "SOBOL_DB_PASSWORD", "synthetic-test-value"));
    }

    @Test void missingConfigurationFailsClosed() {
        assertThrows(SQLException.class, () -> DBAuth.configuration(Map.of()));
    }

    @Test void testProcessCannotConnectToAnyDatabase() {
        assertEquals("false", System.getenv("SOBOL_DB_ENABLED"));
        assertThrows(SQLException.class, DBAuth::get);
    }

    @Test void everyCredentialMustBeExplicit() {
        for (String key : new String[]{"SOBOL_DB_URL", "SOBOL_DB_USER", "SOBOL_DB_PASSWORD"}) {
            var env = development(); env.remove(key);
            assertThrows(SQLException.class, () -> DBAuth.configuration(env));
        }
    }

    @Test void rejectsOriginalDatabaseAndRemoteHostsWithoutDisclosingValues() {
        // Build forbidden credential-bearing inputs from synthetic fixture values only.
        String userInfo = "jdbc:mysql://" + development().get("SOBOL_DB_USER") + ":"
                + development().get("SOBOL_DB_PASSWORD") + "@localhost/sobol_dev";
        String passwordQuery = "jdbc:mysql://localhost/sobol_dev?" + "password="
                + development().get("SOBOL_DB_PASSWORD");
        for (String url : new String[]{"jdbc:mysql://127.0.0.1:3306/accounts",
                "jdbc:mysql://example.invalid:3306/sobol_dev",
                userInfo, passwordQuery}) {
            var env = development(); env.put("SOBOL_DB_URL", url);
            SQLException failure = assertThrows(SQLException.class, () -> DBAuth.configuration(env));
            assertFalse(failure.getMessage().contains(url));
            assertFalse(failure.getMessage().contains("synthetic-test-value"));
        }
    }

    @Test void acceptsOnlyExplicitIsolatedDevelopmentConfigurationWithoutConnecting() throws Exception {
        var config = DBAuth.configuration(development());
        assertTrue(config.url().contains("/sobol_dev"));
        assertFalse(config.toString().contains("synthetic-test-value"));
    }
}
