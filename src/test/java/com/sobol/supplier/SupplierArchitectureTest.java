package com.sobol.supplier;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Focused source/dependency contracts; compiled dependencies are also checked with jdeps. */
class SupplierArchitectureTest {
    @Test void innerLayersDependOnlyInward() throws Exception {
        for (String layer : new String[]{"domain", "application"}) {
            try (var files = Files.walk(Path.of("src/main/java/com/sobol/supplier", layer))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    for (String forbidden : new String[]{"javafx.", "java.sql.", "javax.sql.", "com.sobol.supplier.infrastructure.", "import application.", "com.sobol.category."})
                        assertFalse(source.contains(forbidden), file + " violates the boundary");
                    if (layer.equals("domain")) assertFalse(source.contains("com.sobol.supplier.application."));
                }
            }
        }
    }

    @Test void crudAndLookupConsumersUseServiceAndExplicitComposition() throws Exception {
        for (String controller : new String[]{"SuppliersController", "AddSupplierController", "EditSupplierController", "AddShipmentController"}) {
            String source = Files.readString(Path.of("src/main/java/application", controller + ".java"));
            for (String forbidden : new String[]{"SupplierDAO", "java.sql", "DBAuth", "JdbcSupplierRepository"}) assertFalse(source.contains(forbidden));
            assertTrue(source.contains("SupplierService")); assertTrue(source.contains("SupplierModule.service()"));
        }
        String root = Files.readString(Path.of("src/main/java/application/SupplierModule.java"));
        assertTrue(root.contains("new SupplierService(new JdbcSupplierRepository(PostgresDatabase::get))"));
        String jdbc = Files.readString(Path.of("src/main/java/com/sobol/supplier/infrastructure/JdbcSupplierRepository.java"));
        assertFalse(jdbc.contains("javafx.")); assertFalse(jdbc.contains("import application."));
        assertFalse(Files.exists(Path.of("src/main/java/application/SupplierDAO.java")));
    }

    @Test void shipmentIntegrationRetainsTheExistingTransactionBoundary() throws Exception {
        String view = Files.readString(Path.of("src/main/java/application/ViewSupplierShipmentsController.java"));
        assertTrue(view.contains("ShipmentDAO.getShipmentsBySupplier(id)"));
        assertTrue(view.contains("ShipmentDAO.getTotalCostBySupplier(id)"));
        String add = Files.readString(Path.of("src/main/java/application/AddShipmentController.java"));
        assertTrue(add.contains("loadSuppliers();")); assertTrue(add.contains("d.supplierId = sup.getSupplierId();"));
        assertTrue(add.contains("ShipmentDAO.addShipment(d)"));
    }

    @Test void postgresCompositionAndUnmigratedReceivingBoundaryAreExplicit() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/sobol/supplier/infrastructure/JdbcSupplierRepository.java"));
        assertTrue(source.contains("sobol.supplier")); assertTrue(source.contains("PostgresErrors.unique")); assertFalse(source.contains("1062"));
        String ui = Files.readString(Path.of("src/main/java/application/SuppliersController.java"));
        assertTrue(ui.contains("until receiving and inventory migrate together"));
        assertFalse(ui.contains("ViewSupplierShipments.fxml"));
    }

    @Test void viewMappingIsDetachedAndPreservesAllContactsAndIdentity() {
        var d = new com.sobol.supplier.domain.Supplier.Details("Synthetic", "000", "test@example.invalid", "City", "Address", "Contact", "Notes");
        var domain = new com.sobol.supplier.domain.Supplier(42, d);
        var view = application.Supplier.fromDomain(domain);
        assertAll(() -> assertEquals(42, view.getSupplierId()), () -> assertEquals(d.name(), view.getName()),
                () -> assertEquals(d.phone(), view.getPhone()), () -> assertEquals(d.email(), view.getEmail()),
                () -> assertEquals(d.city(), view.getCity()), () -> assertEquals(d.address(), view.getAddress()),
                () -> assertEquals(d.contactPerson(), view.getContactPerson()), () -> assertEquals(d.notes(), view.getNotes()),
                () -> assertEquals("Synthetic", view.toString()));
        view.setSupplierId(99); view.setName("UI edit"); view.setPhone("111");
        assertEquals(42, domain.id()); assertEquals("Synthetic", domain.details().name()); assertEquals("000", domain.details().phone());
    }
}
