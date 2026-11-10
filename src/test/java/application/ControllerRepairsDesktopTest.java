package application;

import com.sobol.category.application.CategoryException;
import com.sobol.category.application.CategoryRepository;
import com.sobol.category.application.CategoryService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real FXML/JavaFX controls with synthetic, overridable DAO boundaries. No JDBC connection. */
class ControllerRepairsDesktopTest {
    @BeforeAll static void startJavaFx() throws Exception {
        assertEquals("false", System.getenv("SOBOL_DB_ENABLED"));
        var ready = new CompletableFuture<Void>();
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.complete(null); });
        ready.get(30, TimeUnit.SECONDS);
    }
    @AfterAll static void stopJavaFx() { Platform.exit(); }

    @FunctionalInterface interface FxAction { void run() throws Exception; }
    private void onFx(FxAction action) throws Exception {
        var result = new CompletableFuture<Void>();
        Platform.runLater(() -> {
            try { CategorySession.clear(); action.run(); result.complete(null); }
            catch (Throwable failure) { result.completeExceptionally(failure); }
            finally { CategorySession.clear(); }
        });
        result.get(30, TimeUnit.SECONDS);
    }

    private FXMLLoader load(String resource, Object controller) throws Exception {
        var loader = new FXMLLoader(getClass().getResource("/application/" + resource));
        loader.setControllerFactory(type -> {
            assertTrue(type.isInstance(controller), "Unexpected FXML controller");
            return controller;
        });
        loader.load(); return loader;
    }

    private Button button(Parent root, String text) {
        return root.lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }
    @SuppressWarnings("unchecked")
    private <T> T node(FXMLLoader loader, String id) { return (T) loader.getNamespace().get(id); }

    @Test void dailyScreenRestoresExistingStatesAndPreservesDefaultPresent() throws Exception { onFx(() -> {
        var controller = new DailyStub();
        var loader = load("DailyAttendance.fxml", controller);
        TableView<DailyAttendanceRow> table = node(loader, "table");
        assertSame(controller, loader.getController());
        assertEquals(List.of(101, 102, 103), table.getItems().stream().map(DailyAttendanceRow::getEmployeeId).toList());
        assertEquals(List.of(false, true, true), table.getItems().stream().map(DailyAttendanceRow::isPresent).toList());
        assertEquals("Date: " + controller.loadedDate, ((Label) node(loader, "lblDate")).getText());
        assertEquals(3, controller.presenceLookups);
    }); }

    @Test void dailyCheckboxEditsAreSavedForTheLoadedDate() throws Exception { onFx(() -> {
        var controller = new DailyStub();
        var loader = load("DailyAttendance.fxml", controller);
        Parent root = loader.getRoot();
        TableView<DailyAttendanceRow> table = node(loader, "table");
        Stage stage = new Stage();
        try {
            stage.setScene(new Scene(root)); stage.show(); root.applyCss(); root.layout();
            CheckBox checkbox = table.lookupAll(".check-box").stream().filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast).filter(box -> rowIndex(box) == 2).findFirst().orElseThrow();
            assertTrue(checkbox.isSelected()); checkbox.fire();
            assertFalse(table.getItems().get(2).isPresent());
            ((Button) node(loader, "btnSave")).fire();
            assertEquals(controller.loadedDate, controller.savedDate);
            assertEquals(List.of(false, true, false), controller.saved.stream().map(DailyAttendanceRow::isPresent).toList());
            assertEquals(List.of(101, 102, 103), controller.saved.stream().map(DailyAttendanceRow::getEmployeeId).toList());
            assertEquals("Saved", controller.alertTitle); assertEquals(1, controller.backCalls);
        } finally { stage.close(); }
    }); }

    private static int rowIndex(Node node) {
        for (Node p = node; p != null; p = p.getParent()) if (p instanceof TableRow<?> row) return row.getIndex();
        return -1;
    }

    @Test void dailyFailureStaysOnScreenAndCancelDoesNotSave() throws Exception { onFx(() -> {
        var controller = new DailyStub(); controller.saveResult = false;
        var loader = load("DailyAttendance.fxml", controller);
        ((Button) node(loader, "btnSave")).fire();
        assertEquals("Error", controller.alertTitle); assertEquals(0, controller.backCalls);
        assertEquals(1, controller.saveCalls);
        button(loader.getRoot(), "Cancel").fire();
        assertEquals(1, controller.backCalls); assertEquals(1, controller.saveCalls);
    }); }

    @Test void categorySelectionReachesEditorAndRetainsItsOwnIdOnSave() throws Exception { onFx(() -> {
        Category selected = new Category(42, "Synthetic category", "Original description");
        var controller = new CategoriesStub(List.of(selected));
        var loader = load("Category.fxml", controller);
        Parent categoryView = loader.getRoot();
        StackPane content = new StackPane(categoryView); content.setId("contentArea");
        Scene scene = new Scene(content);
        content.applyCss(); content.layout(); // Attach the ScrollPane content through its real skin.
        assertSame(scene, ((Node) node(loader, "categoryGrid")).getScene());
        GridPaneAccess.selectFirstCard(loader);
        button(categoryView, "Edit").fire();
        assertNotSame(categoryView, content.getChildren().getFirst());
        var editorLoader = controller.editorLoader;
        assertNotNull(editorLoader);
        Label id = node(editorLoader, "lblCategoryId");
        TextField name = node(editorLoader, "txtName"); TextArea description = node(editorLoader, "txtDescription");
        assertEquals("42", id.getText()); assertEquals("Synthetic category", name.getText());
        assertEquals("Original description", description.getText());
        CategorySession.setSelectedCategory(99, "Unrelated synthetic category");
        controller.editor.initialSelection.idProperty().set(99); id.setText("99");
        name.setText("  Updated category  "); description.setText("  Updated description  ");
        button(editorLoader.getRoot(), "Save").fire();
        assertEquals(42, controller.repository.updatedId);
        assertEquals("Updated category", controller.repository.updatedName);
        assertEquals("Updated description", controller.repository.updatedDescription);
        assertEquals("Updated category", controller.repository.rows.get(42).name());
        assertEquals(1, controller.editor.backCalls);
    }); }

    @Test void missingOrStaleCategorySelectionDoesNotOpenAnEditor() throws Exception { onFx(() -> {
        for (boolean stale : List.of(false, true)) {
            CategorySession.clear();
            if (stale) CategorySession.setSelectedCategory(99, "Deleted synthetic category");
            var controller = new CategoriesStub(List.of(new Category(42, "Synthetic", "")));
            var loader = load("Category.fxml", controller);
            button(loader.getRoot(), "Edit").fire();
            assertEquals("Edit Category", controller.alertTitle); assertNull(controller.editorLoader);
        }
    }); }

    @Test void editorWithoutAValidCategoryCannotCallPersistence() throws Exception { onFx(() -> {
        var controller = new EditorStub(); var loader = load("EditCategory.fxml", controller);
        ((TextField) node(loader, "txtName")).setText("Synthetic");
        button(loader.getRoot(), "Save").fire();
        assertEquals("Validation", controller.alertTitle); assertEquals(0, controller.repository.updateCalls);
        controller.setCategory(new Category(42, "Synthetic", ""));
        assertThrows(IllegalArgumentException.class, () -> controller.setCategory(null));
        button(loader.getRoot(), "Save").fire();
        assertEquals(0, controller.repository.updateCalls);
        assertThrows(IllegalArgumentException.class, () -> controller.setCategory(new Category(0, "Invalid", "")));
    }); }

    @Test void categoryNameValidationAndDuplicateExclusionRemainActive() throws Exception { onFx(() -> {
        var controller = new EditorStub(); var loader = load("EditCategory.fxml", controller);
        controller.setCategory(new Category(42, "Synthetic", null));
        assertEquals("", ((TextArea) node(loader, "txtDescription")).getText());
        TextField name = node(loader, "txtName"); name.setText("   ");
        button(loader.getRoot(), "Save").fire();
        assertEquals("Validation", controller.alertTitle); assertEquals(0, controller.repository.updateCalls);
        controller.repository.rows.put(99, new com.sobol.category.domain.Category(99, "Duplicate", "Other"));
        name.setText(" Duplicate ");
        button(loader.getRoot(), "Save").fire();
        assertEquals("Duplicate", controller.alertTitle); assertEquals(42, controller.repository.updatedId);
        assertEquals("Synthetic", controller.repository.rows.get(42).name());
        assertEquals("Duplicate", controller.repository.rows.get(99).name());
        assertEquals(0, controller.backCalls);
    }); }

    @Test void categoryUpdateFailureDoesNotNavigateOrChangeItsTarget() throws Exception { onFx(() -> {
        var controller = new EditorStub(); controller.repository.writeFailure = CategoryException.Reason.STORAGE_FAILURE;
        var loader = load("EditCategory.fxml", controller); controller.setCategory(new Category(42, "Synthetic", ""));
        button(loader.getRoot(), "Save").fire();
        assertEquals(42, controller.repository.updatedId); assertEquals("Error", controller.alertTitle);
        assertEquals("Synthetic", controller.repository.rows.get(42).name());
        assertEquals(0, controller.backCalls);
        button(loader.getRoot(), "Cancel").fire();
        assertEquals(1, controller.backCalls); assertEquals(1, controller.repository.updateCalls);
    }); }

    @Test void categoryDeletedAfterOpeningReportsNotFoundWithoutNavigation() throws Exception { onFx(() -> {
        var controller = new EditorStub(); var loader = load("EditCategory.fxml", controller);
        controller.setCategory(new Category(42, "Synthetic", "")); controller.repository.rows.remove(42);
        button(loader.getRoot(), "Save").fire();
        assertEquals("Not Found", controller.alertTitle); assertEquals(0, controller.backCalls);
        assertTrue(controller.repository.rows.isEmpty());
    }); }

    @Test void unchangedCategoryValuesRemainASuccessfulSave() throws Exception { onFx(() -> {
        var controller = new EditorStub(); var loader = load("EditCategory.fxml", controller);
        controller.setCategory(new Category(42, "Synthetic", ""));
        button(loader.getRoot(), "Save").fire();
        assertEquals(1, controller.backCalls); assertNull(controller.alertTitle);
        assertEquals(new com.sobol.category.domain.Category(42, "Synthetic", ""), controller.repository.rows.get(42));
    }); }

    @Test void addCategoryNavigationDelegatesCreationToTheRealService() throws Exception { onFx(() -> {
        var controller = new CategoriesStub(List.of(new Category(42, "Synthetic", "")));
        var loader = load("Category.fxml", controller);
        StackPane content = attachCategoryScene(loader);
        button(loader.getRoot(), "Add").fire();
        assertSame(controller.addLoader.getRoot(), content.getChildren().getFirst());
        ((TextField) node(controller.addLoader, "txtName")).setText("  New synthetic category  ");
        ((TextArea) node(controller.addLoader, "txtDescription")).setText("  Description  ");
        button(controller.addLoader.getRoot(), "Add").fire();
        assertEquals(1, controller.repository.insertCalls); assertEquals(1, controller.add.backCalls);
        assertTrue(controller.repository.rows.values().stream().anyMatch(c -> c.name().equals("New synthetic category") && c.description().equals("Description")));
    }); }

    @Test void categoryCreationValidationDuplicateAndStorageFailureAreDistinct() throws Exception { onFx(() -> {
        var repository = new CategoryRepositoryStub(List.of(new Category(42, "Synthetic", "")));
        var controller = new AddStub(repository); var loader = load("AddCategory.fxml", controller);
        TextField name = node(loader, "txtName"); name.setText("   "); button(loader.getRoot(), "Add").fire();
        assertEquals("Validation", controller.alertTitle); assertEquals(0, repository.insertCalls);
        name.setText("Synthetic"); button(loader.getRoot(), "Add").fire();
        assertEquals("Duplicate", controller.alertTitle); assertEquals(1, repository.rows.size());
        repository.writeFailure = CategoryException.Reason.STORAGE_FAILURE;
        name.setText("Other"); button(loader.getRoot(), "Add").fire();
        assertEquals("Error", controller.alertTitle); assertEquals(0, controller.backCalls); assertEquals(1, repository.rows.size());
        int calls = repository.insertCalls; button(loader.getRoot(), "Cancel").fire();
        assertEquals(1, controller.backCalls); assertEquals(calls, repository.insertCalls);
    }); }

    @Test void categorySearchPreservesSelectionButBlocksUnmigratedInventoryNavigation() throws Exception { onFx(() -> {
        var controller = new CategoriesStub(List.of(new Category(42, "Synthetic A", "First"), new Category(99, "Synthetic B", "Second")));
        var loader = load("Category.fxml", controller); StackPane content = attachCategoryScene(loader);
        TextField search = node(loader, "txtSearch"); search.setText("Second");
        var grid = (javafx.scene.layout.GridPane) loader.getNamespace().get("categoryGrid");
        assertEquals(1, grid.getChildren().size());
        ((Button) grid.getChildren().getFirst()).fire();
        assertEquals(99, CategorySession.getSelectedCategoryId()); assertEquals("Synthetic B", CategorySession.getSelectedCategoryName());
        assertNull(controller.productsLoader); assertSame(loader.getRoot(), content.getChildren().getFirst());
        assertEquals("PostgreSQL transition", controller.alertTitle);
        assertEquals(0, controller.repository.insertCalls); assertEquals(0, controller.repository.updateCalls);
    }); }

    @Test void categoryLoadFailureIsVisibleAndCannotRestoreAStaleSessionTarget() throws Exception { onFx(() -> {
        CategorySession.setSelectedCategory(42, "Synthetic");
        var controller = new CategoriesStub(List.of(new Category(42, "Synthetic", ""))); controller.repository.failRead = true;
        var loader = load("Category.fxml", controller);
        assertEquals("Error", controller.alertTitle);
        button(loader.getRoot(), "Edit").fire(); assertNull(controller.editorLoader);
        assertEquals("Edit Category", controller.alertTitle);
    }); }

    private StackPane attachCategoryScene(FXMLLoader loader) {
        StackPane content = new StackPane((Parent) loader.getRoot()); content.setId("contentArea");
        Scene scene = new Scene(content); content.applyCss(); content.layout();
        assertSame(scene, ((Node) node(loader, "categoryGrid")).getScene()); return content;
    }

    @Test void firstProductBranchLoadsAndSelectsItsFirstWarehouseImmediately() throws Exception { onFx(() -> {
        var controller = new ProductStub(); var loader = load("AddProduct.fxml", controller);
        ComboBox<Branch> branch = node(loader, "cbBranch"); ComboBox<Warehouse> warehouse = node(loader, "cbWarehouse");
        assertSame(controller.a, branch.getValue()); assertEquals(List.of(11), controller.requests);
        assertSame(controller.aWarehouse, warehouse.getValue()); assertFalse(warehouse.isDisabled());
    }); }

    @Test void productBranchChangesClearStaleSelectionsIncludingEmptyAndNullBranches() throws Exception { onFx(() -> {
        var controller = new ProductStub(); var loader = load("AddProduct.fxml", controller);
        ComboBox<Branch> branch = node(loader, "cbBranch"); ComboBox<Warehouse> warehouse = node(loader, "cbWarehouse");
        branch.setValue(controller.b);
        assertEquals(List.of(controller.bWarehouse), warehouse.getItems()); assertSame(controller.bWarehouse, warehouse.getValue());
        branch.setValue(controller.empty); assertEmptyWarehouse(warehouse);
        branch.setValue(null); assertEmptyWarehouse(warehouse);
        branch.setValue(controller.a); assertSame(controller.aWarehouse, warehouse.getValue());
        assertEquals(List.of(11, 22, 33, 11), controller.requests);
    }); }

    @Test void productFormWithNoBranchesCannotProceedToProductPersistence() throws Exception { onFx(() -> {
        var controller = new ProductStub(); controller.branches = List.of();
        var loader = load("AddProduct.fxml", controller);
        assertNull(((ComboBox<?>) node(loader, "cbBranch")).getValue());
        assertEmptyWarehouse(node(loader, "cbWarehouse")); assertTrue(controller.requests.isEmpty());
        button(loader.getRoot(), "Add").fire(); assertEquals("Please select a branch.", controller.alertMessage);
    }); }

    @Test void emptyInitialBranchHasNoWarehouseAndSwitchingCanRecover() throws Exception { onFx(() -> {
        var controller = new ProductStub(); controller.branches = List.of(controller.empty, controller.a);
        var loader = load("AddProduct.fxml", controller);
        ComboBox<Warehouse> warehouse = node(loader, "cbWarehouse"); assertEmptyWarehouse(warehouse);
        button(loader.getRoot(), "Add").fire(); assertEquals("No warehouse available for this branch.", controller.alertMessage);
        ComboBox<Branch> branch = node(loader, "cbBranch"); branch.setValue(controller.a);
        assertSame(controller.aWarehouse, warehouse.getValue()); assertFalse(warehouse.isDisabled());
    }); }

    @Test void failedWarehouseLookupLeavesNoPreviousWarehouseToSave() throws Exception { onFx(() -> {
        var controller = new ProductStub(); var loader = load("AddProduct.fxml", controller);
        controller.failBranch = 22;
        ComboBox<Branch> branch = node(loader, "cbBranch"); branch.setValue(controller.b);
        assertEmptyWarehouse(node(loader, "cbWarehouse")); assertEquals("Error", controller.alertTitle);
        button(loader.getRoot(), "Add").fire(); assertEquals("No warehouse available for this branch.", controller.alertMessage);
    }); }

    @Test void productFormRejectsWarehousesOutsideTheSelectedBranchOrLoadedList() throws Exception { onFx(() -> {
        var controller = new ProductStub(); var loader = load("AddProduct.fxml", controller);
        ComboBox<Branch> branch = node(loader, "cbBranch"); branch.setValue(controller.b);
        ComboBox<Warehouse> warehouse = node(loader, "cbWarehouse");
        for (Warehouse invalid : List.of(controller.aWarehouse, new Warehouse(999, 22, "Synthetic stale", 10))) {
            warehouse.setValue(invalid); button(loader.getRoot(), "Add").fire();
            assertEquals("Please select a warehouse for this branch.", controller.alertMessage);
        }
    }); }

    private void assertEmptyWarehouse(ComboBox<Warehouse> warehouse) {
        assertNull(warehouse.getValue()); assertNull(warehouse.getSelectionModel().getSelectedItem());
        assertTrue(warehouse.getItems().isEmpty()); assertTrue(warehouse.isDisabled());
    }

    private static class GridPaneAccess {
        static void selectFirstCard(FXMLLoader loader) {
            var grid = (javafx.scene.layout.GridPane) loader.getNamespace().get("categoryGrid");
            ((Button) grid.getChildren().getFirst()).getOnContextMenuRequested().handle(null);
        }
    }

    private static class DailyStub extends DailyAttendanceController {
        int presenceLookups, saveCalls, backCalls; boolean saveResult = true;
        LocalDate loadedDate, savedDate; String alertTitle; List<DailyAttendanceRow> saved;
        @Override List<GeneralWorkerRow> loadDailyWorkers() {
            return List.of(worker(101), worker(102), worker(103));
        }
        private GeneralWorkerRow worker(int id) {
            return new GeneralWorkerRow(id, "Synthetic worker " + id, "0000000000", 11, "Synthetic branch", 50, "2026-01-15", null);
        }
        @Override Boolean getRecordedPresence(int id, LocalDate date) {
            presenceLookups++; loadedDate = date; return id == 103 ? null : id == 102;
        }
        @Override boolean persistDailyAttendance(LocalDate date, List<DailyAttendanceRow> rows) {
            saveCalls++; savedDate = date; saved = List.copyOf(rows); return saveResult;
        }
        @Override void goBack() { backCalls++; }
        @Override void alert(String title, String message) { alertTitle = title; }
    }

    private static class CategoriesStub extends CategoryController {
        final CategoryRepositoryStub repository; final EditorStub editor; final AddStub add;
        String alertTitle; FXMLLoader editorLoader, addLoader, productsLoader;
        CategoriesStub(List<Category> categories) { this(new CategoryRepositoryStub(categories)); }
        CategoriesStub(CategoryRepositoryStub repository) {
            super(new CategoryService(repository)); this.repository = repository;
            editor = new EditorStub(repository); add = new AddStub(repository);
        }
        @Override FXMLLoader createViewLoader(String fxml) {
            var loader = new FXMLLoader(getClass().getResource("/application/" + fxml));
            switch (fxml) {
                case "EditCategory.fxml" -> { editorLoader = loader; loader.setControllerFactory(type -> editor); }
                case "AddCategory.fxml" -> { addLoader = loader; loader.setControllerFactory(type -> add); }
                case "Products.fxml" -> { productsLoader = loader; loader.setControllerFactory(type -> new ProductsController() {
                    @Override public void initialize() { } // Test the navigation boundary without invoking the legacy product DAO.
                }); }
                default -> fail("Unexpected category navigation target");
            }
            return loader;
        }
        @Override void showInfo(String title, String message) { alertTitle = title; }
    }

    private static class EditorStub extends EditCategoryController {
        final CategoryRepositoryStub repository; int backCalls; String alertTitle; Category initialSelection;
        EditorStub() { this(new CategoryRepositoryStub(List.of(new Category(42, "Synthetic", "")))); }
        EditorStub(CategoryRepositoryStub repository) { super(new CategoryService(repository)); this.repository = repository; }
        @Override public void setCategory(Category category) { initialSelection = category; super.setCategory(category); }
        @Override void goBack() { backCalls++; }
        @Override void showAlert(String title, String message) { alertTitle = title; }
    }

    private static class AddStub extends AddCategoryController {
        int backCalls; String alertTitle;
        AddStub(CategoryRepositoryStub repository) { super(new CategoryService(repository)); }
        @Override void goBack() { backCalls++; }
        @Override void showAlert(String title, String message) { alertTitle = title; }
    }

    private static class CategoryRepositoryStub implements CategoryRepository {
        final Map<Integer, com.sobol.category.domain.Category> rows = new LinkedHashMap<>();
        int insertCalls, updateCalls, updatedId, sequence = 100;
        String updatedName, updatedDescription; CategoryException.Reason writeFailure; boolean failRead;
        CategoryRepositoryStub(List<Category> categories) {
            for (Category c : categories) rows.put(c.getId(), new com.sobol.category.domain.Category(c.getId(), c.nameProperty().get(), c.descriptionProperty().get()));
        }
        public List<com.sobol.category.domain.Category> findAll() {
            if (failRead) throw new CategoryException(CategoryException.Reason.STORAGE_FAILURE);
            return new ArrayList<>(rows.values());
        }
        private void canWrite(String name, int id) {
            if (writeFailure != null) throw new CategoryException(writeFailure);
            if (rows.values().stream().anyMatch(c -> c.id() != id && c.name().equals(name)))
                throw new CategoryException(CategoryException.Reason.DUPLICATE_NAME);
        }
        public com.sobol.category.domain.Category insert(String name, String description) {
            insertCalls++; canWrite(name, 0);
            var created = new com.sobol.category.domain.Category(++sequence, name, description); rows.put(created.id(), created); return created;
        }
        public Optional<com.sobol.category.domain.Category> update(int id, String name, String description) {
            updateCalls++; updatedId = id; updatedName = name; updatedDescription = description;
            if (writeFailure != null) throw new CategoryException(writeFailure);
            if (!rows.containsKey(id)) return Optional.empty();
            canWrite(name, id); var updated = new com.sobol.category.domain.Category(id, name, description); rows.put(id, updated);
            return Optional.of(updated);
        }
    }

    private static class ProductStub extends AddProductController {
        final Branch a = new Branch(11, "Synthetic A", "Synthetic City", "0000000000", 1, 10);
        final Branch b = new Branch(22, "Synthetic B", "Synthetic City", "0000000000", 1, 10);
        final Branch empty = new Branch(33, "Synthetic Empty", "Synthetic City", "0000000000", 0, 0);
        final Warehouse aWarehouse = new Warehouse(101, 11, "Synthetic warehouse A", 10);
        final Warehouse bWarehouse = new Warehouse(202, 22, "Synthetic warehouse B", 10);
        List<Branch> branches = List.of(a, b, empty); final List<Integer> requests = new ArrayList<>();
        int failBranch; String alertTitle, alertMessage;
        @Override List<Branch> getBranches() { return branches; }
        @Override List<Warehouse> getWarehouses(int id) {
            requests.add(id);
            if (id == failBranch) throw new IllegalStateException("Synthetic warehouse lookup failure");
            return id == 11 ? List.of(aWarehouse) : id == 22 ? List.of(bWarehouse) : List.of();
        }
        @Override void alert(String title, String message) { alertTitle = title; alertMessage = message; }
    }
}
