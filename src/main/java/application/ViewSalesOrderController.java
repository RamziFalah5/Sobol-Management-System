package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class ViewSalesOrderController {

	@FXML
	private Label lblTitle;
	@FXML
	private Label lblCustomer;
	@FXML
	private Label lblEmployee;
	@FXML
	private Label lblDate;
	@FXML
	private Label lblTotal;

	@FXML
	private TableView<SalesItem> table;
	@FXML
	private TableColumn<SalesItem, String> colProduct;
	@FXML
	private TableColumn<SalesItem, Number> colQty;
	@FXML
	private TableColumn<SalesItem, Number> colUnit;
	@FXML
	private TableColumn<SalesItem, Number> colLine;

	private final ObservableList<SalesItem> list = FXCollections.observableArrayList();
	private SalesOrder order;

	public void setOrder(SalesOrder o) {
		this.order = o;

		lblTitle.setText("Order View - #" + o.getOrderId());
		lblCustomer.setText("Customer: " + o.getCustomerName());
		lblEmployee.setText("Employee: " + o.getEmployeeName());
		lblDate.setText("Date: " + o.getOrderDate());
		lblTotal.setText(String.format("Total: %.2f", o.getTotalAmount()));

		loadItems();
	}

	@FXML
	public void initialize() {
		colProduct.setCellValueFactory(d -> d.getValue().productNameProperty());
		colQty.setCellValueFactory(d -> d.getValue().quantityProperty());
		colUnit.setCellValueFactory(d -> d.getValue().unitPriceProperty());
		colLine.setCellValueFactory(d -> d.getValue().lineTotalProperty());
	}

	private void loadItems() {
		if (order == null)
			return;
		list.setAll(SalesDAO.getOrderItems(order.getOrderId()));
		table.setItems(list);
	}

	@FXML
	private void onBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Sales.fxml"));
			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
