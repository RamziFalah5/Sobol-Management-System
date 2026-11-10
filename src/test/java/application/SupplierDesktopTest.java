package application;

import com.sobol.supplier.application.*;
import com.sobol.supplier.domain.Supplier.Details;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static com.sobol.supplier.application.SupplierException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Supplier FXML and service; only persistence, modal feedback and navigation boundaries are substituted. */
class SupplierDesktopTest {
    @BeforeAll static void startJavaFx() throws Exception {
        assertEquals("false", System.getenv("SOBOL_DB_ENABLED"));
        var ready = new CompletableFuture<Void>();
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.complete(null); });
        ready.get(30, TimeUnit.SECONDS);
    }
    @AfterAll static void stopJavaFx() { Platform.exit(); }
    @FunctionalInterface interface Action { void run() throws Exception; }
    private void onFx(Action action) throws Exception {
        var result = new CompletableFuture<Void>();
        Platform.runLater(() -> {
            try { action.run(); result.complete(null); }
            catch (Throwable failure) { result.completeExceptionally(failure); }
        });
        result.get(30, TimeUnit.SECONDS);
    }
    private static FXMLLoader loader(String file, Object controller) {
        var l = new FXMLLoader(SupplierDesktopTest.class.getResource("/application/" + file));
        l.setControllerFactory(type -> { assertTrue(type.isInstance(controller)); return controller; }); return l;
    }
    private FXMLLoader load(String file, Object controller) throws Exception { var l = loader(file, controller); l.load(); return l; }
    @SuppressWarnings("unchecked") private <T> T node(FXMLLoader l, String id) { return (T) l.getNamespace().get(id); }
    private TextField field(FXMLLoader l, String id) { return node(l, id); }
    private Button button(FXMLLoader l, String text) {
        return ((Parent) l.getRoot()).lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }
    private StackPane attach(FXMLLoader l) {
        var content = new StackPane((Parent) l.getRoot()); content.setId("contentArea");
        new Scene(content); content.applyCss(); content.layout(); return content;
    }
    private void fill(FXMLLoader l, String name) { field(l, "txtName").setText(name); field(l, "txtPhone").setText(" 000 "); }

    @Test void listBindingsAndSearchByNamePhoneAndCityArePreserved() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new ListController(r); var l = load("Suppliers.fxml", c);
        TableView<Supplier> table = node(l, "table");
        assertEquals(List.of(42, 99), table.getItems().stream().map(Supplier::getSupplierId).toList());
        String[] expected = {"First", "000", "first@example.invalid", "Alpha", "Contact"};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], table.getColumns().get(i).getCellObservableValue(0).getValue());
        TextField search = node(l, "txtSearch");
        for (String key : List.of("SECOND", "111", "BETA")) {
            search.setText(key); assertEquals(List.of(99), table.getItems().stream().map(Supplier::getSupplierId).toList());
        }
        search.setText("absent"); assertTrue(table.getItems().isEmpty()); search.clear(); assertEquals(2, table.getItems().size());
        assertEquals(0, r.writes);
    }); }

    @Test void unselectedEditAndViewCannotNavigate() throws Exception { onFx(() -> {
        var c = new ListController(new Memory()); var l = load("Suppliers.fxml", c);
        button(l, "Edit").fire(); assertEquals("Edit Supplier", c.title); assertNull(c.destination);
        button(l, "View").fire(); assertEquals("View Supplier Shipments", c.title); assertNull(c.destination);
    }); }

    @Test void addNavigationSubmitsNormalizedFieldsThroughTheRealService() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new ListController(r); var l = load("Suppliers.fxml", c); var content = attach(l);
        button(l, "Add").fire(); assertSame(c.destination.getRoot(), content.getChildren().getFirst());
        fill(c.destination, " New supplier "); field(c.destination, "txtCity").setText(" City ");
        field(c.destination, "txtEmail").setText(" new@example.invalid "); field(c.destination, "txtAddress").setText(" Address ");
        field(c.destination, "txtContact").setText(" Contact "); ((TextArea) node(c.destination, "txtNotes")).setText(" Notes ");
        button(c.destination, "Add").fire();
        assertEquals(new Details("New supplier", "000", "new@example.invalid", "City", "Address", "Contact", "Notes"), r.rows.get(101).details());
        assertEquals(1, c.add.back); assertEquals(1, r.writes);
    }); }

    @Test void addRequiredFieldsAndEmailValidationStayOnForm() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new Add(r); var l = load("AddSupplier.fxml", c);
        button(l, "Add").fire(); assertEquals("Validation", c.title);
        field(l, "txtName").setText("New"); button(l, "Add").fire(); assertEquals("Validation", c.title);
        fill(l, "New"); field(l, "txtEmail").setText("invalid"); button(l, "Add").fire();
        assertEquals("Validation", c.title); assertEquals(0, r.writes); assertEquals(0, c.back);
    }); }

    @Test void addDuplicateStorageFailureAndCancelHaveDistinctOutcomes() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new Add(r); var l = load("AddSupplier.fxml", c);
        fill(l, "First"); button(l, "Add").fire(); assertEquals("Duplicate", c.title);
        fill(l, "New"); r.failure = STORAGE_FAILURE; button(l, "Add").fire(); assertEquals("Error", c.title);
        assertEquals(2, r.rows.size()); assertEquals(0, c.back);
        int writes = r.writes; button(l, "Cancel").fire(); assertEquals(writes, r.writes); assertEquals(1, c.back);
    }); }

    @Test void selectedEditPrefillsEveryFieldAndSnapshotsIdentity() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new ListController(r); var l = load("Suppliers.fxml", c); var content = attach(l);
        TableView<Supplier> table = node(l, "table"); table.getSelectionModel().selectFirst();
        var selected = table.getSelectionModel().getSelectedItem(); button(l, "Edit").fire();
        var edit = c.destination; assertSame(edit.getRoot(), content.getChildren().getFirst());
        assertEquals("ID: 42", ((Label) node(edit, "lblId")).getText());
        assertEquals("First", field(edit, "txtName").getText()); assertEquals("000", field(edit, "txtPhone").getText());
        assertEquals("first@example.invalid", field(edit, "txtEmail").getText()); assertEquals("Alpha", field(edit, "txtCity").getText());
        assertEquals("Address", field(edit, "txtAddress").getText()); assertEquals("Contact", field(edit, "txtContact").getText());
        assertEquals("Notes", ((TextArea) node(edit, "txtNotes")).getText());
        selected.setSupplierId(99); ((Label) node(edit, "lblId")).setText("ID: 99");
        field(edit, "txtName").setText(" Updated "); button(edit, "Save").fire();
        assertEquals(42, r.updatedId); assertEquals("Updated", r.rows.get(42).details().name());
        assertEquals("Second", r.rows.get(99).details().name()); assertEquals(1, c.edit.back);
    }); }

    @Test void editWithoutAValidSelectionCannotWriteAndInvalidResetClearsItsTarget() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new Edit(r); var l = load("EditSupplier.fxml", c); fill(l, "First");
        button(l, "Save").fire(); assertEquals("Validation", c.title); assertEquals(0, r.writes);
        c.setSupplier(Supplier.fromDomain(r.rows.get(42)));
        assertThrows(IllegalArgumentException.class, () -> c.setSupplier(null));
        button(l, "Save").fire(); assertEquals("Validation", c.title); assertEquals(0, r.writes);
        assertThrows(IllegalArgumentException.class, () -> c.setSupplier(new Supplier()));
    }); }

    @Test void nullOptionalFieldsPrefillAndUnchangedExistingSupplierSaves() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new Edit(r); var l = load("EditSupplier.fxml", c);
        c.setSupplier(Supplier.fromDomain(r.rows.get(99)));
        for (String id : List.of("txtEmail", "txtAddress", "txtContact")) assertEquals("", field(l, id).getText());
        assertEquals("", ((TextArea) node(l, "txtNotes")).getText());
        var before = r.rows.get(99); button(l, "Save").fire(); assertEquals(before, r.rows.get(99));
        assertEquals(1, c.back); assertNull(c.title);
    }); }

    @Test void deletedTargetDuplicateAndStorageFailureDoNotNavigateOrOverwrite() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new Edit(r); var l = load("EditSupplier.fxml", c);
        c.setSupplier(Supplier.fromDomain(r.rows.get(42)));
        field(l, "txtName").setText("Second"); button(l, "Save").fire(); assertEquals("Duplicate", c.title);
        field(l, "txtName").setText("Changed"); r.failure = STORAGE_FAILURE;
        button(l, "Save").fire(); assertEquals("Error", c.title); assertEquals("First", r.rows.get(42).details().name());
        r.failure = null; r.rows.remove(42); button(l, "Save").fire(); assertEquals("Not Found", c.title);
        assertEquals("Second", r.rows.get(99).details().name()); assertEquals(0, c.back);
        int writes = r.writes; button(l, "Cancel").fire(); assertEquals(writes, r.writes); assertEquals(1, c.back);
    }); }

    @Test void editValidationRetainsOriginalDataWithoutCallingRepository() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new Edit(r); var l = load("EditSupplier.fxml", c); c.setSupplier(Supplier.fromDomain(r.rows.get(42)));
        for (String id : List.of("txtName", "txtPhone", "txtEmail", "txtCity", "txtAddress", "txtContact")) {
            String previous = field(l, id).getText();
            field(l, id).setText(id.equals("txtEmail") ? "invalid" : id.equals("txtName") || id.equals("txtPhone") ? " " : "x".repeat(256));
            button(l, "Save").fire(); assertEquals("Validation", c.title); assertEquals(0, r.writes);
            field(l, id).setText(previous);
        }
        ((TextArea) node(l, "txtNotes")).setText("x".repeat(401)); button(l, "Save").fire();
        assertEquals("Validation", c.title); assertEquals(0, r.writes); assertEquals(0, c.back);
    }); }

    @Test void listFailureIsVisibleAndCannotOfferAStaleSelection() throws Exception { onFx(() -> {
        var r = new Memory(); r.failure = STORAGE_FAILURE; var c = new ListController(r); var l = load("Suppliers.fxml", c);
        assertEquals("Error", c.title); assertTrue(((TableView<?>) node(l, "table")).getItems().isEmpty());
        button(l, "Edit").fire(); assertNull(c.destination);
    }); }

    @Test void selectedSupplierShipmentsPreserveIdBindingsTotalsAndBackNavigation() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new ListController(r); var l = load("Suppliers.fxml", c); var content = attach(l);
        TableView<Supplier> suppliers = node(l, "table"); suppliers.getSelectionModel().selectFirst();
        c.view.duringRead = () -> suppliers.getItems().getFirst().setSupplierId(99);
        button(l, "View").fire(); assertNull(c.destination); assertEquals("PostgreSQL transition", c.title);
        assertSame(l.getRoot(), content.getChildren().getFirst());
        // Retain the isolated legacy view's identity/mapping contract without routing PostgreSQL IDs into it.
        var selected = Supplier.fromDomain(r.rows.get(42));
        var view = load("ViewSupplierShipments.fxml", c.view); content.getChildren().setAll((Parent) view.getRoot());
        c.view.setSupplier(selected);
        assertEquals("Supplier Shipments - First", ((Label) node(view, "lblTitle")).getText());
        assertEquals(List.of(42), c.view.rowIds); assertEquals(List.of(42), c.view.totalIds);
        assertEquals("Shipments: 1", ((Label) node(view, "lblCount")).getText());
        assertEquals(String.format("Total Cost: %.2f", 6.0), ((Label) node(view, "lblTotal")).getText());
        TableView<Shipment> table = node(view, "table"); assertSame(c.view.rows.getFirst(), table.getItems().getFirst());
        Object[] expected = {7, "Synthetic product", "Synthetic branch", "Synthetic warehouse", 3, 2.0, 6.0,
                LocalDate.of(2026, 1, 15), "CASH", "TRUCK", "SYNTHETIC"};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], table.getColumns().get(i).getCellObservableValue(0).getValue());
        button(view, "Back").fire(); assertSame(c.view.backLoader.getRoot(), content.getChildren().getFirst());
    }); }

    @Test void emptySupplierShipmentListIsValidAndInvalidSelectionIsRejected() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new View(r); c.rows = List.of(); var l = load("ViewSupplierShipments.fxml", c);
        assertThrows(IllegalArgumentException.class, () -> c.setSupplier(null));
        assertThrows(IllegalArgumentException.class, () -> c.setSupplier(new Supplier()));
        assertTrue(c.rowIds.isEmpty()); c.setSupplier(Supplier.fromDomain(r.rows.get(99)));
        assertEquals("Shipments: 0", ((Label) node(l, "lblCount")).getText());
        assertEquals(String.format("Total Cost: %.2f", 0.0), ((Label) node(l, "lblTotal")).getText());
        assertEquals(List.of(99), c.rowIds); assertEquals(List.of(99), c.totalIds);
    }); }

    @Test void shipmentSupplierLookupMapsValidIdsAndAllowsSelection() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new ShipmentLookup(r); var l = load("AddShipment.fxml", c);
        ComboBox<Supplier> suppliers = node(l, "cbSupplier");
        assertEquals(List.of(42, 99), suppliers.getItems().stream().map(Supplier::getSupplierId).toList());
        assertEquals(42, suppliers.getValue().getSupplierId()); assertEquals("First", suppliers.getValue().toString());
        suppliers.getSelectionModel().select(1); assertEquals(99, suppliers.getValue().getSupplierId());
        assertEquals("111", suppliers.getValue().getPhone()); assertEquals(0, r.writes); assertNull(c.title);
        assertNull(l.getNamespace().get("cbMethod")); // Existing unrelated shipment form defect is preserved.
    }); }

    @Test void shipmentLookupEmptyAndStorageFailureClearStaleValuesAndRemainDistinct() throws Exception { onFx(() -> {
        var r = new Memory(); var c = new ShipmentLookup(r); var l = load("AddShipment.fxml", c);
        ComboBox<Supplier> suppliers = node(l, "cbSupplier"); assertNotNull(suppliers.getValue());
        r.failure = STORAGE_FAILURE; c.loadSuppliers();
        assertEquals("Error", c.title); assertTrue(suppliers.getItems().isEmpty()); assertNull(suppliers.getValue()); assertNull(c.target);
        r.failure = null; r.rows.clear(); c.loadSuppliers();
        assertEquals("Suppliers Required", c.title); assertEquals("Imports.fxml", c.target); assertNull(suppliers.getValue());
        assertEquals(0, r.writes);
    }); }

    private static class Memory implements SupplierRepository {
        final Map<Integer, com.sobol.supplier.domain.Supplier> rows = new LinkedHashMap<>();
        int sequence = 100, writes, updatedId; SupplierException.Reason failure;
        Memory() {
            rows.put(42, new com.sobol.supplier.domain.Supplier(42, new Details("First", "000", "first@example.invalid", "Alpha", "Address", "Contact", "Notes")));
            rows.put(99, new com.sobol.supplier.domain.Supplier(99, new Details("Second", "111", null, "Beta", null, null, null)));
        }
        private void available() { if (failure != null) throw new SupplierException(failure); }
        private void unique(Details d, int id) {
            if (rows.values().stream().anyMatch(s -> s.id() != id && s.details().name().equals(d.name()))) throw new SupplierException(DUPLICATE_NAME);
        }
        public List<com.sobol.supplier.domain.Supplier> findAll() { available(); return new ArrayList<>(rows.values()); }
        public com.sobol.supplier.domain.Supplier insert(Details d) {
            writes++; available(); unique(d, 0); var s = new com.sobol.supplier.domain.Supplier(++sequence, d); rows.put(s.id(), s); return s;
        }
        public Optional<com.sobol.supplier.domain.Supplier> update(int id, Details d) {
            writes++; updatedId = id; available(); if (!rows.containsKey(id)) return Optional.empty();
            unique(d, id); var s = new com.sobol.supplier.domain.Supplier(id, d); rows.put(id, s); return Optional.of(s);
        }
    }
    private static class Add extends AddSupplierController {
        int back; String title;
        Add(Memory r) { super(new SupplierService(r)); }
        @Override void alert(String t, String message) { title = t; }
        @Override void goBack() { back++; }
    }
    private static class Edit extends EditSupplierController {
        int back; String title;
        Edit(Memory r) { super(new SupplierService(r)); }
        @Override void alert(String t, String message) { title = t; }
        @Override void goBack() { back++; }
    }
    private static class ListController extends SuppliersController {
        final Add add; final Edit edit; final View view; FXMLLoader destination; String title;
        ListController(Memory r) { super(new SupplierService(r)); add = new Add(r); edit = new Edit(r); view = new View(r); }
        @Override void alert(String t, String message) { title = t; }
        @Override FXMLLoader createViewLoader(String file) {
            Object controller = switch (file) {
                case "AddSupplier.fxml" -> add;
                case "EditSupplier.fxml" -> edit;
                case "ViewSupplierShipments.fxml" -> view;
                default -> throw new AssertionError("Unexpected supplier navigation");
            };
            destination = loader(file, controller); return destination;
        }
    }
    private static class View extends ViewSupplierShipmentsController {
        final Memory repository; final List<Integer> rowIds = new ArrayList<>(), totalIds = new ArrayList<>();
        List<Shipment> rows = List.of(new Shipment(7, 11, 42, 21, 31, "Synthetic product", "First", "Synthetic branch", "Synthetic warehouse",
                3, 2, 6, LocalDate.of(2026, 1, 15), "CASH", "TRUCK", "SYNTHETIC", null));
        Runnable duringRead; FXMLLoader backLoader;
        View(Memory r) { repository = r; }
        @Override List<Shipment> loadShipments(int id) { rowIds.add(id); if (duringRead != null) duringRead.run(); return rows; }
        @Override double loadTotalCost(int id) { totalIds.add(id); return rows.stream().mapToDouble(Shipment::getTotalCost).sum(); }
        @Override FXMLLoader createViewLoader(String file) { assertEquals("Suppliers.fxml", file); backLoader = loader(file, new ListController(repository)); return backLoader; }
    }
    private static class ShipmentLookup extends AddShipmentController {
        String title, target;
        ShipmentLookup(Memory r) { super(new SupplierService(r)); }
        // Isolate the real Supplier loader from unchanged static Branch/Warehouse DAO initialization.
        @Override public void initialize() { loadSuppliers(); }
        @Override void alert(String t, String message) { title = t; }
        @Override void go(String file) { target = file; }
    }
}
