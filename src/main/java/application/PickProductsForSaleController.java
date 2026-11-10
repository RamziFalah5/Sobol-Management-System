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

	private Customer customer;

	public void setCustomer(Customer c) {
		this.customer = c;
		lblTitle.setText("Pick Products - Customer: " + c.getFullName());
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
		list.setAll(SalesDAO.getAllProductsForSale());
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
			FXMLLoader loader = new FXMLLoader(getClass().getResource("SaleQuantities.fxml"));
			Parent view = loader.load();

			SaleQuantitiesController c = loader.getController();
			c.setData(customer, picked);

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
