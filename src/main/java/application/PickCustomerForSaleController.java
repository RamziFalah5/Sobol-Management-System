package application;

import com.sobol.customer.application.CustomerService;
import com.sobol.customer.application.CustomerException;
import java.util.Objects;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class PickCustomerForSaleController {

	private final CustomerService service;
	public PickCustomerForSaleController() { this(CustomerModule.service()); }
	PickCustomerForSaleController(CustomerService service) { this.service = Objects.requireNonNull(service); }

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
		try {
			list.setAll(service.listCustomers().stream().map(Customer::fromDomain).toList());
		} catch (CustomerException failure) {
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
		if (sel == null || sel.getCustomerId() <= 0 || !list.contains(sel)) {
			alert("Select Customer", "Please select a customer first.");
			return;
		}

		try {
			Customer snapshot = Customer.fromDomain(sel.toDomain());
			FXMLLoader loader = createViewLoader("PickProductsForSale.fxml");
			Parent view = loader.load();

			PickProductsForSaleController c = loader.getController();
			c.setCustomer(snapshot);

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
			Parent view = createViewLoader(fxml).load();
			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	FXMLLoader createViewLoader(String fxml) { return new FXMLLoader(getClass().getResource(fxml)); }

	void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
