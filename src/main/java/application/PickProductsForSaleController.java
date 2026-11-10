package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

public class PickProductsForSaleController {

	@FXML
	private Label lblTitle;

	@FXML
	private TableView<ProductSalePickRow> table;
	@FXML
	private TableColumn<ProductSalePickRow, String> colName;
	@FXML
	private TableColumn<ProductSalePickRow, Boolean> colPick;

	@FXML
	private TextField txtSearch;

	private final ObservableList<ProductSalePickRow> list = FXCollections.observableArrayList();

	private com.sobol.customer.domain.Customer customer;

	public void setCustomer(Customer c) {
		customer = null;
		if (c == null) throw new IllegalArgumentException("A valid selected customer is required.");
		customer = c.toDomain();
		lblTitle.setText("Pick Products - Customer: " + customer.details().fullName());
		load();
	}

	@FXML
	public void initialize() {
		colName.setCellValueFactory(d -> d.getValue().productNameProperty());

		// ✅ checkbox that toggles (true/false) when clicked
		colPick.setCellValueFactory(d -> d.getValue().selectedProperty());
		colPick.setCellFactory(CheckBoxTableCell.forTableColumn(colPick));
		table.setEditable(true);
		colPick.setEditable(true);

		txtSearch.textProperty().addListener((obs, o, n) -> filter(n));
	}

	private void load() {
		list.setAll(loadProducts());
		table.setItems(list);
	}

	private void filter(String key) {
		if (key == null || key.isBlank()) {
			table.setItems(list);
			return;
		}
		String k = key.toLowerCase();

		ObservableList<ProductSalePickRow> filtered = FXCollections.observableArrayList();
		for (ProductSalePickRow p : list) {
			String name = p.getProductName() == null ? "" : p.getProductName().toLowerCase();
			if (name.contains(k))
				filtered.add(p);
		}
		table.setItems(filtered);
	}

	@FXML
	private void onSelectQuantity() {

		if (customer == null) {
			alert("Error", "No customer selected.");
			return;
		}

		List<ProductSalePickRow> picked = new ArrayList<>();
		for (ProductSalePickRow r : list) {
			if (r.isSelected())
				picked.add(r);
		}

		if (picked.isEmpty()) {
			alert("Pick Products", "Please select at least one product.");
			return;
		}

		try {
			FXMLLoader loader = createViewLoader("SaleQuantities.fxml");
			Parent view = loader.load();

			SaleQuantitiesController c = loader.getController();
			c.setData(Customer.fromDomain(customer), picked);

			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onBack() {
		go("PickCustomerForSale.fxml");
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

	List<ProductSalePickRow> loadProducts() { return SalesDAO.getAllProductsForSale(); }
	FXMLLoader createViewLoader(String fxml) { return new FXMLLoader(getClass().getResource(fxml)); }

	void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
