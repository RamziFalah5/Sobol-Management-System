package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class PickCustomerForSaleController {

	@FXML
	private TableView<Customer> table;
	@FXML
	private TableColumn<Customer, String> colName;
	@FXML
	private TableColumn<Customer, String> colPhone;
	@FXML
	private TableColumn<Customer, String> colEmail;
	@FXML
	private TextField txtSearch;

	private final ObservableList<Customer> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		colName.setCellValueFactory(d -> d.getValue().fullNameProperty());
		colPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
		colEmail.setCellValueFactory(d -> d.getValue().emailProperty());

		load();

		txtSearch.textProperty().addListener((obs, o, n) -> filter(n));
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
	private void onSelectCustomer() {
		Customer sel = table.getSelectionModel().getSelectedItem();
		if (sel == null) {
			alert("Select Customer", "Please select a customer first.");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("PickProductsForSale.fxml"));
			Parent view = loader.load();

			PickProductsForSaleController c = loader.getController();
			c.setCustomer(sel);

			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onBack() {
		go("Sales.fxml");
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
