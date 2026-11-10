package application;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ViewSupplierShipmentsController {

	@FXML
	private Label lblTitle;

	@FXML
	private Label lblCount;

	@FXML
	private Label lblTotal;

	@FXML
	private TableView<Shipment> table;

	@FXML
	private TableColumn<Shipment, Number> colId;
	@FXML
	private TableColumn<Shipment, String> colProduct;
	@FXML
	private TableColumn<Shipment, String> colBranch;
	@FXML
	private TableColumn<Shipment, String> colWarehouse;
	@FXML
	private TableColumn<Shipment, Number> colQty;
	@FXML
	private TableColumn<Shipment, Number> colUnit;
	@FXML
	private TableColumn<Shipment, Number> colTotalCost;

	// ✅ FIX: was Object, must be LocalDate
	@FXML
	private TableColumn<Shipment, LocalDate> colDate;

	@FXML
	private TableColumn<Shipment, String> colPayment;
	@FXML
	private TableColumn<Shipment, String> colShipMethod;
	@FXML
	private TableColumn<Shipment, String> colRef;

	private final ObservableList<Shipment> list = FXCollections.observableArrayList();

	private int supplierId = -1;

	public void setSupplier(Supplier s) {
		supplierId = -1;
		if (s == null || s.getSupplierId() <= 0) throw new IllegalArgumentException("A valid selected supplier is required.");
		supplierId = s.getSupplierId();

		lblTitle.setText("Supplier Shipments - " + s.getName());

		load();
	}

	@FXML
	public void initialize() {

		colId.setCellValueFactory(d -> d.getValue().shipmentIdProperty());
		colProduct.setCellValueFactory(d -> d.getValue().productNameProperty());
		colBranch.setCellValueFactory(d -> d.getValue().branchNameProperty());
		colWarehouse.setCellValueFactory(d -> d.getValue().warehouseNameProperty());

		colQty.setCellValueFactory(d -> d.getValue().quantityProperty());
		colUnit.setCellValueFactory(d -> d.getValue().unitCostProperty());
		colTotalCost.setCellValueFactory(d -> d.getValue().totalCostProperty());

		// ✅ FIX: LocalDate matches the column type now
		colDate.setCellValueFactory(d -> d.getValue().shipmentDateProperty());

		colPayment.setCellValueFactory(d -> d.getValue().paymentTypeProperty());
		colShipMethod.setCellValueFactory(d -> d.getValue().shippingMethodProperty());
		colRef.setCellValueFactory(d -> d.getValue().referenceNoProperty());

		// Center text for nice look
		center(colId);
		center(colProduct);
		center(colBranch);
		center(colWarehouse);
		center(colQty);
		center(colUnit);
		center(colTotalCost);

		// ✅ date special center + formatting
		centerDate(colDate);

		center(colPayment);
		center(colShipMethod);
		center(colRef);
	}

	private <T> void center(TableColumn<Shipment, T> col) {
		col.setCellFactory(tc -> {
			TableCell<Shipment, T> cell = new TableCell<>() {
				@Override
				protected void updateItem(T item, boolean empty) {
					super.updateItem(item, empty);
					setText(empty || item == null ? "" : item.toString());
				}
			};
			cell.setStyle("-fx-alignment: CENTER;");
			return cell;
		});
	}

	// ✅ Better date formatting (yyyy-MM-dd) + centered
	private void centerDate(TableColumn<Shipment, LocalDate> col) {

		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		col.setCellFactory(tc -> {
			TableCell<Shipment, LocalDate> cell = new TableCell<>() {
				@Override
				protected void updateItem(LocalDate item, boolean empty) {
					super.updateItem(item, empty);
					setText(empty || item == null ? "" : fmt.format(item));
				}
			};
			cell.setStyle("-fx-alignment: CENTER;");
			return cell;
		});
	}

	private void load() {

		if (supplierId <= 0) {
			alert("Error", "No supplier selected.");
			return;
		}

		list.setAll(loadShipments(supplierId));
		table.setItems(list);

		lblCount.setText("Shipments: " + list.size());

		double total = loadTotalCost(supplierId);
		lblTotal.setText(String.format("Total Cost: %.2f", total));

		if (list.isEmpty()) {
			// optional info (not blocking)
			// alert("Info", "No shipments found for this supplier.");
		}
	}

	@FXML
	private void onBack() {
		try {
			Parent view = createViewLoader("Suppliers.fxml").load();
			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	java.util.List<Shipment> loadShipments(int id) { return ShipmentDAO.getShipmentsBySupplier(id); }
	double loadTotalCost(int id) { return ShipmentDAO.getTotalCostBySupplier(id); }
	FXMLLoader createViewLoader(String fxml) { return new FXMLLoader(getClass().getResource(fxml)); }

	void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
