package application;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class ControllerSystem {

	@FXML
	private StackPane contentArea;

	@FXML
	private ToggleButton btnDashboard;
	@FXML
	private ToggleButton btnCategory;
	@FXML
	private ToggleButton btnSales;
	@FXML
	private ToggleButton btnImports;
	@FXML
	private ToggleButton btnBranches;
	@FXML
	private ToggleButton btnEmployee;
	@FXML
	private ToggleButton btnVehicles;
	@FXML
	private ToggleButton btnSupplier;
	@FXML
	private ToggleButton btnCustomer;
	@FXML
	private ToggleButton btnReports;
	@FXML
	private ToggleButton btnSettings;
	@FXML
	private TextField searchField;

    private final AuthModule.Context auth;
    private com.sobol.auth.application.SessionManager.Handle handle;
    private javafx.animation.Timeline revocation;
    private boolean polling;
    public ControllerSystem() { this(AuthModule.context()); }
    ControllerSystem(AuthModule.Context auth) { this.auth = auth; }

    public void initialize() {
        handle = auth.sessions.capture();
        switchToDashboard();
        revocation = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(15), event -> poll()));
        revocation.setCycleCount(javafx.animation.Animation.INDEFINITE);
        contentArea.sceneProperty().addListener((property, oldScene, scene) -> {
            if (scene == null) { revocation.stop(); return; }
            scene.windowProperty().addListener((windowProperty, oldWindow, window) -> {
                if (window == null) { revocation.stop(); contentArea.getChildren().clear(); }
                else window.showingProperty().addListener((visibleProperty, oldValue, visible) -> {
                    if (!visible) { revocation.stop(); auth.sessions.invalidate(handle); contentArea.getChildren().clear(); }
                });
            });
            revocation.play();
        });
    }
    void poll() {
        if (polling || contentArea.getScene() == null || contentArea.getScene().getWindow() == null
                || !contentArea.getScene().getWindow().isShowing()) return;
        polling = true;
        auth.worker.execute(() -> {
            boolean valid;
            try { auth.sessions.inspect(handle); valid = true; }
            catch (com.sobol.auth.application.AuthenticationException failure) { valid = false; }
            boolean result = valid;
            javafx.application.Platform.runLater(() -> { polling = false; if (!result) leave(); });
        });
    }
    private com.sobol.auth.application.SessionManager.Identity identity() {
        try { return auth.sessions.require(handle); }
        catch (com.sobol.auth.application.AuthenticationException failure) { leave(); return null; }
    }
    private boolean authorized() { return identity() != null; }
    @FXML private void switchToDashboard() {
        var identity = identity();
        if (identity == null) return;
        var title = new Label("Welcome, " + identity.displayName());
        var home = new javafx.scene.layout.VBox(16, title, new Label("Role: " + identity.role()));
        home.setStyle("-fx-padding: 32;");
        for (String page : new String[]{"Category.fxml", "Suppliers.fxml", "Customers.fxml"}) {
            var button = new javafx.scene.control.Button(page.substring(0, page.length() - 5));
            button.setOnAction(event -> {
                try { loadPage(page, page.equals("Category.fxml") ? btnCategory : page.equals("Suppliers.fxml") ? btnSupplier : btnCustomer); }
                catch (IOException failure) { unavailableScreen(); }
            });
            home.getChildren().add(button);
        }
        contentArea.getChildren().setAll(home); select(btnDashboard);
    }

	@FXML
	private void switchToCategory() throws IOException {
		loadPage("Category.fxml", btnCategory);
	}

	@FXML
	private void switchToSales() throws IOException {
		loadPage("Sales.fxml", btnSales);
	}

	@FXML
	private void switchToImports() throws IOException {
		loadPage("Imports.fxml", btnImports);
	}

	@FXML
	private void switchToBranches() throws IOException {
		loadPage("Branches.fxml", btnBranches);
	}

	@FXML
	private void switchToEmployee() throws IOException {
		loadPage("Employee.fxml", btnEmployee);
	}

	@FXML
	private void switchToVehicles() throws IOException {
		loadPage("Vehicles.fxml", btnVehicles);
	}

	@FXML
	private void switchToSupplier() throws IOException {
		loadPage("Suppliers.fxml", btnSupplier);
	}

	@FXML
	private void switchToCustomer() throws IOException {
		loadPage("Customers.fxml", btnCustomer);
	}

	@FXML
	private void switchToReports() throws IOException {
		loadPage("Reports.fxml", btnReports);
	}

	@FXML
	private void switchToSettings() throws IOException {
		loadPage("Settings.fxml", btnSettings);
	}

    @FXML private void logout() {
        auth.sessions.invalidate(handle); leave();
    }
    private void leave() {
        if (revocation != null) revocation.stop();
        contentArea.getChildren().clear();
        var scene = contentArea.getScene();
        if (scene == null || !(scene.getWindow() instanceof javafx.stage.Stage stage) || stage.getScene() != scene) return;
        Controller1.clearBusinessSelection();
        // Remove all owned dialogs as well as the protected root.
        for (var window : java.util.List.copyOf(javafx.stage.Window.getWindows())) {
            if (window instanceof javafx.stage.Stage child && child.getOwner() == stage) child.close();
        }
        try { auth.show(stage, "Signin.fxml"); }
        catch (IOException failure) { stage.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(new Label("Please restart and sign in.")), 480, 640)); }
    }
    @FXML private void onSearchClicked() {
        if (authorized()) contentArea.getChildren().setAll(new Label("Search is temporarily unavailable."));
    }
    private void unavailableScreen() {
        contentArea.getChildren().setAll(new Label("The screen could not be opened. Please try again."));
    }
    private void loadPage(String fxmlFile, ToggleButton activeBtn) throws IOException {
        if (!authorized()) return;
        try {
            Node view = isPageAvailable(fxmlFile) ? auth.loader(fxmlFile).load()
                    : new Label("This module is temporarily unavailable.");
            contentArea.getChildren().setAll(view); select(activeBtn);
        } catch (IOException failure) {
            // A failed authentication check in a nested FXML constructor must fail closed.
            if (authorized()) unavailableScreen();
        }
    }
    private void select(ToggleButton activeBtn) {
        for (ToggleButton tb : new ToggleButton[]{btnDashboard, btnCategory, btnSales, btnImports, btnBranches,
                btnEmployee, btnVehicles, btnSupplier, btnCustomer, btnReports, btnSettings}) {
            if (tb != null) { tb.getStyleClass().remove("nav-btn-active"); tb.setSelected(false); }
        }
        if (activeBtn != null) { activeBtn.getStyleClass().add("nav-btn-active"); activeBtn.setSelected(true); }
    }

	// Keep PostgreSQL master-data IDs out of unmigrated transactional/reporting workflows.
	static boolean isPageAvailable(String page) {
		return java.util.Set.of("Category.fxml", "Suppliers.fxml", "Customers.fxml").contains(page);
	}

}
