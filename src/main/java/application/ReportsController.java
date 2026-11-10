package application;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ReportsController {

	// Overview
	@FXML private Label lblCustomers, lblSuppliers, lblProducts, lblShipments;
	@FXML private Label lblCustomersHint, lblSuppliersHint, lblProductsHint, lblShipmentsHint;

	// Warehouse circle
	@FXML private ProgressIndicator piWarehouse;
	@FXML private Label lblWarehousePct, lblWarehouseUsed, lblWarehouseRemaining, lblWarehouseCap, lblMostFullWarehouse;

	// Stock circle
	@FXML private ProgressIndicator piLowStock;
	@FXML private Label lblLowStockPct, lblLowStockCount, lblNormalCount, lblHighCount, lblZeroCount;

	// Inventory value
	@FXML private Label lblInvUnits, lblInvCost, lblInvSell, lblMarginHint;
	@FXML private ProgressBar pbMargin;

	// Charts
	@FXML private PieChart piePayment;
	@FXML private PieChart pieEmployeeType;

	@FXML private BarChart<String, Number> barWarehouses;
	@FXML private CategoryAxis xWarehouse;
	@FXML private NumberAxis yWarehouse;

	@FXML private BarChart<String, Number> barSuppliers;
	@FXML private CategoryAxis xSupplier;
	@FXML private NumberAxis ySupplier;

	// Tables
	@FXML private TableView<ReportsDAO.SupplierSpendRow> tblTopSuppliers;
	@FXML private TableColumn<ReportsDAO.SupplierSpendRow, String> tsName;
	@FXML private TableColumn<ReportsDAO.SupplierSpendRow, Integer> tsCount;
	@FXML private TableColumn<ReportsDAO.SupplierSpendRow, Double> tsTotal;

	@FXML private TableView<ReportsDAO.ProductImportRow> tblTopProducts;
	@FXML private TableColumn<ReportsDAO.ProductImportRow, String> tpName;
	@FXML private TableColumn<ReportsDAO.ProductImportRow, Integer> tpQty;
	@FXML private TableColumn<ReportsDAO.ProductImportRow, Double> tpCost;

	@FXML private ListView<String> lvAlerts;

	@FXML
	public void initialize() {

		// Top suppliers table
		tsName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tsCount.setCellValueFactory(new PropertyValueFactory<>("shipmentsCount"));
		tsTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));

		// Top products table
		tpName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tpQty.setCellValueFactory(new PropertyValueFactory<>("totalQty"));
		tpCost.setCellValueFactory(new PropertyValueFactory<>("totalCost"));

		loadReport();
	}

	private void loadReport() {

		ReportsDAO.ReportStats s = ReportsDAO.loadStats();

		// ===== Overview =====
		lblCustomers.setText(String.valueOf(s.customers));
		lblSuppliers.setText(String.valueOf(s.suppliers));
		lblProducts.setText(String.valueOf(s.products));
		lblShipments.setText(String.valueOf(s.shipments));

		lblCustomersHint.setText("Records in customer table");
		lblSuppliersHint.setText("Records in supplier table");
		lblProductsHint.setText("Products in inventory");
		lblShipmentsHint.setText("Import shipment history");

		// ===== Warehouse utilization =====
		int cap = s.totalPalletCapacity;
		int used = s.totalPalletUsed;
		int remaining = Math.max(cap - used, 0);

		double util = (cap <= 0) ? 0 : (used * 1.0 / cap);
		piWarehouse.setProgress(clamp(util));
		lblWarehousePct.setText(String.format("%.0f%%", util * 100));
		lblWarehouseUsed.setText("Used: " + used + " pallets");
		lblWarehouseRemaining.setText("Remaining: " + remaining + " pallets");
		lblWarehouseCap.setText("Capacity: " + cap + " pallets");
		lblMostFullWarehouse.setText("Most full: " + (s.mostFullWarehouseName == null ? "-" : s.mostFullWarehouseName));

		// ===== Stock ratio =====
		int totalProducts = Math.max(s.products, 1);
		double lowRatio = s.lowStockCount * 1.0 / totalProducts;
		piLowStock.setProgress(clamp(lowRatio));
		lblLowStockPct.setText(String.format("%.0f%%", lowRatio * 100));

		lblLowStockCount.setText("LOW: " + s.lowStockCount + " products");
		lblNormalCount.setText("NORMAL: " + s.normalStockCount + " products");
		lblHighCount.setText("HIGH: " + s.highStockCount + " products");
		lblZeroCount.setText("Out of stock: " + s.zeroStockCount);

		// ===== Inventory value =====
		lblInvUnits.setText("Total Units: " + s.totalUnits);
		lblInvCost.setText(String.format("Cost Value: %.2f", s.inventoryCostValue));
		lblInvSell.setText(String.format("Selling Value: %.2f", s.inventorySellValue));

		double marginRatio = (s.inventoryCostValue <= 0) ? 0 : (s.inventorySellValue / s.inventoryCostValue);
		// normalize for progress bar: show up to 2.0x -> 100%
		double pb = Math.min(marginRatio / 2.0, 1.0);
		pbMargin.setProgress(clamp(pb));
		lblMarginHint.setText(String.format("%.2fx", marginRatio));

		// ===== Pie: payment type =====
		ObservableList<PieChart.Data> pay = FXCollections.observableArrayList();
		for (ReportsDAO.KeyCountRow r : s.paymentBreakdown) {
			pay.add(new PieChart.Data(r.key + " (" + r.count + ")", r.count));
		}
		piePayment.setData(pay);
		piePayment.setLegendVisible(true);

		// ===== Pie: employee type =====
		ObservableList<PieChart.Data> emp = FXCollections.observableArrayList();
		for (ReportsDAO.KeyCountRow r : s.employeeTypeBreakdown) {
			emp.add(new PieChart.Data(r.key + " (" + r.count + ")", r.count));
		}
		pieEmployeeType.setData(emp);
		pieEmployeeType.setLegendVisible(true);

		// ===== Bar: top warehouses utilization =====
		barWarehouses.getData().clear();
		XYChart.Series<String, Number> ws = new XYChart.Series<>();
		ws.setName("Utilization %");

		for (ReportsDAO.WarehouseUsageRow w : s.topWarehousesByUtil) {
			ws.getData().add(new XYChart.Data<>(shortName(w.warehouseName), Math.round(w.utilization * 100)));
		}
		barWarehouses.getData().add(ws);

		// ===== Bar: top suppliers cost =====
		barSuppliers.getData().clear();
		XYChart.Series<String, Number> ss = new XYChart.Series<>();
		ss.setName("Total Cost");

		for (ReportsDAO.SupplierSpendRow sp : s.topSuppliers) {
			ss.getData().add(new XYChart.Data<>(shortName(sp.name), sp.totalCost));
		}
		barSuppliers.getData().add(ss);

		// ===== Tables =====
		tblTopSuppliers.setItems(FXCollections.observableArrayList(s.topSuppliers));
		tblTopProducts.setItems(FXCollections.observableArrayList(s.topProducts));

		// ===== Alerts =====
		ObservableList<String> alerts = FXCollections.observableArrayList();
		alerts.add("Low Stock Products: " + s.lowStockCount);
		alerts.add("Out of Stock Products: " + s.zeroStockCount);
		alerts.add(String.format("Imports (Last 30 days): %d shipments / %.2f cost", s.imports30DaysCount, s.imports30DaysCost));
		alerts.add(String.format("Attendance (Last 30 days): %d records / present rate %.0f%%",
				s.att30DaysTotal, s.att30DaysRate * 100));
		if (s.mostFullWarehouseName != null) {
			alerts.add(String.format("Most Full Warehouse: %s (%.0f%%)",
					s.mostFullWarehouseName, s.mostFullWarehouseUtil * 100));
		}
		if (s.mostEmptyWarehouseName != null) {
			alerts.add(String.format("Most Empty Warehouse: %s (%.0f%%)",
					s.mostEmptyWarehouseName, s.mostEmptyWarehouseUtil * 100));
		}
		alerts.add("Sales dashboard can be added later (needs sales tables).");
		lvAlerts.setItems(alerts);
	}

	private double clamp(double v) {
		if (v < 0) return 0;
		if (v > 1) return 1;
		return v;
	}

	private String shortName(String s) {
		if (s == null) return "-";
		return s.length() > 10 ? s.substring(0, 10) + "…" : s;
	}
}
