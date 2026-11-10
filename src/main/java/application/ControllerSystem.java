package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

	public void initialize() throws IOException {
		switchToDashboard();
	}

	@FXML
	private void switchToDashboard() throws IOException {
		loadPage("Dashboard.fxml", btnDashboard);
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

	@FXML
	private void logout() {
//		System.out.println("Logging out...");
	}

	@FXML
	private void onSearchClicked() {
		String query = searchField.getText();
		System.out.println("Search clicked! Query: " + query);
	}

	private void loadPage(String fxmlFile, ToggleButton activeBtn) throws IOException {
		Node view = isPageAvailable(fxmlFile)
				? FXMLLoader.load(getClass().getResource(fxmlFile))
				: new Label("This module is temporarily unavailable.");
		contentArea.getChildren().setAll(view);

		for (ToggleButton tb : new ToggleButton[] { btnDashboard, btnCategory, btnSales, btnImports, btnBranches,
				btnEmployee, btnVehicles, btnSupplier, btnCustomer, btnReports, btnSettings }) {
			if (tb != null) {
				tb.getStyleClass().remove("nav-btn-active");
				tb.setSelected(false);
			}
		}

		if (activeBtn != null) {
			activeBtn.getStyleClass().add("nav-btn-active");
			activeBtn.setSelected(true);
		}
	}

	// Keep PostgreSQL master-data IDs out of unmigrated transactional/reporting workflows.
	static boolean isPageAvailable(String page) {
		return java.util.Set.of("Category.fxml", "Suppliers.fxml", "Customers.fxml").contains(page);
	}

}
