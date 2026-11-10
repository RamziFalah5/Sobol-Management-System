package com.sobol.database;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class PostgresErrorsTest {
    public static PSQLException conflict(String schema, String table, String constraint, String state) {
        return new PSQLException(new ServerErrorMessage("SERROR\0C" + state + "\0s" + schema + "\0t" + table
                + "\0n" + constraint + "\0Mprivate synthetic localized diagnostic\0Dprivate synthetic duplicate value\0\0"));
    }
    @Test void matchesOnlyStructuredStateSchemaTableAndConstraint() {
        assertTrue(PostgresErrors.unique(conflict("sobol", "customer", "uq_customer_email", "23505"), "customer", "uq_customer_email"));
        for (String[] fields : new String[][]{{"other", "customer", "uq_customer_email", "23505"},
                {"sobol", "other", "uq_customer_email", "23505"}, {"sobol", "customer", "unknown", "23505"},
                {"sobol", "customer", "uq_customer_email", "23503"}})
            assertFalse(PostgresErrors.unique(conflict(fields[0], fields[1], fields[2], fields[3]), "customer", "uq_customer_email"));
        assertFalse(PostgresErrors.unique(new SQLException("uq_customer_email", "23505"), "customer", "uq_customer_email"));
        assertFalse(PostgresErrors.unique(new SQLException("uq_customer_email", "23000", 1062), "customer", "uq_customer_email"));
    }
}
