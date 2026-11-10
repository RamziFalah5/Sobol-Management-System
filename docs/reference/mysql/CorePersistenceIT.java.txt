package application;

import org.junit.jupiter.api.*;
import com.sobol.category.application.CategoryException;
import com.sobol.supplier.application.SupplierException;
import com.sobol.customer.application.CustomerException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in only. Correct-behavior assertions intentionally remain red for original defects. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(90)
class CorePersistenceIT {
    private Connection c;
    private SyntheticFixture f;

    @BeforeAll void verifyTargetBeforeAnyFixtureWrite() throws Exception {
        c = DevDatabase.application(System.getenv());
        DevDatabase.lock(c, "sobol_dev.foundation");
        // Negative IDs are used only for rejection tests; never mutate an unowned sentinel row.
        for (String table : List.of("branch", "category", "supplier", "employee")) {
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + table + "_id=-1")) {
                if (!r.next() || r.getInt(1) != 0)
                    throw new SQLException("An invalid-ID test sentinel is occupied; no fixture writes are permitted.");
            }
        }
    }
    @BeforeEach void seed() throws Exception { f = SyntheticFixture.create(c); }
    @AfterEach void cleanOwnedRows() throws Exception { if (f != null) { f.close(); f = null; } }
    @AfterAll void disconnect() throws Exception { if (c != null) c.close(); }

    @Test void dedicatedConnectionAndHistoricalSchemaAreAvailable() throws Exception {
        DevDatabase.verifyApplication(c);
        DevDatabase.verifyVersion(c);
        assertEquals(15, DevDatabase.existingTables(c).size()); // 14 business + 1 tooling history.
        assertEquals(15, f.integer("SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='sobol_dev'"));
    }

    @Test void branchCreationWarehouseUpdatesAndAggregatesPersist() throws Exception {
        String name = f.prefix + "_dao_branch";
        boolean inserted = BranchDAO.insertBranch(name, "Synthetic City", "0000000000");
        int id = f.integer("SELECT branch_id FROM branch WHERE name=?", name); f.branches.add(id);
        assertTrue(inserted);
        assertTrue(BranchDAO.getAllBranches().stream().anyMatch(b -> b.getBranchId() == id));
        assertTrue(WarehouseDAO.insertWarehouse(id, f.prefix + "_dao_warehouse", 7));
        var warehouses = WarehouseDAO.getWarehousesByBranch(id);
        assertEquals(1, warehouses.size());
        int wh = warehouses.getFirst().getWarehouseId();
        assertEquals(id, warehouses.getFirst().getBranchId());
        assertTrue(WarehouseDAO.updateWarehouse(wh, id, f.prefix + "_edited", 9, 2));
        assertEquals(9, f.integer("SELECT total_pallet_capacity FROM branch WHERE branch_id=?", id));
        assertEquals(1, f.integer("SELECT warehouses_count FROM branch WHERE branch_id=?", id));
        assertTrue(WarehouseDAO.deleteWarehouse(wh, id, 9));
        assertEquals(0, f.integer("SELECT total_pallet_capacity FROM branch WHERE branch_id=?", id));
    }

    @Test void productDaoPreservesRelationsPricesAndThresholds() throws Exception {
        var d = productForm("dao_product", f.warehouse, 7);
        assertTrue(ProductDAO.insertProduct(d));
        var product = ProductDAO.getProductsByCategory(f.category).stream().filter(p -> p.getName().equals(d.name)).findFirst().orElseThrow();
        assertAll(() -> assertEquals(f.category, product.getCategoryId()),
                () -> assertEquals(f.branch, product.getBranchId()), () -> assertEquals(f.warehouse, product.getWarehouseId()),
                () -> assertEquals(7, product.getQuantity()), () -> assertEquals(5, product.getMinQuantity()),
                () -> assertEquals(100, product.getMaxQuantity()), () -> assertEquals(1.25, product.getSellingPrice()));
        d.quantity = 9;
        assertTrue(ProductDAO.updateProduct(product.getProductId(), d));
        assertEquals(9, f.stock(product.getProductId()));
        assertTrue(ProductDAO.deleteProduct(product.getProductId()));
    }

    @Test void capacityUsesRoundedPalletsAndExcludesCurrentProduct() {
        assertEquals(9, WarehouseViewDAO.getUsedPalletsForWarehouse(f.nearWarehouse));
        assertTrue(ProductDAO.canFitInWarehouse(f.nearWarehouse, 1, null));
        assertFalse(ProductDAO.canFitInWarehouse(f.nearWarehouse, 2, null));
        assertTrue(ProductDAO.canFitInWarehouse(f.nearWarehouse, 10, f.nearProduct));
        assertEquals(0, WarehouseViewDAO.getUsedPalletsForWarehouse(f.emptyWarehouse));
        var status = WarehouseViewDAO.getStockStatusCounts(f.warehouse);
        assertEquals(1, status.low); assertEquals(1, status.out);
    }

    @Test @Tag("original-defect") void emptyWarehouseHasZeroProducts() {
        var summary = WarehouseViewDAO.getWarehouseSummary(f.emptyWarehouse);
        assertNotNull(summary);
        assertEquals(0, summary.totalProducts, "Original LEFT JOIN COUNT(*) counts its empty placeholder row.");
    }

    @Test void supplierAndCustomerDaoRoundTripsAndUniqueEmail() throws Exception {
        var suppliers = SupplierModule.service();
        String supplierEmail = f.prefix + "_updated_supplier@example.invalid";
        var supplier = suppliers.updateSupplier(f.supplier, f.prefix + "_supplier", "0000000000",
                supplierEmail, null, null, null, null);
        assertEquals(f.supplier, supplier.id());
        assertTrue(suppliers.listSuppliers().stream().anyMatch(s -> s.id() == f.supplier && supplierEmail.equals(s.details().email())));
        var customers = CustomerModule.service();
        String customerEmail = f.prefix + "_updated_customer@example.invalid";
        var customer = customers.updateCustomer(f.customer, f.prefix + "_customer", "0000000000", customerEmail, null, null, null);
        assertEquals(f.customer, customer.id());
        assertTrue(customers.listCustomers().stream().anyMatch(row -> row.id() == f.customer && customerEmail.equals(row.details().email())));
        assertEquals(CustomerException.Reason.DUPLICATE_EMAIL, assertThrows(CustomerException.class,
                () -> customers.createCustomer(f.prefix + "_duplicate_email", "0000000000", customerEmail, null, null, null)).reason());
        assertTrue(customers.listCustomers().stream().anyMatch(row -> customerEmail.equals(row.details().email())));
        assertEquals(customer, customers.listCustomers().stream().filter(row -> row.id() == f.customer).findFirst().orElseThrow());
    }

    @Test void unchangedExistingCustomerUpdateSucceeds() {
        var service = CustomerModule.service();
        var existing = service.listCustomers().stream().filter(row -> row.id() == f.customer).findFirst().orElseThrow();
        var d = existing.details();
        assertEquals(existing, service.updateCustomer(existing.id(), d.fullName(), d.phone(), d.email(), d.city(), d.address(), d.notes()));
    }

    @Test void customerCreationNullsAndUniqueConstraintsUseNewRepository() throws Exception {
        var service = CustomerModule.service();
        var first = service.createCustomer(f.prefix + "_customer_created", "000", f.prefix + "_created@example.invalid", null, null, null);
        com.sobol.customer.domain.Customer second = null;
        try {
            second = service.createCustomer(f.prefix + "_customer_second", "000", " ", "", "", "");
            var target = second;
            assertEquals(first, service.listCustomers().stream().filter(row -> row.id() == first.id()).findFirst().orElseThrow());
            assertEquals(1, f.integer("SELECT COUNT(*) FROM customer WHERE customer_id=? AND email IS NULL AND city='' AND address='' AND notes=''", second.id()));
            assertEquals(CustomerException.Reason.DUPLICATE_NAME, assertThrows(CustomerException.class,
                    () -> service.createCustomer(first.details().fullName(), "000", null, null, null, null)).reason());
            assertEquals(CustomerException.Reason.DUPLICATE_EMAIL, assertThrows(CustomerException.class,
                    () -> service.createCustomer(f.prefix + "_customer_conflict", "000", first.details().email(), null, null, null)).reason());
            assertEquals(CustomerException.Reason.DUPLICATE_NAME, assertThrows(CustomerException.class,
                    () -> service.updateCustomer(target.id(), first.details().fullName(), "000", null, null, null, null)).reason());
            assertEquals(CustomerException.Reason.DUPLICATE_EMAIL, assertThrows(CustomerException.class,
                    () -> service.updateCustomer(target.id(), target.details().fullName(), "000", first.details().email(), null, null, null)).reason());
            assertEquals(target, service.listCustomers().stream().filter(row -> row.id() == target.id()).findFirst().orElseThrow());
        } finally {
            if (second != null) f.update("DELETE FROM customer WHERE customer_id=?", second.id());
            f.update("DELETE FROM customer WHERE customer_id=?", first.id());
        }
    }

    @Test void deletedCustomerUpdateReportsNotFound() throws Exception {
        int removed = f.insert("customer", "full_name,phone", f.prefix + "_customer_removed", "000");
        try {
            f.update("DELETE FROM customer WHERE customer_id=?", removed);
            assertEquals(CustomerException.Reason.NOT_FOUND, assertThrows(CustomerException.class,
                    () -> CustomerModule.service().updateCustomer(removed, "Synthetic", "000", null, null, null, null)).reason());
        } finally { f.update("DELETE FROM customer WHERE customer_id=?", removed); }
    }

    @Test void unchangedExistingSupplierUpdateSucceeds() {
        var service = SupplierModule.service();
        var supplier = service.listSuppliers().stream().filter(s -> s.id() == f.supplier).findFirst().orElseThrow();
        var d = supplier.details();
        assertEquals(supplier, service.updateSupplier(supplier.id(), d.name(), d.phone(), d.email(), d.city(), d.address(), d.contactPerson(), d.notes()));
    }

    @Test void supplierCreationNullMappingAndUniqueConflictsUseTheNewRepository() throws Exception {
        var service = SupplierModule.service();
        var created = service.createSupplier(f.prefix + "_created_supplier", "0000000000", " ", null, null, null, null);
        try {
            assertEquals(created, service.listSuppliers().stream().filter(s -> s.id() == created.id()).findFirst().orElseThrow());
            assertEquals(1, f.integer("SELECT COUNT(*) FROM supplier WHERE supplier_id=? AND email IS NULL AND city IS NULL AND address IS NULL AND contact_person IS NULL AND notes IS NULL", created.id()));
            assertEquals(SupplierException.Reason.DUPLICATE_NAME, assertThrows(SupplierException.class,
                    () -> service.createSupplier(created.details().name(), "000", null, null, null, null, null)).reason());
            var existing = service.listSuppliers().stream().filter(s -> s.id() == f.supplier).findFirst().orElseThrow();
            assertEquals(SupplierException.Reason.DUPLICATE_NAME, assertThrows(SupplierException.class,
                    () -> service.updateSupplier(created.id(), existing.details().name(), "000", null, null, null, null, null)).reason());
            assertEquals(created, service.listSuppliers().stream().filter(s -> s.id() == created.id()).findFirst().orElseThrow());
        } finally { f.update("DELETE FROM supplier WHERE supplier_id=?", created.id()); }
    }

    @Test void deletedSupplierUpdateReportsNotFound() throws Exception {
        int removed = f.insert("supplier", "name,phone", f.prefix + "_removed_supplier", "000");
        try {
            f.update("DELETE FROM supplier WHERE supplier_id=?", removed);
            assertEquals(SupplierException.Reason.NOT_FOUND, assertThrows(SupplierException.class,
                    () -> SupplierModule.service().updateSupplier(removed, "Synthetic", "000", null, null, null, null, null)).reason());
        } finally { f.update("DELETE FROM supplier WHERE supplier_id=?", removed); }
    }

    @Test void shipmentCommitsHeaderAndInventoryTogether() throws Exception {
        int count = f.shipments();
        assertTrue(ShipmentDAO.addShipment(shipment(f.normal, f.warehouse, 5)));
        assertEquals(25, f.stock(f.normal));
        assertEquals(count + 1, f.shipments());
        assertEquals(2, ShipmentDAO.getShipmentsBySupplier(f.supplier).size());
        assertEquals(3.50, ShipmentDAO.getTotalCostBySupplier(f.supplier));
    }

    @Test void shipmentRejectsOverflowAndInvalidForeignKeyWithoutChanges() throws Exception {
        int count = f.shipments();
        assertFalse(ShipmentDAO.addShipment(shipment(f.nearProduct, f.nearWarehouse, 20)));
        assertEquals(81, f.stock(f.nearProduct)); assertEquals(count, f.shipments());
        var invalid = shipment(f.normal, f.warehouse, 5); invalid.supplierId = -1;
        assertFalse(ShipmentDAO.addShipment(invalid));
        assertEquals(20, f.stock(f.normal)); assertEquals(count, f.shipments());
    }

    @Test void multiItemSaleUsesCurrentPricesAndDeductsStock() throws Exception {
        int before = f.orders();
        assertTrue(sell(List.of(item(f.normal, 2), item(f.low, 1))));
        assertEquals(18, f.stock(f.normal)); assertEquals(2, f.stock(f.low)); assertEquals(before + 1, f.orders());
        int order = f.integer("SELECT MAX(order_id) FROM sales_order WHERE customer_id=?", f.customer);
        assertEquals(0, new BigDecimal("6.00").compareTo(f.decimal("SELECT total_amount FROM sales_order WHERE order_id=?", order)));
        assertEquals(0, new BigDecimal("6.00").compareTo(f.decimal("SELECT SUM(line_total) FROM sales_order_item WHERE order_id=?", order)));
        assertEquals(2, SalesDAO.getOrderItems(order).size());
        assertTrue(SalesDAO.searchOrdersByCustomerName(f.prefix).stream().anyMatch(o -> o.getOrderId() == order));
    }

    @Test void failedSecondSalesItemRollsBackFirstItemHeaderAndStock() throws Exception {
        int before = f.orders();
        assertFalse(sell(List.of(item(f.normal, 2), item(f.zero, 1))));
        assertEquals(20, f.stock(f.normal)); assertEquals(0, f.stock(f.zero)); assertEquals(before, f.orders());
        assertFalse(sell(List.of()));
        assertFalse(sell(List.of(item(f.normal, 0))));
        assertFalse(sell(List.of(item(f.normal, -1))));
        assertEquals(before, f.orders()); assertEquals(20, f.stock(f.normal));
    }

    @Test void employeeInsertionDriverAssignmentAndSoftDeletePersist() throws Exception {
        assertTrue(EmployeeDAO.insertGeneralWorker(f.prefix + "_inserted", "0000000000", f.branch,
                SyntheticFixture.DATE, null, "DAILY", 50.0, null));
        assertTrue(WorkerDAO.getDailyWorkers().stream().anyMatch(e -> e.getFullName().equals(f.prefix + "_inserted")));
        assertTrue(GeneralWorkerDAO.getHourlyWorkers().stream().anyMatch(e -> e.getEmployeeId() == f.hourly));
        assertTrue(TruckDriverDAO.getAllDrivers().stream().anyMatch(e -> e.getEmployeeId() == f.driver));
        var available = VehicleDAO.getAvailableVehiclesForDrivers();
        assertTrue(available.stream().anyMatch(v -> v.getVehicleId() == f.freeVehicle));
        assertFalse(available.stream().anyMatch(v -> v.getVehicleId() == f.assignedVehicle));
        assertTrue(SalesRepDAO.deleteSalesRep(f.representative));
        assertEquals(0, f.integer("SELECT is_active FROM employee WHERE employee_id=?", f.representative));
        assertFalse(SalesRepDAO.getAllSalesReps().stream().anyMatch(e -> e.getEmployeeId() == f.representative));
    }

    @Test void dailyHourlyAndDriverAttendanceUpsertWithoutDuplicates() throws Exception {
        var date = SyntheticFixture.DATE.plusDays(1);
        assertTrue(AttendanceDAO.saveDaily(date, List.of(new DailyAttendanceRow(f.daily, "Synthetic", false))));
        assertEquals(Boolean.FALSE, AttendanceDAO.getPresent(f.daily, date)); assertNull(AttendanceDAO.getHours(f.daily, date));
        assertTrue(AttendanceDAO.saveHourly(date, List.of(new HourlyAttendanceRow(f.hourly, "Synthetic", 7))));
        assertEquals(7, AttendanceDAO.getHours(f.hourly, date)); assertEquals(Boolean.TRUE, AttendanceDAO.getPresent(f.hourly, date));
        assertTrue(AttendanceDAO.saveHourly(date, List.of(new HourlyAttendanceRow(f.hourly, "Synthetic", 0))));
        assertEquals(Boolean.FALSE, AttendanceDAO.getPresent(f.hourly, date));
        assertTrue(AttendanceDAO.upsertAttendance(f.driver, date, true, null));
        assertEquals(Boolean.TRUE, AttendanceDAO.getPresent(f.driver, date));
        assertEquals(3, f.integer("SELECT COUNT(*) FROM attendance WHERE employee_id IN (?,?,?) AND work_date=?", f.daily, f.hourly, f.driver, date));
    }

    @Test void attendanceBatchRollsBackWhenSecondEmployeeDoesNotExist() throws Exception {
        var date = SyntheticFixture.DATE.plusDays(2);
        assertFalse(AttendanceDAO.saveDaily(date, List.of(new DailyAttendanceRow(f.daily, "Synthetic", true),
                new DailyAttendanceRow(-1, "Synthetic invalid", true))));
        assertEquals(0, f.integer("SELECT COUNT(*) FROM attendance WHERE employee_id=? AND work_date=?", f.daily, date));
    }

    @Test void databaseEnforcesUniqueKeysAndForeignKeys() throws Exception {
        SQLException duplicate = assertThrows(SQLException.class, () -> f.insert("branch", "name,city", f.prefix + "_branch", "Synthetic"));
        assertEquals(1062, duplicate.getErrorCode());
        SQLException foreignKey = assertThrows(SQLException.class, () -> f.insert("warehouse", "branch_id,name,pallet_capacity", -1, f.prefix + "_invalid", 1));
        assertEquals(1452, foreignKey.getErrorCode());
        SQLException assignedTwice = assertThrows(SQLException.class, () -> f.employee("other_driver", "TRUCK_DRIVER", null, f.assignedVehicle, 1));
        assertEquals(1062, assignedTwice.getErrorCode());
        assertThrows(SQLException.class, () -> f.update("DELETE FROM category WHERE category_id=?", f.category));
        int id = f.integer("SELECT MAX(order_id) FROM sales_order WHERE customer_id=?", f.customer);
        f.update("DELETE FROM sales_order WHERE order_id=?", id);
        assertEquals(0, f.integer("SELECT COUNT(*) FROM sales_order_item WHERE order_id=?", id));
        assertEquals(1, f.integer("SELECT COUNT(*) FROM user_database WHERE user_id=?", f.user));
    }

    @Test @Tag("original-defect") void nonexistentCategoryUpdateMustReportFailure() throws Exception {
        int removed = f.insert("category", "name,description", f.prefix + "_removed", "Synthetic");
        try {
            f.update("DELETE FROM category WHERE category_id=?", removed);
            var failure = assertThrows(CategoryException.class,
                    () -> CategoryModule.service().updateCategory(removed, f.prefix + "_absent", "Synthetic"));
            assertEquals(CategoryException.Reason.NOT_FOUND, failure.reason());
        } finally { f.update("DELETE FROM category WHERE category_id=?", removed); }
    }

    @Test void unchangedExistingCategoryUpdateSucceeds() {
        var service = CategoryModule.service();
        var category = service.listCategories().stream().filter(c -> c.id() == f.category).findFirst().orElseThrow();
        assertEquals(category, service.updateCategory(category.id(), category.name(), category.description()));
    }

    @Test void categoryCreationAndUniqueConflictsUseTheNewRepository() throws Exception {
        var service = CategoryModule.service();
        var created = service.createCategory(f.prefix + "_created", "Synthetic");
        try {
            var duplicate = assertThrows(CategoryException.class,
                    () -> service.createCategory(created.name(), "Synthetic"));
            assertEquals(CategoryException.Reason.DUPLICATE_NAME, duplicate.reason());
            var existing = service.listCategories().stream().filter(c -> c.id() == f.category).findFirst().orElseThrow();
            var conflict = assertThrows(CategoryException.class,
                    () -> service.updateCategory(created.id(), existing.name(), "Synthetic"));
            assertEquals(CategoryException.Reason.DUPLICATE_NAME, conflict.reason());
            assertEquals(created, service.listCategories().stream().filter(c -> c.id() == created.id()).findFirst().orElseThrow());
        } finally { f.update("DELETE FROM category WHERE category_id=?", created.id()); }
    }

    @Test @Tag("original-defect") void concurrentSalesMustNotOversellTheSameStock() throws Exception {
        f.update("UPDATE product SET quantity=10 WHERE product_id=?", f.normal);
        String url = DBAuth.configuration(System.getenv()).url();
        try (StockReadBarrier timing = new StockReadBarrier(url)) {
            List<Boolean> outcomes = parallel(() -> sell(List.of(item(f.normal, 8))), () -> sell(List.of(item(f.normal, 8))));
            assertEquals(2, timing.synchronizedReads(), "Timing seam must observe both real DAO reads.");
            assertEquals(1, outcomes.stream().filter(Boolean::booleanValue).count(), "Only one sale of 8 can consume stock of 10.");
            assertEquals(2, f.stock(f.normal));
        }
    }

    @Test @Tag("original-defect") void concurrentCapacityChecksMustNotAllowWarehouseOverflow() throws Exception {
        CyclicBarrier checked = new CyclicBarrier(2);
        Callable<Boolean> first = capacityInsert("race_a", checked);
        Callable<Boolean> second = capacityInsert("race_b", checked);
        var outcomes = parallel(first, second);
        assertTrue(outcomes.stream().anyMatch(Boolean::booleanValue), "At least one valid capacity request must persist.");
        assertTrue(WarehouseViewDAO.getUsedPalletsForWarehouse(f.emptyWarehouse) <= 3,
                "Both callers can approve 2 pallets against the same empty capacity of 3.");
    }

    private Callable<Boolean> capacityInsert(String name, CyclicBarrier gate) {
        return () -> {
            boolean fits = ProductDAO.canFitInWarehouse(f.emptyWarehouse, 2, null);
            gate.await(10, TimeUnit.SECONDS);
            return fits && ProductDAO.insertProduct(productForm(name, f.emptyWarehouse, 20));
        };
    }

    private List<Boolean> parallel(Callable<Boolean> a, Callable<Boolean> b) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var results = executor.invokeAll(List.of(a, b), 35, TimeUnit.SECONDS);
            return List.of(results.get(0).get(), results.get(1).get());
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(20, TimeUnit.SECONDS)) throw new IllegalStateException("IT workers did not terminate; inspect fixture ownership before cleanup.");
        }
    }

    private boolean sell(List<SalesDAO.OrderItemForm> items) { return SalesDAO.createOrder(f.customer, f.representative, items); }
    private SalesDAO.OrderItemForm item(int product, int quantity) { return new SalesDAO.OrderItemForm(product, quantity); }
    private ProductDAO.ProductFormData productForm(String label, int warehouse, int quantity) {
        var d = new ProductDAO.ProductFormData(); d.categoryId = f.category; d.branchId = f.branch;
        d.warehouseId = warehouse; d.name = f.prefix + "_" + label; d.baseUnit = "CARTON";
        d.cartonsPerPallet = 10; d.quantity = quantity; d.minQuantity = 5; d.maxQuantity = 100;
        d.costPrice = .50; d.sellingPrice = 1.25; return d;
    }
    private ShipmentDAO.ShipmentFormData shipment(int product, int warehouse, int quantity) {
        var d = new ShipmentDAO.ShipmentFormData(); d.productId = product; d.supplierId = f.supplier;
        d.branchId = f.branch; d.warehouseId = warehouse; d.quantity = quantity; d.unitCost = .50;
        d.totalCost = quantity * .50; d.shipmentDate = SyntheticFixture.DATE;
        d.paymentType = "CASH"; d.shippingMethod = "TRUCK"; d.referenceNo = f.prefix + "_import"; return d;
    }
}
