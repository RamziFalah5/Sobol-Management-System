package com.sobol.category;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Small source-boundary checks; this single Maven application is not a JVM-enforced module split. */
class CategoryArchitectureTest {
    @Test void innerLayersHaveNoUiJdbcOrInfrastructureDependencies() throws Exception {
        for (String layer : new String[]{"domain", "application"}) {
            try (var files = Files.walk(Path.of("src/main/java/com/sobol/category", layer))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    for (String forbidden : new String[]{"javafx.", "java.sql.", "javax.sql.", "com.sobol.category.infrastructure.", "import application."})
                        assertFalse(source.contains(forbidden), file + " crosses an inner-layer boundary");
                }
            }
        }
    }

    @Test void infrastructureDoesNotDependOnLegacyPresentationAndControllersDoNotReferenceDaos() throws Exception {
        String jdbc = Files.readString(Path.of("src/main/java/com/sobol/category/infrastructure/JdbcCategoryRepository.java"));
        assertFalse(jdbc.contains("import application.")); assertFalse(jdbc.contains("javafx."));
        for (String controller : new String[]{"CategoryController", "AddCategoryController", "EditCategoryController"}) {
            String source = Files.readString(Path.of("src/main/java/application", controller + ".java"));
            assertFalse(source.contains("CategoryDAO")); assertFalse(source.contains("java.sql")); assertFalse(source.contains("DBAuth"));
            assertTrue(source.contains("CategoryService"));
        }
    }

    @Test void compositionAndSqlUseOnlyThePostgresSlice() throws Exception {
        String root = Files.readString(Path.of("src/main/java/application/CategoryModule.java"));
        assertTrue(root.contains("PostgresDatabase::get")); assertFalse(root.contains("DBAuth"));
        String jdbc = Files.readString(Path.of("src/main/java/com/sobol/category/infrastructure/JdbcCategoryRepository.java"));
        assertTrue(jdbc.contains("sobol.category")); assertTrue(jdbc.contains("PostgresErrors.unique")); assertFalse(jdbc.contains("1062"));
    }

    @Test void javaFxViewStateCannotMutateTheImmutableDomainObject() {
        var domain = new com.sobol.category.domain.Category(42, "Synthetic", "Description");
        var view = application.Category.fromDomain(domain);
        view.idProperty().set(99); view.nameProperty().set("Edited UI state");
        assertEquals(42, domain.id()); assertEquals("Synthetic", domain.name());
        assertEquals("Description", domain.description());
    }
}
