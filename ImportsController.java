package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;

public class ImportsController {

	@FXML
	private TableView<Shipment> table;

	@FXML
	private TableColumn<Shipment, Number> colId;
	@FXML
	private TableColumn<Shipment, LocalDate> colDate;
	@FXML
	private TableColumn<Shipment, String> colProduct;
	@FXML
	private TableColumn<Shipment, String> colSupplier;
	@FXML
	private TableColumn<Shipment, String> colBranch;
	@FXML
	private TableColumn<Shipment, String> colWarehouse;
	@FXML
	private TableColumn<Shipment, Number> colQty;
	@FXML
	private TableColumn<Shipment, Number> colUnit;
	@FXML
	private TableColumn<Shipment, Number> colTotal;

	@FXML
	private TextField txtSearch;
	@FXML
	private Button btnAdd;

	private final ObservableList<Shipment> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		colId.setStyle("-fx-alignment: CENTER;");
		colDate.setStyle("-fx-alignment: CENTER;");
		colProduct.setStyle("-fx-alignment: CENTER;");
		colSupplier.setStyle("-fx-alignment: CENTER;");
		colBranch.setStyle("-fx-alignment: CENTER;");
		colWarehouse.setStyle("-fx-alignment: CENTER;");
		colQty.setStyle("-fx-alignment: CENTER;");
		colUnit.setStyle("-fx-alignment: CENTER;");
		colTotal.setStyle("-fx-alignment: CENTER;");

		colId.setCellValueFactory(d -> d.getValue().shipmentIdProperty());
		colDate.setCellValueFactory(d -> d.getValue().shipmentDateProperty());
		colProduct.setCellValueFactory(d -> d.getValue().productNameProperty());
		colSupplier.setCellValueFactory(d -> d.getValue().supplierNameProperty());
		colBranch.setCellValueFactory(d -> d.getValue().branchNameProperty());
		colWarehouse.setCellValueFactory(d -> d.getValue().warehouseNameProperty());
		colQty.setCellValueFactory(d -> d.getValue().quantityProperty());
		colUnit.setCellValueFactory(d -> d.getValue().unitCostProperty());
		colTotal.setCellValueFactory(d -> d.getValue().totalCostProperty());

		load();

		txtSearch.textProperty().addListener((obs, o, n) -> filter(n));
	}

	private void load() {
		list.setAll(ShipmentDAO.getAllShipments());
		table.setItems(list);
	}

	private void filter(String key) {
		if (key == null || key.isBlank()) {
			table.setItems(list);
			return;
		}

		String k = key.toLowerCase();
		ObservableList<Shipment> filtered = FXCollections.observableArrayList();

		for (Shipment s : list) {
			String p = s.getProductName() == null ? "" : s.getProductName().toLowerCase();
			String sup = s.getSupplierName() == null ? "" : s.getSupplierName().toLowerCase();
			String b = s.getBranchName() == null ? "" : s.getBranchName().toLowerCase();
			String w = s.getWarehouseName() == null ? "" : s.getWarehouseName().toLowerCase();

			if (p.contains(k) || sup.contains(k) || b.contains(k) || w.contains(k)) {
				filtered.add(s);
			}
		}

		table.setItems(filtered);
	}

	@FXML
	private void onRefresh() {
		load();
	}

	@FXML
	private void onNewShipment() {
		go("ShipmentProducts.fxml");
	}

	private void go(String fxml) {
		try {
			Parent view = FXMLLoader.load(getClass().getResource(fxml));
			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
