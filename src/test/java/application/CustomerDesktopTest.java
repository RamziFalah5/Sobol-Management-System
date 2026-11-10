package application;

import com.sobol.customer.application.*;
import com.sobol.customer.domain.Customer.Details;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import static com.sobol.customer.application.CustomerException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual FXML/controllers/services; synthetic repositories and narrow legacy Sales/UI delegates only. */
class CustomerDesktopTest {
    @BeforeAll static void start() throws Exception {
        assertEquals("false", System.getenv("SOBOL_DB_ENABLED"));
        var ready = new CompletableFuture<Void>();
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.complete(null); }); ready.get(30, TimeUnit.SECONDS);
    }
    @AfterAll static void stop() { Platform.exit(); }
    @FunctionalInterface interface Action { void run() throws Exception; }
    private void onFx(Action a) throws Exception {
        var result = new CompletableFuture<Void>();
        Platform.runLater(() -> { try { a.run(); result.complete(null); } catch (Throwable t) { result.completeExceptionally(t); } });
        result.get(30, TimeUnit.SECONDS);
    }
    @SuppressWarnings("unchecked") private static <T> T node(FXMLLoader l, String id) { return (T) l.getNamespace().get(id); }
    private static TextField field(FXMLLoader l, String id) { return node(l, id); }
    private static TableView<Customer> customers(FXMLLoader l) { return node(l, "table"); }
    private static Button button(FXMLLoader l, String text) {
        return ((Parent) l.getRoot()).lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }
    private static void fill(FXMLLoader l, String name) { field(l, "txtName").setText(name); field(l, "txtPhone").setText(" 000 "); }
    private static Customer select(FXMLLoader l, int id) {
        var row = customers(l).getItems().stream().filter(c -> c.getCustomerId() == id).findFirst().orElseThrow();
        customers(l).getSelectionModel().select(row); return row;
    }

    @Test void listsInRepositoryOrderWithAllColumnMappingsAndSearchFields() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("Customers.fxml");
        assertEquals(List.of(99, 42), customers(l).getItems().stream().map(Customer::getCustomerId).toList());
        Object[] expected = {"Second", "111", null, "Beta", null};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], customers(l).getColumns().get(i).getCellObservableValue(0).getValue());
        for (String key : List.of("FIRST", "000", "FIRST@EXAMPLE")) {
            field(l, "txtSearch").setText(key); assertEquals(List.of(42), customers(l).getItems().stream().map(Customer::getCustomerId).toList());
        }
        field(l, "txtSearch").setText("absent"); assertTrue(customers(l).getItems().isEmpty());
        field(l, "txtSearch").clear(); assertEquals(2, customers(l).getItems().size()); assertEquals(0, h.r.writes);
        assertTrue(button(l, "View").isDisabled()); // Original unavailable action is not invented.
    }); }

    @Test void editRequiresACurrentValidSelection() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("Customers.fxml"); var c = (ListScreen) l.getController();
        button(l, "Edit").fire(); assertEquals("Edit", c.title); assertFalse(h.views.containsKey("EditCustomer.fxml"));
        select(l, 42).setCustomerId(0); button(l, "Edit").fire(); assertFalse(h.views.containsKey("EditCustomer.fxml"));
    }); }

    @Test void addNavigationCreatesNormalizedCustomerAndReloadsActualList() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("Customers.fxml"); button(l, "Add").fire();
        var add = h.current("AddCustomer.fxml"); h.assertVisible(add); fill(add, " New ");
        field(add, "txtEmail").setText(" new@example.invalid "); field(add, "txtCity").setText(" City ");
        field(add, "txtAddress").setText(" Address "); ((TextArea) node(add, "txtNotes")).setText(" Notes ");
        button(add, "Add").fire(); var back = h.current("Customers.fxml"); h.assertVisible(back);
        assertEquals(new Details("New", "000", "new@example.invalid", "City", "Address", "Notes"), h.r.rows.get(101).details());
        assertEquals(List.of(101, 99, 42), customers(back).getItems().stream().map(Customer::getCustomerId).toList());
        assertEquals(1, h.r.writes);
    }); }

    @Test void addValidationAndEmailConflictProduceDistinctFeedback() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("AddCustomer.fxml"); var c = (Add) l.getController();
        button(l, "Add").fire(); assertEquals("Validation", c.title);
        field(l, "txtName").setText("New"); button(l, "Add").fire(); assertEquals("Validation", c.title);
        fill(l, "New"); field(l, "txtEmail").setText("invalid"); button(l, "Add").fire(); assertEquals("Validation", c.title); assertEquals(0, h.r.writes);
        fill(l, "New"); field(l, "txtEmail").setText("first@example.invalid"); button(l, "Add").fire();
        assertEquals("Duplicate", c.title); assertTrue(c.message.contains("email")); assertEquals(2, h.r.rows.size()); h.assertVisible(l);
    }); }

    @Test void addPreservesPermissiveEmailAndOptionalBlankStorage() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("AddCustomer.fxml"); fill(l, "Permissive"); field(l, "txtEmail").setText("@.");
        button(l, "Add").fire(); assertEquals(new Details("Permissive", "000", "@.", "", "", ""), h.r.rows.get(101).details());
    }); }

    @Test void addFailureStaysOnFormAndCancelReturnsWithoutAnotherWrite() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("AddCustomer.fxml"); var c = (Add) l.getController(); fill(l, "New"); h.r.failure = STORAGE_FAILURE;
        button(l, "Add").fire(); assertEquals("Error", c.title); h.assertVisible(l); assertEquals(2, h.r.rows.size());
        h.r.failure = null; int writes = h.r.writes; button(l, "Cancel").fire();
        h.assertVisible(h.current("Customers.fxml")); assertEquals(writes, h.r.writes);
    }); }

    @Test void editPrefillSnapshotsIdAndDoesNotRetargetAfterViewOrSelectionMutation() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("Customers.fxml"); var original = select(l, 42); button(l, "Edit").fire();
        var edit = h.current("EditCustomer.fxml"); h.assertVisible(edit);
        assertEquals("ID: 42", ((Label) node(edit, "lblId")).getText());
        assertEquals("First", field(edit, "txtName").getText()); assertEquals("000", field(edit, "txtPhone").getText());
        assertEquals("first@example.invalid", field(edit, "txtEmail").getText()); assertEquals("Alpha", field(edit, "txtCity").getText());
        assertEquals("Address", field(edit, "txtAddress").getText()); assertEquals("Notes", ((TextArea) node(edit, "txtNotes")).getText());
        original.setCustomerId(99); customers(l).getSelectionModel().selectFirst(); ((Label) node(edit, "lblId")).setText("ID: 99");
        field(edit, "txtName").setText(" Updated "); button(edit, "Save").fire();
        assertEquals(42, h.r.updatedId); assertEquals("Updated", h.r.rows.get(42).details().fullName()); assertEquals("Second", h.r.rows.get(99).details().fullName());
        h.assertVisible(h.current("Customers.fxml"));
    }); }

    @Test void editInvalidInitializationCannotReuseAnEarlierTarget() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("EditCustomer.fxml"); var c = (Edit) l.getController(); fill(l, "First");
        button(l, "Save").fire(); assertEquals("Validation", c.title); assertEquals(0, h.r.writes);
        c.setCustomer(Customer.fromDomain(h.r.rows.get(42))); assertThrows(IllegalArgumentException.class, () -> c.setCustomer(null));
        button(l, "Save").fire(); assertEquals(0, h.r.writes);
        assertThrows(IllegalArgumentException.class, () -> c.setCustomer(new Customer()));
    }); }

    @Test void unchangedEditSucceedsAndNullableFieldsHaveSafePrefill() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("EditCustomer.fxml"); var c = (Edit) l.getController();
        c.setCustomer(Customer.fromDomain(h.r.rows.get(42))); var before = h.r.rows.get(42); button(l, "Save").fire();
        assertEquals(before, h.r.rows.get(42)); h.assertVisible(h.current("Customers.fxml"));
        var other = h.open("EditCustomer.fxml"); ((Edit) other.getController()).setCustomer(Customer.fromDomain(h.r.rows.get(99)));
        assertEquals("", field(other, "txtEmail").getText()); assertEquals("", field(other, "txtAddress").getText()); assertEquals("", ((TextArea) node(other, "txtNotes")).getText());
    }); }

    @Test void editEmailMissingAndStorageFailuresNeverNavigateOrOverwrite() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("EditCustomer.fxml"); var c = (Edit) l.getController(); c.setCustomer(Customer.fromDomain(h.r.rows.get(99)));
        field(l, "txtName").setText("Second"); field(l, "txtEmail").setText("first@example.invalid"); button(l, "Save").fire(); assertTrue(c.message.contains("email"));
        field(l, "txtEmail").clear(); h.r.failure = STORAGE_FAILURE; button(l, "Save").fire(); assertEquals("Error", c.title);
        assertEquals("Second", h.r.rows.get(99).details().fullName()); h.r.failure = null; h.r.rows.remove(99);
        button(l, "Save").fire(); assertEquals("Not Found", c.title); h.assertVisible(l); assertEquals("First", h.r.rows.get(42).details().fullName());
        int writes = h.r.writes; button(l, "Cancel").fire(); h.assertVisible(h.current("Customers.fxml")); assertEquals(writes, h.r.writes);
    }); }

    @Test void editChecksRequiredFormatAndSchemaLengthsBeforePersistence() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("EditCustomer.fxml"); var c = (Edit) l.getController(); c.setCustomer(Customer.fromDomain(h.r.rows.get(42)));
        for (String id : List.of("txtName", "txtPhone", "txtEmail", "txtCity", "txtAddress")) {
            String old = field(l, id).getText(); field(l, id).setText(id.equals("txtEmail") ? "invalid" : id.equals("txtName") || id.equals("txtPhone") ? " " : "x".repeat(221));
            button(l, "Save").fire(); assertEquals("Validation", c.title); assertEquals(0, h.r.writes); field(l, id).setText(old);
        }
        ((TextArea) node(l, "txtNotes")).setText("x".repeat(401)); button(l, "Save").fire(); assertEquals("Validation", c.title); assertEquals(0, h.r.writes);
    }); }

    @Test void listAndSalePickerReadFailureAreVisibleAndOfferNoSelection() throws Exception { onFx(() -> {
        var h = new Harness(); h.r.failure = STORAGE_FAILURE;
        var list = h.open("Customers.fxml"); assertEquals("Error", ((ListScreen) list.getController()).title); assertTrue(customers(list).getItems().isEmpty());
        var picker = h.open("PickCustomerForSale.fxml"); var c = (Picker) picker.getController();
        assertEquals("Error", c.title); assertTrue(customers(picker).getItems().isEmpty());
        button(picker, "Select Customer").fire(); assertEquals("Select Customer", c.title); assertFalse(h.views.containsKey("PickProductsForSale.fxml"));
    }); }

    @Test void pickerSearchBindingsAndSelectionValidationUseCurrentRows() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("PickCustomerForSale.fxml"); var c = (Picker) l.getController();
        button(l, "Select Customer").fire(); assertEquals("Select Customer", c.title);
        for (String key : List.of("FIRST", "000", "FIRST@EXAMPLE")) { field(l, "txtSearch").setText(key); assertEquals(42, customers(l).getItems().getFirst().getCustomerId()); assertEquals(1, customers(l).getItems().size()); }
        assertEquals("first@example.invalid", customers(l).getColumns().get(2).getCellObservableValue(0).getValue());
        select(l, 42).setCustomerId(0); button(l, "Select Customer").fire(); assertFalse(h.views.containsKey("PickProductsForSale.fxml"));
        field(l, "txtSearch").setText("none"); assertTrue(customers(l).getItems().isEmpty()); field(l, "txtSearch").clear(); assertEquals(2, customers(l).getItems().size());
    }); }

    @Test void customerListToExistingSalesEntryAndPickerPreservesTheActualChosenIdentity() throws Exception { onFx(() -> {
        var h = new Harness(); var list = h.open("Customers.fxml"); var stale = select(list, 42);
        // Existing app navigation switches to Sales; Customers has no direct start-sale action.
        var sales = h.open("Sales.fxml"); button(sales, "New Order").fire(); var picker = h.current("PickCustomerForSale.fxml"); h.assertVisible(picker);
        assertNull(customers(picker).getSelectionModel().getSelectedItem()); var chosen = select(picker, 99);
        h.beforeProductsLoad = () -> { chosen.setCustomerId(42); chosen.setFullName("Mutated during navigation"); customers(picker).getSelectionModel().clearSelection(); stale.setCustomerId(7); };
        button(picker, "Select Customer").fire(); var products = h.current("PickProductsForSale.fxml"); h.assertVisible(products);
        var c = (Products) products.getController(); assertEquals(99, c.received.getCustomerId()); assertEquals("Second", c.received.getFullName());
        c.received.setCustomerId(7); c.received.setFullName("Mutated after setter");
        assertEquals("Pick Products - Customer: Second", ((Label) node(products, "lblTitle")).getText());
        TableView<ProductSalePickRow> table = node(products, "table"); table.getItems().getFirst().setSelected(true);
        button(products, "Select Quantity").fire(); var quantities = h.current("SaleQuantities.fxml"); h.assertVisible(quantities);
        var q = (Quantities) quantities.getController(); assertEquals(99, q.received.getCustomerId()); assertEquals("Second", q.received.getFullName());
        assertEquals(11, q.picked.getFirst().getProductId()); assertEquals(0, h.r.writes);
    }); }

    @Test void invalidProductCustomerResetCannotReuseAnEarlierSaleIdentity() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("PickProductsForSale.fxml"); var c = (Products) l.getController();
        c.setCustomer(Customer.fromDomain(h.r.rows.get(42)));
        assertThrows(IllegalArgumentException.class, () -> c.setCustomer(new Customer()));
        button(l, "Select Quantity").fire(); assertEquals("Error", c.title); assertFalse(h.views.containsKey("SaleQuantities.fxml"));
        assertThrows(IllegalArgumentException.class, () -> c.setCustomer(null));
    }); }

    @Test void productsRequireAProductAndBackReturnsToFreshPickerThenSales() throws Exception { onFx(() -> {
        var h = new Harness(); var l = h.open("PickProductsForSale.fxml"); var c = (Products) l.getController(); c.setCustomer(Customer.fromDomain(h.r.rows.get(42)));
        button(l, "Select Quantity").fire(); assertEquals("Pick Products", c.title); assertFalse(h.views.containsKey("SaleQuantities.fxml"));
        button(l, "Back").fire(); var picker = h.current("PickCustomerForSale.fxml"); h.assertVisible(picker);
        assertNull(customers(picker).getSelectionModel().getSelectedItem()); button(picker, "Back").fire(); h.assertVisible(h.current("Sales.fxml"));
    }); }

    @Test void sameNameCustomersCanBeAddedAndEditedWithoutChangingAnotherIdentity() throws Exception { onFx(() -> {
        var h = new Harness(); var add = h.open("AddCustomer.fxml"); fill(add, "First");
        button(add, "Add").fire();
        assertEquals("First", h.r.rows.get(101).details().fullName()); assertEquals("First", h.r.rows.get(42).details().fullName());
        var list = h.current("Customers.fxml"); select(list, 101); button(list, "Edit").fire();
        var edit = h.current("EditCustomer.fxml"); field(edit, "txtPhone").setText("222"); button(edit, "Save").fire();
        assertEquals(101, h.r.updatedId); assertEquals("222", h.r.rows.get(101).details().phone());
        assertEquals("000", h.r.rows.get(42).details().phone());
        var picker = h.open("PickCustomerForSale.fxml"); var selected = select(picker, 101);
        assertEquals("First", selected.getFullName()); assertEquals(101, selected.getCustomerId());
    }); }

    private static class Memory implements CustomerRepository {
        final Map<Integer, com.sobol.customer.domain.Customer> rows = new LinkedHashMap<>();
        int sequence = 100, writes, updatedId; CustomerException.Reason failure;
        Memory() {
            rows.put(42, new com.sobol.customer.domain.Customer(42, new Details("First", "000", "first@example.invalid", "Alpha", "Address", "Notes")));
            rows.put(99, new com.sobol.customer.domain.Customer(99, new Details("Second", "111", null, "Beta", null, null)));
        }
        private void available() { if (failure != null) throw new CustomerException(failure); }
        private void unique(Details d, int id) {
            for (var row : rows.values()) if (row.id() != id) {
                if (d.email() != null && d.email().equals(row.details().email())) throw new CustomerException(DUPLICATE_EMAIL);
            }
        }
        public List<com.sobol.customer.domain.Customer> findAll() { available(); return rows.values().stream().sorted(Comparator.comparingInt(com.sobol.customer.domain.Customer::id).reversed()).toList(); }
        public com.sobol.customer.domain.Customer insert(Details d) { writes++; available(); unique(d, 0); var c = new com.sobol.customer.domain.Customer(++sequence, d); rows.put(c.id(), c); return c; }
        public Optional<com.sobol.customer.domain.Customer> update(int id, Details d) {
            writes++; updatedId = id; available(); if (!rows.containsKey(id)) return Optional.empty(); unique(d, id);
            var c = new com.sobol.customer.domain.Customer(id, d); rows.put(id, c); return Optional.of(c);
        }
    }
    private static class Harness {
        final Memory r = new Memory(); final CustomerService service = new CustomerService(r);
        final Map<String, FXMLLoader> views = new HashMap<>(); final StackPane content = new StackPane(); Runnable beforeProductsLoad;
        Harness() { content.setId("contentArea"); new Scene(content); }
        FXMLLoader loader(String file) {
            if (file.equals("PickProductsForSale.fxml") && beforeProductsLoad != null) { beforeProductsLoad.run(); beforeProductsLoad = null; }
            Object controller = switch (file) {
                case "Customers.fxml" -> new ListScreen(this);
                case "AddCustomer.fxml" -> new Add(this);
                case "EditCustomer.fxml" -> new Edit(this);
                case "PickCustomerForSale.fxml" -> new Picker(this);
                case "PickProductsForSale.fxml" -> new Products(this);
                case "Sales.fxml" -> new Sales(this);
                case "SaleQuantities.fxml" -> new Quantities();
                default -> throw new AssertionError("Unexpected customer navigation");
            };
            var l = new FXMLLoader(getClass().getResource("/application/" + file));
            l.setControllerFactory(type -> { assertTrue(type.isInstance(controller)); return controller; }); views.put(file, l); return l;
        }
        FXMLLoader open(String file) throws Exception { var l = loader(file); Parent root = l.load(); content.getChildren().setAll(root); content.applyCss(); content.layout(); return l; }
        FXMLLoader current(String file) { return Objects.requireNonNull(views.get(file)); }
        void assertVisible(FXMLLoader l) { assertSame(l.getRoot(), content.getChildren().getFirst()); }
    }
    private static class ListScreen extends CustomersController {
        final Harness h; String title;
        ListScreen(Harness h) { super(h.service); this.h = h; }
        @Override void alert(String t, String message) { title = t; }
        @Override FXMLLoader createViewLoader(String file) { return h.loader(file); }
    }
    private static class Add extends AddCustomerController {
        final Harness h; String title, message;
        Add(Harness h) { super(h.service); this.h = h; }
        @Override void alert(String t, String m) { title = t; message = m; }
        @Override FXMLLoader createViewLoader(String file) { return h.loader(file); }
    }
    private static class Edit extends EditCustomerController {
        final Harness h; String title, message;
        Edit(Harness h) { super(h.service); this.h = h; }
        @Override void alert(String t, String m) { title = t; message = m; }
        @Override FXMLLoader createViewLoader(String file) { return h.loader(file); }
    }
    private static class Picker extends PickCustomerForSaleController {
        final Harness h; String title;
        Picker(Harness h) { super(h.service); this.h = h; }
        @Override void alert(String t, String m) { title = t; }
        @Override FXMLLoader createViewLoader(String file) { return h.loader(file); }
    }
    private static class Products extends PickProductsForSaleController {
        final Harness h; Customer received; String title;
        Products(Harness h) { this.h = h; }
        @Override public void setCustomer(Customer c) { received = c; super.setCustomer(c); }
        @Override List<ProductSalePickRow> loadProducts() { return List.of(new ProductSalePickRow(11, "Synthetic product", 20, 1.25)); }
        @Override void alert(String t, String m) { title = t; }
        @Override FXMLLoader createViewLoader(String file) { return h.loader(file); }
    }
    private static class Sales extends SalesController {
        final Harness h; Sales(Harness h) { this.h = h; }
        @Override List<SalesOrder> loadOrders() { return List.of(); }
        @Override FXMLLoader createViewLoader(String file) { return h.loader(file); }
    }
    private static class Quantities extends SaleQuantitiesController {
        Customer received; List<ProductSalePickRow> picked;
        // Observe the existing sales boundary without employee lookup, financial work or a database write.
        @Override public void setData(Customer c, List<ProductSalePickRow> selected) { received = c; picked = List.copyOf(selected); }
    }
}
