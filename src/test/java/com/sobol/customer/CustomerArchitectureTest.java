package com.sobol.customer;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerArchitectureTest {
    @Test void domainAndApplicationHaveOnlyInwardDependencies() throws Exception {
        for (String layer : new String[]{"domain", "application"}) try (var files = Files.walk(Path.of("src/main/java/com/sobol/customer", layer))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : new String[]{"javafx.", "java.sql.", "javax.sql.", "import application.", "com.sobol.customer.infrastructure.", "com.sobol.category.", "com.sobol.supplier."})
                    assertFalse(source.contains(forbidden), file.toString());
                if (layer.equals("domain")) assertFalse(source.contains("com.sobol.customer.application."));
            }
        }
    }
    @Test void allCrudAndLookupConsumersUseServiceAndExplicitComposition() throws Exception {
        for (String name : new String[]{"CustomersController", "AddCustomerController", "EditCustomerController", "PickCustomerForSaleController"}) {
            String source = Files.readString(Path.of("src/main/java/application", name + ".java"));
            for (String forbidden : new String[]{"CustomerDAO", "DBAuth", "java.sql", "JdbcCustomerRepository"}) assertFalse(source.contains(forbidden));
            assertTrue(source.contains("CustomerService")); assertTrue(source.contains("CustomerModule.service()"));
        }
        assertFalse(Files.exists(Path.of("src/main/java/application/CustomerDAO.java")));
        String root = Files.readString(Path.of("src/main/java/application/CustomerModule.java"));
        assertTrue(root.contains("new CustomerService(new JdbcCustomerRepository(PostgresDatabase::get))"));
    }
    @Test void infrastructureImplementsThePortWithoutLegacyPresentation() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/sobol/customer/infrastructure/JdbcCustomerRepository.java"));
        assertFalse(source.contains("javafx.")); assertFalse(source.contains("import application."));
        assertTrue(com.sobol.customer.application.CustomerRepository.class.isAssignableFrom(com.sobol.customer.infrastructure.JdbcCustomerRepository.class));
    }
    @Test void postgresSqlAndConflictContractDoNotTreatNamesAsIdentities() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/sobol/customer/infrastructure/JdbcCustomerRepository.java"));
        assertTrue(source.contains("sobol.customer")); assertTrue(source.contains("PostgresErrors.unique"));
        assertFalse(source.contains("1062")); assertFalse(source.contains("uq_customer_name"));
        for (var reason : com.sobol.customer.application.CustomerException.Reason.values()) assertNotEquals("DUPLICATE_NAME", reason.name());
    }
    @Test void domainAndViewMappingsPreserveAllContactsWithoutAliasing() {
        var d = new com.sobol.customer.domain.Customer.Details("Synthetic", "000", "test@example.invalid", "City", "Address", "Notes");
        var domain = new com.sobol.customer.domain.Customer(42, d);
        var view = application.Customer.fromDomain(domain); assertEquals(domain, view.toDomain());
        var snapshot = view.toDomain(); view.setCustomerId(99); view.setFullName("UI edit"); view.setPhone("111");
        assertEquals(domain, snapshot); assertEquals(42, domain.id()); assertEquals(d, domain.details());
    }
    @Test void salesHandoffUsesAnImmutableSnapshotAndRetainsLegacySalesCalls() throws Exception {
        String picker = Files.readString(Path.of("src/main/java/application/PickCustomerForSaleController.java"));
        assertTrue(picker.contains("Customer.fromDomain(sel.toDomain())"));
        String products = Files.readString(Path.of("src/main/java/application/PickProductsForSaleController.java"));
        assertTrue(products.contains("private com.sobol.customer.domain.Customer customer;"));
        assertTrue(products.contains("Customer.fromDomain(customer)")); assertTrue(products.contains("SalesDAO.getAllProductsForSale()"));
        String quantities = Files.readString(Path.of("src/main/java/application/SaleQuantitiesController.java"));
        assertTrue(quantities.contains("SalesDAO.createOrder(customer.getCustomerId(), emp.id, items)"));
    }
}
