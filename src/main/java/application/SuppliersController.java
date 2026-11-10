package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class SuppliersController {

	@FXML
	private TableView<Supplier> table;

	@FXML
	private TableColumn<Supplier, String> colName;
	@FXML
	private TableColumn<Supplier, String> colPhone;
	@FXML
	private TableColumn<Supplier, String> colEmail;
	@FXML
	private TableColumn<Supplier, String> colCity;
	@FXML
	private TableColumn<Supplier, String> colContact;

	@FXML
	private TextField txtSearch;

	private final ObservableList<Supplier> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		colName.setCellValueFactory(d -> d.getValue().nameProperty());
		colPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
		colEmail.setCellValueFactory(d -> d.getValue().emailProperty());
		colCity.setCellValueFactory(d -> d.getValue().cityProperty());
		colContact.setCellValueFactory(d -> d.getValue().contactPersonProperty());

		load();

		txtSearch.textProperty().addListener((obs, o, n) -> filter(n));
	}

	private void load() {
		list.setAll(SupplierDAO.getAllSuppliers());
		table.setItems(list);
	}

	private void filter(String key) {

		if (key == null || key.isBlank()) {
			table.setItems(list);
			return;
		}

		String k = key.toLowerCase();
		ObservableList<Supplier> filtered = FXCollections.observableArrayList();

		for (Supplier s : list) {
			String name = s.getName() == null ? "" : s.getName().toLowerCase();
			String phone = s.getPhone() == null ? "" : s.getPhone().toLowerCase();
			String city = s.getCity() == null ? "" : s.getCity().toLowerCase();

			if (name.contains(k) || phone.contains(k) || city.contains(k)) {
				filtered.add(s);
			}
		}

		table.setItems(filtered);
	}

	private Supplier selectedOrAlert(String title) {
		Supplier s = table.getSelectionModel().getSelectedItem();
		if (s == null) {
			alert(title, "Please select a supplier first.");
		}
		return s;
	}

	@FXML
	private void onAdd() {
		go("AddSupplier.fxml");
	}

	@FXML
	private void onEdit() {
		Supplier sel = selectedOrAlert("Edit Supplier");
		if (sel == null)
			return;

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EditSupplier.fxml"));
			Parent view = loader.load();

			EditSupplierController c = loader.getController();
			c.setSupplier(sel);

			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ================= VIEW (Shipments of supplier) =================
	@FXML
	private void onView() {

		Supplier sel = selectedOrAlert("View Supplier Shipments");
		if (sel == null)
			return;

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("ViewSupplierShipments.fxml"));
			Parent view = loader.load();

			ViewSupplierShipmentsController c = loader.getController();
			c.setSupplier(sel);

			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	private void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
