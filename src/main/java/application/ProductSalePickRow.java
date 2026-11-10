package application;

import javafx.beans.property.*;

public class ProductSalePickRow {

	private final IntegerProperty productId = new SimpleIntegerProperty();
	private final StringProperty productName = new SimpleStringProperty();

	private final IntegerProperty availableQty = new SimpleIntegerProperty();
	private final DoubleProperty sellingPrice = new SimpleDoubleProperty();

	// ✅ الكمية المختارة (الجديد)
	private final IntegerProperty pickedQty = new SimpleIntegerProperty(0);

	private final BooleanProperty selected = new SimpleBooleanProperty(false);

	public ProductSalePickRow(int productId, String productName, int availableQty, double sellingPrice) {
		this.productId.set(productId);
		this.productName.set(productName);
		this.availableQty.set(availableQty);
		this.sellingPrice.set(sellingPrice);
		this.pickedQty.set(0);
	}

	/* ===================== GETTERS ===================== */

	public int getProductId() {
		return productId.get();
	}

	public String getProductName() {
		return productName.get();
	}

	public int getAvailableQty() {
		return availableQty.get();
	}

	public double getSellingPrice() {
		return sellingPrice.get();
	}

	public int getPickedQty() {
		return pickedQty.get();
	}

	public boolean isSelected() {
		return selected.get();
	}

	/* ===================== SETTERS ===================== */

	public void setPickedQty(int qty) {
		this.pickedQty.set(qty);
	}

	public void setSelected(boolean v) {
		selected.set(v);
	}

	/* ===================== PROPERTIES ===================== */

	public IntegerProperty productIdProperty() {
		return productId;
	}

	public StringProperty productNameProperty() {
		return productName;
	}

	public IntegerProperty availableQtyProperty() {
		return availableQty;
	}

	public DoubleProperty sellingPriceProperty() {
		return sellingPrice;
	}

	public IntegerProperty pickedQtyProperty() {
		return pickedQty;
	}

	public BooleanProperty selectedProperty() {
		return selected;
	}
}
