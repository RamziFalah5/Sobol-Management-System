package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class CustomersController {

	@FXML
	private TableView<Customer> table;
	@FXML
	private TableColumn<Customer, String> colName;
	@FXML
	private TableColumn<Customer, String> colPhone;
	@FXML
	private TableColumn<Customer, String> colEmail;
	@FXML
	private TableColumn<Customer, String> colCity;
	@FXML
	private TableColumn<Customer, String> colAddress;
	@FXML
	private TextField txtSearch;

	private final ObservableList<Customer> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		colName.setCellValueFactory(d -> d.getValue().fullNameProperty());
		colPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
		colEmail.setCellValueFactory(d -> d.getValue().emailProperty());
		colCity.setCellValueFactory(d -> d.getValue().cityProperty());
		colAddress.setCellValueFactory(d -> d.getValue().addressProperty());

		// ✅ Center text in all table columns cells
		centerAllColumns();

		load();

		txtSearch.textProperty().addListener((obs, o, n) -> filter(n));
	}

	private void centerAllColumns() {
		for (TableColumn<Customer, ?> c : table.getColumns()) {
			c.setStyle("-fx-alignment: CENTER;");
		}
	}

	private void load() {
		list.setAll(CustomerDAO.getAllCustomers());
		table.setItems(list);
	}

	private void filter(String key) {
		if (key == null || key.isBlank()) {
			table.setItems(list);
			return;
		}

		String k = key.toLowerCase();
		ObservableList<Customer> filtered = FXCollections.observableArrayList();

		for (Customer c : list) {
			String name = c.getFullName() == null ? "" : c.getFullName().toLowerCase();
			String phone = c.getPhone() == null ? "" : c.getPhone().toLowerCase();
			String email = c.getEmail() == null ? "" : c.getEmail().toLowerCase();

			if (name.contains(k) || phone.contains(k) || email.contains(k)) {
				filtered.add(c);
			}
		}

		table.setItems(filtered);
	}

	@FXML
	private void onAdd() {
		go("AddCustomer.fxml");
	}

	@FXML
	private void onEdit() {
		Customer sel = table.getSelectionModel().getSelectedItem();
		if (sel == null) {
			alert("Edit", "Please select a customer to edit.");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EditCustomer.fxml"));
			Parent view = loader.load();

			EditCustomerController c = loader.getController();
			c.setCustomer(sel);

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
