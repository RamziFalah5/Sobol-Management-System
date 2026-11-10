package application;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostgresTransitionContractTest {
    @Test void onlyMigratedShellPagesCanLoadOperationalControllers() {
        for (String page : new String[]{"Category.fxml", "Suppliers.fxml", "Customers.fxml"})
            assertTrue(ControllerSystem.isPageAvailable(page));
        for (String page : new String[]{"Dashboard.fxml", "Products.fxml", "Sales.fxml", "Imports.fxml", "Branches.fxml",
                "Employee.fxml", "Vehicles.fxml", "Reports.fxml", "Settings.fxml", "unknown.fxml"})
            assertFalse(ControllerSystem.isPageAvailable(page));
    }
    @Test void onlyTheThreeCompositionRootsUseThePostgresConnectionBoundary() throws Exception {
        int callers = 0;
        try (var files = Files.list(Path.of("src/main/java/application"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                if (!source.contains("PostgresDatabase")) continue;
                assertTrue(java.util.Set.of("CategoryModule.java", "SupplierModule.java", "CustomerModule.java").contains(file.getFileName().toString()));
                callers++;
            }
        }
        assertEquals(3, callers);
    }
}
