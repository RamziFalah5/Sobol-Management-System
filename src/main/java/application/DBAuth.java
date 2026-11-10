package application;

import java.sql.Connection;
import java.sql.SQLException;

/** Retired MySQL boundary. Legacy workflows must migrate as complete dependency closures. */
public final class DBAuth {
    private DBAuth() { }
    public static Connection get() throws SQLException {
        throw new SQLException("Legacy database workflows are unavailable during the PostgreSQL transition.");
    }
}
