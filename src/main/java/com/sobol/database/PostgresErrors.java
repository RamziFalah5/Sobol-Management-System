package com.sobol.database;

import java.sql.SQLException;
import org.postgresql.util.PSQLException;

/** PostgreSQL protocol fields only; never parse localized diagnostics or duplicate values. */
public final class PostgresErrors {
    private PostgresErrors() { }
    public static boolean unique(SQLException failure, String table, String constraint) {
        if (!(failure instanceof PSQLException pg) || !"23505".equals(pg.getSQLState())) return false;
        var error = pg.getServerErrorMessage();
        return error != null && "sobol".equals(error.getSchema()) && table.equals(error.getTable())
                && constraint.equals(error.getConstraint());
    }
}
