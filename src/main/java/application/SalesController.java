package application;

import java.time.LocalDate;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class SalesController {

	@FXML
	private TableView<SalesOrder> table;
	@FXML
	private TableColumn<SalesOrder, Number> colId;
	@FXML
	private TableColumn<SalesOrder, LocalDate> colDate;
	@FXML
	private TableColumn<SalesOrder, String> colCustomer;
	@FXML
	private TableColumn<SalesOrder, String> colEmployee;
	@FXML
	private TableColumn<SalesOrder, Number> colItems;
	@FXML
	private TableColumn<SalesOrder, Number> colTotal;

	@FXML
	private TextField txtSearch;

	private final ObservableList<SalesOrder> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		colId.setCellValueFactory(d -> d.getValue().orderIdProperty());

		colDate.setCellValueFactory(d -> d.getValue().orderDateProperty());

		colCustomer.setCellValueFactory(d -> d.getValue().customerNameProperty());
		colEmployee.setCellValueFactory(d -> d.getValue().employeeNameProperty());
		colItems.setCellValueFactory(d -> d.getValue().itemsCountProperty());
		colTotal.setCellValueFactory(d -> d.getValue().totalAmountProperty());

		load();

		txtSearch.textProperty().addListener((obs, o, n) -> {
			if (n == null || n.isBlank())
				load();
			else {
				list.setAll(SalesDAO.searchOrdersByCustomerName(n));
				table.setItems(list);
			}
		});
	}

	private void load() {
		list.setAll(loadOrders());
		table.setItems(list);
	}

	@FXML
	private void onNewOrder() {
		go("PickCustomerForSale.fxml");
	}

	@FXML
	private void onView() {
		SalesOrder sel = table.getSelectionModel().getSelectedItem();
		if (sel == null) {
			alert("View Order", "Please select an order first.");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("ViewSalesOrder.fxml"));
			Parent view = loader.load();

			ViewSalesOrderController c = loader.getController();
			c.setOrder(sel);

			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	private void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}

	java.util.List<SalesOrder> loadOrders() { return SalesDAO.getAllOrders(); }
	FXMLLoader createViewLoader(String fxml) { return new FXMLLoader(getClass().getResource(fxml)); }
}
