package application;

import javafx.beans.property.*;

public class WarehouseProductRow {

	private final IntegerProperty productId = new SimpleIntegerProperty();
	private final StringProperty name = new SimpleStringProperty();
	private final IntegerProperty quantity = new SimpleIntegerProperty();
	private final IntegerProperty cartonsPerPallet = new SimpleIntegerProperty();
	private final IntegerProperty palletsUsed = new SimpleIntegerProperty();
	private final StringProperty imagePath = new SimpleStringProperty();

	public WarehouseProductRow(int productId, String name, int quantity, int cartonsPerPallet, int palletsUsed,
			String imagePath) {
		this.productId.set(productId);
		this.name.set(name);
		this.quantity.set(quantity);
		this.cartonsPerPallet.set(cartonsPerPallet);
		this.palletsUsed.set(palletsUsed);
		this.imagePath.set(imagePath);
	}

	public int getProductId() {
		return productId.get();
	}

	public String getName() {
		return name.get();
	}

	public int getQuantity() {
		return quantity.get();
	}

	public int getCartonsPerPallet() {
		return cartonsPerPallet.get();
	}

	public int getPalletsUsed() {
		return palletsUsed.get();
	}

	public String getImagePath() {
		return imagePath.get();
	}

	public IntegerProperty productIdProperty() {
		return productId;
	}

	public StringProperty nameProperty() {
		return name;
	}

	public IntegerProperty quantityProperty() {
		return quantity;
	}

	public IntegerProperty cartonsPerPalletProperty() {
		return cartonsPerPallet;
	}

	public IntegerProperty palletsUsedProperty() {
		return palletsUsed;
	}

	public StringProperty imagePathProperty() {
		return imagePath;
	}
}
