package application;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/** Development-only connection boundary. Never falls back to historical credentials. */
public final class DBAuth {
    private DBAuth() {}

    public static Connection get() throws SQLException {
        DbConfiguration config = configuration(System.getenv());
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(config.url(), config.user(), config.password());
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver is unavailable.");
        } catch (SQLException e) {
            // Do not propagate driver messages/causes that may contain account details.
            throw new SQLException("Development database connection failed; check private configuration.",
                    e.getSQLState(), e.getErrorCode());
        }
    }

    static DbConfiguration configuration(Map<String, String> environment) throws SQLException {
        if (!"true".equalsIgnoreCase(environment.get("SOBOL_DB_ENABLED"))) {
            throw new SQLException("Database access is disabled. Explicit development configuration is required.");
        }
        String url = required(environment, "SOBOL_DB_URL");
        String user = required(environment, "SOBOL_DB_USER");
        String password = required(environment, "SOBOL_DB_PASSWORD");
        try {
            if (!url.startsWith("jdbc:mysql://")) throw new URISyntaxException("", "scheme");
            URI uri = new URI(url.substring("jdbc:".length()));
            if (!Set.of("localhost", "127.0.0.1").contains(uri.getHost() == null ? "" : uri.getHost())
                    || !"/sobol_dev".equals(uri.getPath())
                    || uri.getUserInfo() != null || uri.getFragment() != null
                    || uri.getPort() < -1 || uri.getPort() > 65535) {
                throw new URISyntaxException("", "development boundary");
            }
            // Credentials and custom connection hooks must never be embedded in a URL.
            if (uri.getRawQuery() != null) {
                for (String option : uri.getRawQuery().split("&")) {
                    String key = option.split("=", 2)[0];
                    if (!Set.of("sslMode", "serverTimezone").contains(key)) {
                        throw new URISyntaxException("", "unsupported option");
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new SQLException("Only a loopback MySQL URL for the isolated sobol_dev schema is allowed.");
        }
        return new DbConfiguration(url, user, password);
    }

    private static String required(Map<String, String> environment, String key) throws SQLException {
        String value = environment.get(key);
        if (value == null || value.isBlank()) throw new SQLException("Missing required development setting: " + key);
        return value;
    }

    // Deliberately no generated toString(): configuration objects must not expose credentials.
    static final class DbConfiguration {
        private final String url;
        private final String user;
        private final String password;
        DbConfiguration(String url, String user, String password) {
            this.url = url; this.user = user; this.password = password;
        }
        String url() { return url; }
        String user() { return user; }
        String password() { return password; }
    }
}
