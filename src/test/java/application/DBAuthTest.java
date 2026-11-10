package application;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class DBAuthTest {
    @Test void legacyBoundaryIsUnconditionallyClosed() {
        var failure = assertThrows(SQLException.class, DBAuth::get);
        assertTrue(failure.getMessage().contains("PostgreSQL transition"));
        assertNull(failure.getCause()); assertEquals(0, failure.getSuppressed().length);
    }
    @Test void normalVerificationDisablesBothDatabaseBoundaries() {
        assertEquals("false", System.getenv("SOBOL_DB_ENABLED"));
        assertEquals("false", System.getenv("SOBOL_PG_ENABLED"));
        assertThrows(SQLException.class, com.sobol.database.PostgresDatabase::get);
    }
}
