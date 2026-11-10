package application;

import javafx.beans.property.*;

public class SalesItem {

	private final IntegerProperty productId = new SimpleIntegerProperty();
	private final StringProperty productName = new SimpleStringProperty();

	private final IntegerProperty quantity = new SimpleIntegerProperty();
	private final DoubleProperty unitPrice = new SimpleDoubleProperty();
	private final DoubleProperty lineTotal = new SimpleDoubleProperty();

	public SalesItem() {
	}

	public SalesItem(int productId, String productName, int quantity, double unitPrice, double lineTotal) {
		this.productId.set(productId);
		this.productName.set(productName);
		this.quantity.set(quantity);
		this.unitPrice.set(unitPrice);
		this.lineTotal.set(lineTotal);
	}

	public int getProductId() {
		return productId.get();
	}

	public String getProductName() {
		return productName.get();
	}

	public int getQuantity() {
		return quantity.get();
	}

	public double getUnitPrice() {
		return unitPrice.get();
	}

	public double getLineTotal() {
		return lineTotal.get();
	}

	public IntegerProperty productIdProperty() {
		return productId;
	}

	public StringProperty productNameProperty() {
		return productName;
	}

	public IntegerProperty quantityProperty() {
		return quantity;
	}

	public DoubleProperty unitPriceProperty() {
		return unitPrice;
	}

	public DoubleProperty lineTotalProperty() {
		return lineTotal;
	}
}
