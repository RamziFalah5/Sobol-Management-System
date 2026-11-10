package application;

import com.sobol.supplier.application.SupplierService;
import com.sobol.supplier.application.SupplierException;
import java.util.Objects;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class SuppliersController {

	private final SupplierService service;
	public SuppliersController() { this(SupplierModule.service()); }
	SuppliersController(SupplierService service) { this.service = Objects.requireNonNull(service); }

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
		try {
			list.setAll(service.listSuppliers().stream().map(Supplier::fromDomain).toList());
		} catch (SupplierException failure) {
			list.clear();
			alert("Error", failure.getMessage());
		}
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
		if (s == null || s.getSupplierId() <= 0 || !list.contains(s)) {
			alert(title, "Please select a supplier first.");
			return null;
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
			FXMLLoader loader = createViewLoader("EditSupplier.fxml");
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

		alert("PostgreSQL transition", "Supplier shipments are unavailable until receiving and inventory migrate together.");
	}

	private void go(String fxml) {
		try {
			Parent view = createViewLoader(fxml).load();
			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	FXMLLoader createViewLoader(String fxml) {
		return new FXMLLoader(getClass().getResource(fxml));
	}

	void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
