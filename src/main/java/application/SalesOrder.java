package application;

import javafx.beans.property.*;
import java.time.LocalDate;

public class SalesOrder {

	private final IntegerProperty orderId = new SimpleIntegerProperty();
	private final IntegerProperty customerId = new SimpleIntegerProperty();
	private final IntegerProperty employeeId = new SimpleIntegerProperty();

	private final StringProperty customerName = new SimpleStringProperty();
	private final StringProperty employeeName = new SimpleStringProperty();

	private final ObjectProperty<LocalDate> orderDate = new SimpleObjectProperty<>();
	private final DoubleProperty totalAmount = new SimpleDoubleProperty();

	private final IntegerProperty itemsCount = new SimpleIntegerProperty();

	public SalesOrder() {
	}

	public SalesOrder(int orderId, int customerId, int employeeId, String customerName, String employeeName,
			LocalDate orderDate, double totalAmount, int itemsCount) {

		this.orderId.set(orderId);
		this.customerId.set(customerId);
		this.employeeId.set(employeeId);

		this.customerName.set(customerName);
		this.employeeName.set(employeeName);

		this.orderDate.set(orderDate);
		this.totalAmount.set(totalAmount);
		this.itemsCount.set(itemsCount);
	}

	public int getOrderId() {
		return orderId.get();
	}

	public int getCustomerId() {
		return customerId.get();
	}

	public int getEmployeeId() {
		return employeeId.get();
	}

	public String getCustomerName() {
		return customerName.get();
	}

	public String getEmployeeName() {
		return employeeName.get();
	}

	public LocalDate getOrderDate() {
		return orderDate.get();
	}

	public double getTotalAmount() {
		return totalAmount.get();
	}

	public int getItemsCount() {
		return itemsCount.get();
	}

	public IntegerProperty orderIdProperty() {
		return orderId;
	}

	public StringProperty customerNameProperty() {
		return customerName;
	}

	public StringProperty employeeNameProperty() {
		return employeeName;
	}

	public ObjectProperty<LocalDate> orderDateProperty() {
		return orderDate;
	}

	public DoubleProperty totalAmountProperty() {
		return totalAmount;
	}

	public IntegerProperty itemsCountProperty() {
		return itemsCount;
	}
}
