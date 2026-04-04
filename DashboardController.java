package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

public class DashboardController {

	@FXML
	private Label lblProducts, lblCategories, lblSuppliers, lblCustomers;
	@FXML
	private Label lblEmployees, lblVehicles, lblBranches, lblWarehouses;

	@FXML
	private ProgressIndicator piCapacity, piLowStock, piShipments;
	@FXML
	private Label lblCapPercent, lblCapDetails;
	@FXML
	private Label lblLowPercent, lblLowDetails;
	@FXML
	private Label lblShipPercent, lblShipDetails;

	@FXML
	private Label lblInventoryCost, lblTotalImports, lblStockSplit;

	@FXML
	public void initialize() {
		loadStats();
	}

	private void loadStats() {
		try {
			DashboardStats s = DashboardDAO.getStats();

			lblProducts.setText(String.valueOf(s.products));
			lblCategories.setText(String.valueOf(s.categories));
			lblSuppliers.setText(String.valueOf(s.suppliers));
			lblCustomers.setText(String.valueOf(s.customers));
			lblEmployees.setText(String.valueOf(s.employees));
			lblVehicles.setText(String.valueOf(s.vehicles));
			lblBranches.setText(String.valueOf(s.branches));
			lblWarehouses.setText(String.valueOf(s.warehouses));

			double capRatio = ratioSafe(s.usedPallets, s.totalPalletCapacity);
			piCapacity.setProgress(capRatio);
			lblCapPercent.setText(percentText(capRatio));
			lblCapDetails.setText("Used " + s.usedPallets + " / " + s.totalPalletCapacity + " pallets");

			double lowRatio = ratioSafe(s.lowStock, s.products);
			piLowStock.setProgress(lowRatio);
			lblLowPercent.setText(percentText(lowRatio));
			lblLowDetails.setText(s.lowStock + " low out of " + s.products + " products");

			double shipRatio = ratioSafe(s.shipments30, Math.max(1, s.products));
			piShipments.setProgress(shipRatio);
			lblShipPercent.setText(percentText(shipRatio));
			lblShipDetails.setText(s.shipments30 + " shipments | Qty " + s.shippedQty30);

			lblInventoryCost.setText("Inventory Cost: " + format2(s.inventoryCost));
			lblTotalImports.setText("Total Imports Cost: " + format2(s.totalImportsCost));
			lblStockSplit.setText("Stock: Normal " + s.normalStock + " | Low " + s.lowStock + " | High " + s.highStock);

		} catch (Exception e) {
			e.printStackTrace();
			alert("Dashboard", "Failed to load dashboard statistics.");
		}
	}

	private double ratioSafe(double part, double total) {
		if (total <= 0)
			return 0;
		double r = part / total;
		if (r < 0)
			return 0;
		if (r > 1)
			return 1;
		return r;
	}

	private String percentText(double r) {
		int p = (int) Math.round(r * 100.0);
		return p + "%";
	}

	private String format2(double v) {
		return String.format("%.2f", v);
	}

	@FXML
	private void openCategories() {
		go("Category.fxml");
	}

	@FXML
	private void openBranches() {
		go("Branches.fxml");
	}

	@FXML
	private void openEmployees() {
		go("Employee.fxml");
	}

	@FXML
	private void openVehicles() {
		go("Vehicles.fxml");
	}

	@FXML
	private void openSuppliers() {
		go("Suppliers.fxml");
	}

	@FXML
	private void openCustomers() {
		go("Customers.fxml");
	}

	@FXML
	private void openImports() {
		go("Imports.fxml");
	}

	@FXML
	private void openSales() {
		go("Sales.fxml");
	}

	@FXML
	private void openReports() {
		go("Reports.fxml");
	}

	@FXML
	private void openSettings() {
		go("Settings.fxml");
	}

	private void go(String fxml) {
		try {
			Parent view = FXMLLoader.load(getClass().getResource(fxml));
			StackPane content = (StackPane) lblProducts.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
			alert("Navigation", "Cannot open: " + fxml);
		}
	}

	private void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
