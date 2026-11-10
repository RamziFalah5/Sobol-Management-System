package application;

import javafx.beans.property.*;

public class ProductPickRow {

	private final IntegerProperty productId = new SimpleIntegerProperty();
	private final StringProperty imagePath = new SimpleStringProperty();

	private final StringProperty productName = new SimpleStringProperty();
	private final StringProperty categoryName = new SimpleStringProperty();

	private final IntegerProperty branchId = new SimpleIntegerProperty();
	private final IntegerProperty warehouseId = new SimpleIntegerProperty();

	private final StringProperty branchName = new SimpleStringProperty();
	private final StringProperty warehouseName = new SimpleStringProperty();

	private final IntegerProperty cartonsPerPallet = new SimpleIntegerProperty();
	private final IntegerProperty quantity = new SimpleIntegerProperty();
	private final DoubleProperty costPrice = new SimpleDoubleProperty();

	public ProductPickRow(int productId, String imagePath, String productName, String categoryName, int branchId,
			int warehouseId, String branchName, String warehouseName, int cartonsPerPallet, int quantity,
			double costPrice) {

		this.productId.set(productId);
		this.imagePath.set(imagePath);

		this.productName.set(productName);
		this.categoryName.set(categoryName);

		this.branchId.set(branchId);
		this.warehouseId.set(warehouseId);

		this.branchName.set(branchName);
		this.warehouseName.set(warehouseName);

		this.cartonsPerPallet.set(cartonsPerPallet);
		this.quantity.set(quantity);
		this.costPrice.set(costPrice);
	}

	public int getProductId() {
		return productId.get();
	}

	public String getImagePath() {
		return imagePath.get();
	}

	public String getProductName() {
		return productName.get();
	}

	public String getCategoryName() {
		return categoryName.get();
	}

	public int getBranchId() {
		return branchId.get();
	}

	public int getWarehouseId() {
		return warehouseId.get();
	}

	public String getBranchName() {
		return branchName.get();
	}

	public String getWarehouseName() {
		return warehouseName.get();
	}

	public int getCartonsPerPallet() {
		return cartonsPerPallet.get();
	}

	public int getQuantity() {
		return quantity.get();
	}

	public double getCostPrice() {
		return costPrice.get();
	}

	public IntegerProperty productIdProperty() {
		return productId;
	}

	public StringProperty imagePathProperty() {
		return imagePath;
	}

	public StringProperty productNameProperty() {
		return productName;
	}

	public StringProperty categoryNameProperty() {
		return categoryName;
	}

	public StringProperty branchNameProperty() {
		return branchName;
	}

	public StringProperty warehouseNameProperty() {
		return warehouseName;
	}

	public IntegerProperty cartonsPerPalletProperty() {
		return cartonsPerPallet;
	}

	public IntegerProperty quantityProperty() {
		return quantity;
	}

	public DoubleProperty costPriceProperty() {
		return costPrice;
	}
}
