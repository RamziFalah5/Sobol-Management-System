package com.sobol.auth;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthArchitectureTest {
    @Test void domainAndApplicationDoNotDependOnUiJdbcOrConcreteInfrastructure() throws Exception {
        for (String layer : new String[]{"domain", "application"}) try (var files = Files.walk(Path.of("src/main/java/com/sobol/auth", layer))) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : new String[]{"javafx.", "java.sql.", "org.postgresql.", "import application.", "com.sobol.database.", "com.sobol.auth.infrastructure."})
                    assertFalse(source.contains(forbidden), file.toString());
                if (layer.equals("domain")) assertFalse(source.contains("com.sobol.auth.application."));
            }
        }
    }
    @Test void accountMigrationHasNoSeedAndV1IsUnchanged() throws Exception {
        String sql = Files.readString(Path.of("src/test/resources/db/postgresql/V2__accounts.sql"));
        assertTrue(sql.contains("uq_account_login")); assertTrue(sql.contains("DEFAULT 'PENDING'")); assertTrue(sql.contains("DEFAULT 'MEMBER'"));
        for (String forbidden : new String[]{"INSERT INTO", "user_database", "CREATE DATABASE", "CREATE ROLE", "GRANT ", "DROP "}) assertFalse(sql.contains(forbidden));
        var digest = java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of("src/test/resources/db/postgresql/V1__reference_data.sql")));
        assertEquals("8eb6eb1591b0a0fac6d6eb5377760699a6664ca023fb5611e355f466fbc88a90", java.util.HexFormat.of().formatHex(digest), "Published V1 must remain immutable");
        assertTrue(Files.readString(Path.of("docs/postgresql-migrations.json")).contains(java.util.HexFormat.of().formatHex(digest)));
    }
    @Test void loginAndShellHaveNoLegacyCredentialVerificationOrDirectDatabaseAccess() throws Exception {
        for (String file : new String[]{"Controller1.java", "ControllerSystem.java"}) {
            String source = Files.readString(Path.of("src/main/java/application",file));
            for (String forbidden : new String[]{"java.sql", "DBAuth", "PasswordUtil", "PostgresDatabase", "printStackTrace", "System.out"}) assertFalse(source.contains(forbidden));
        }
        String root = Files.readString(Path.of("src/main/java/application/AuthModule.java"));
        assertTrue(root.contains("sessions.require(handle)")); assertTrue(root.contains("SOBOL_REGISTRATION_ENABLED"));
    }

}
