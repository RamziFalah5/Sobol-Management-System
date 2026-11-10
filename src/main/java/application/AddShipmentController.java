package application;

import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.util.List;

public class AddShipmentController {

	@FXML
	private TextField txtProduct, txtCategory, txtCartons, txtCurrentQty;
	@FXML
	private ComboBox<Branch> cbBranch;
	@FXML
	private ComboBox<Warehouse> cbWarehouse;
	@FXML
	private ComboBox<Supplier> cbSupplier;
	@FXML
	private DatePicker dpDate;

	@FXML
	private TextField txtQty, txtUnitCost, txtTotal, txtRef;
	@FXML
	private RadioButton rbCash, rbCredit;
	@FXML
	private ComboBox<String> cbMethod;
	@FXML
	private TextArea txtNotes;

	private final ToggleGroup payGroup = new ToggleGroup();
	private ProductPickRow selected;

	@FXML
	public void initialize() {

		/* ================= SAFETY CHECK ================= */
		if (rbCash != null && rbCredit != null) {
			rbCash.setToggleGroup(payGroup);
			rbCredit.setToggleGroup(payGroup);
			rbCash.setSelected(true);
		}

		if (cbMethod != null) {
			cbMethod.getItems().setAll("TRUCK", "PICKUP", "OTHER");
			cbMethod.getSelectionModel().selectFirst();
		}

		selected = ShipmentSession.getSelectedProduct();
		if (selected == null) {
			alert("New Shipment", "No product selected. Please select a product first.");
			go("ShipmentProducts.fxml");
			return;
		}

		/* ================= PRODUCT INFO ================= */
		txtProduct.setText(selected.getProductName());
		txtCategory.setText(selected.getCategoryName());
		txtCartons.setText(String.valueOf(selected.getCartonsPerPallet()));
		txtCurrentQty.setText(String.valueOf(selected.getQuantity()));

		txtUnitCost.setText(String.format("%.2f", selected.getCostPrice()));
		txtTotal.setText("0.00");

		dpDate.setValue(LocalDate.now());

		loadBranches();
		loadSuppliers();

		cbBranch.setOnAction(e -> loadWarehouses());
		preSelectBranchWarehouse();

		txtQty.textProperty().addListener((obs, o, n) -> calcTotal());

		/* ===== Lock branch & warehouse (product-bound) ===== */
		cbBranch.setMouseTransparent(true);
		cbBranch.setFocusTraversable(false);

		cbWarehouse.setMouseTransparent(true);
		cbWarehouse.setFocusTraversable(false);
	}

	/* ================= LOADERS ================= */

	private void loadBranches() {
		List<Branch> branches = BranchDAO.getAllBranches();
		if (branches.isEmpty()) {
			alert("Branches Required", "No branches found. Please create branches first.");
			go("Imports.fxml");
			return;
		}
		cbBranch.setItems(FXCollections.observableArrayList(branches));
	}

	private void loadWarehouses() {
		Branch b = cbBranch.getSelectionModel().getSelectedItem();
		if (b == null) {
			cbWarehouse.getItems().clear();
			return;
		}

		List<Warehouse> whs = WarehouseDAO.getWarehousesByBranch(b.getBranchId());
		cbWarehouse.setItems(FXCollections.observableArrayList(whs));

		if (whs.isEmpty()) {
			alert("Warehouses Required", "This branch has no warehouses.");
		} else {
			cbWarehouse.getSelectionModel().selectFirst();
		}
	}

	private void loadSuppliers() {
		List<Supplier> suppliers = SupplierDAO.getAllSuppliers();
		if (suppliers.isEmpty()) {
			alert("Suppliers Required", "No suppliers found. Please add suppliers first.");
			go("Imports.fxml");
			return;
		}
		cbSupplier.setItems(FXCollections.observableArrayList(suppliers));
		cbSupplier.getSelectionModel().selectFirst();
	}

	private void preSelectBranchWarehouse() {
		for (Branch b : cbBranch.getItems()) {
			if (b.getBranchId() == selected.getBranchId()) {
				cbBranch.getSelectionModel().select(b);
				break;
			}
		}

		loadWarehouses();

		for (Warehouse w : cbWarehouse.getItems()) {
			if (w.getWarehouseId() == selected.getWarehouseId()) {
				cbWarehouse.getSelectionModel().select(w);
				break;
			}
		}
	}

	/* ================= CALCULATIONS ================= */

	private void calcTotal() {
		String s = txtQty.getText().trim();
		if (s.isEmpty()) {
			txtTotal.setText("0.00");
			return;
		}
		try {
			int q = Integer.parseInt(s);
			if (q <= 0) {
				txtTotal.setText("0.00");
				return;
			}
			double total = q * selected.getCostPrice();
			txtTotal.setText(String.format("%.2f", total));
		} catch (NumberFormatException e) {
			txtTotal.setText("0.00");
		}
	}

	/* ================= ACTIONS ================= */

	@FXML
	private void onSave() {

		Branch branch = cbBranch.getSelectionModel().getSelectedItem();
		Warehouse wh = cbWarehouse.getSelectionModel().getSelectedItem();
		Supplier sup = cbSupplier.getSelectionModel().getSelectedItem();

		if (branch == null || wh == null || sup == null) {
			alert("Validation", "Branch, Warehouse and Supplier are required.");
			return;
		}

		Integer qty = parseIntSafe(txtQty, "Quantity");
		if (qty == null || qty <= 0)
			return;

		LocalDate date = dpDate.getValue();
		if (date == null) {
			alert("Validation", "Shipment date is required.");
			return;
		}

		String payment = (rbCredit != null && rbCredit.isSelected()) ? "CREDIT" : "CASH";
		String method = cbMethod.getSelectionModel().getSelectedItem();
		if (method == null)
			method = "TRUCK";

		double unit = selected.getCostPrice();
		double total = qty * unit;

		ShipmentDAO.ShipmentFormData d = new ShipmentDAO.ShipmentFormData();
		d.productId = selected.getProductId();
		d.supplierId = sup.getSupplierId();
		d.branchId = branch.getBranchId();
		d.warehouseId = wh.getWarehouseId();
		d.quantity = qty;
		d.unitCost = unit;
		d.totalCost = total;
		d.shipmentDate = date;
		d.paymentType = payment;
		d.shippingMethod = method;
		d.referenceNo = txtRef.getText().trim();
		d.notes = txtNotes.getText().trim();

		boolean ok = ShipmentDAO.addShipment(d);
		if (!ok) {
			alert("No Space / Error", "Warehouse capacity is not enough.");
			return;
		}

		alert("Success", "Shipment saved successfully ✅");
		ShipmentSession.clear();
		go("Imports.fxml");
	}

	@FXML
	private void onCancel() {
		ShipmentSession.clear();
		go("ShipmentProducts.fxml");
	}

	/* ================= UTIL ================= */

	private Integer parseIntSafe(TextField tf, String label) {
		String v = tf.getText().trim();
		if (v.isEmpty()) {
			alert("Validation", label + " is required.");
			return null;
		}
		try {
			return Integer.parseInt(v);
		} catch (NumberFormatException e) {
			alert("Validation", label + " must be a number.");
			return null;
		}
	}

	private void go(String fxml) {
		try {
			Parent view = FXMLLoader.load(getClass().getResource(fxml));
			StackPane content = (StackPane) txtProduct.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
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
