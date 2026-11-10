package application;

import javafx.beans.property.*;

public class Product {

	private final IntegerProperty productId = new SimpleIntegerProperty();
	private final IntegerProperty categoryId = new SimpleIntegerProperty();

	private final IntegerProperty branchId = new SimpleIntegerProperty();
	private final IntegerProperty warehouseId = new SimpleIntegerProperty();
	private final StringProperty branchName = new SimpleStringProperty();
	private final StringProperty warehouseName = new SimpleStringProperty();

	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty description = new SimpleStringProperty();
	private final StringProperty baseUnit = new SimpleStringProperty();

	private final IntegerProperty cartonsPerPallet = new SimpleIntegerProperty();
	private final IntegerProperty quantity = new SimpleIntegerProperty();
	private final IntegerProperty minQuantity = new SimpleIntegerProperty();
	private final IntegerProperty maxQuantity = new SimpleIntegerProperty();

	private final DoubleProperty costPrice = new SimpleDoubleProperty();
	private final DoubleProperty sellingPrice = new SimpleDoubleProperty();

	private final StringProperty imagePath = new SimpleStringProperty();

	public Product() {
	}

	// ✅ updated constructor (include branch/warehouse + names)
	public Product(int productId, int categoryId, int branchId, int warehouseId, String branchName,
			String warehouseName, String name, String description, String baseUnit, int cartonsPerPallet, int quantity,
			int minQuantity, int maxQuantity, double costPrice, double sellingPrice, String imagePath) {

		this.productId.set(productId);
		this.categoryId.set(categoryId);

		this.branchId.set(branchId);
		this.warehouseId.set(warehouseId);
		this.branchName.set(branchName);
		this.warehouseName.set(warehouseName);

		this.name.set(name);
		this.description.set(description);
		this.baseUnit.set(baseUnit);

		this.cartonsPerPallet.set(cartonsPerPallet);
		this.quantity.set(quantity);
		this.minQuantity.set(minQuantity);
		this.maxQuantity.set(maxQuantity);

		this.costPrice.set(costPrice);
		this.sellingPrice.set(sellingPrice);

		this.imagePath.set(imagePath);
	}

	public int getProductId() {
		return productId.get();
	}

	public int getCategoryId() {
		return categoryId.get();
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

	public String getName() {
		return name.get();
	}

	public String getDescription() {
		return description.get();
	}

	public String getBaseUnit() {
		return baseUnit.get();
	}

	public int getCartonsPerPallet() {
		return cartonsPerPallet.get();
	}

	public int getQuantity() {
		return quantity.get();
	}

	public int getMinQuantity() {
		return minQuantity.get();
	}

	public int getMaxQuantity() {
		return maxQuantity.get();
	}

	public double getCostPrice() {
		return costPrice.get();
	}

	public double getSellingPrice() {
		return sellingPrice.get();
	}

	public String getImagePath() {
		return imagePath.get();
	}

	public void setProductId(int v) {
		productId.set(v);
	}

	public void setCategoryId(int v) {
		categoryId.set(v);
	}

	public void setBranchId(int v) {
		branchId.set(v);
	}

	public void setWarehouseId(int v) {
		warehouseId.set(v);
	}

	public void setBranchName(String v) {
		branchName.set(v);
	}

	public void setWarehouseName(String v) {
		warehouseName.set(v);
	}

	public void setName(String v) {
		name.set(v);
	}

	public void setDescription(String v) {
		description.set(v);
	}

	public void setBaseUnit(String v) {
		baseUnit.set(v);
	}

	public void setCartonsPerPallet(int v) {
		cartonsPerPallet.set(v);
	}

	public void setQuantity(int v) {
		quantity.set(v);
	}

	public void setMinQuantity(int v) {
		minQuantity.set(v);
	}

	public void setMaxQuantity(int v) {
		maxQuantity.set(v);
	}

	public void setCostPrice(double v) {
		costPrice.set(v);
	}

	public void setSellingPrice(double v) {
		sellingPrice.set(v);
	}

	public void setImagePath(String v) {
		imagePath.set(v);
	}

	public IntegerProperty productIdProperty() {
		return productId;
	}

	public IntegerProperty categoryIdProperty() {
		return categoryId;
	}

	public IntegerProperty branchIdProperty() {
		return branchId;
	}

	public IntegerProperty warehouseIdProperty() {
		return warehouseId;
	}

	public StringProperty branchNameProperty() {
		return branchName;
	}

	public StringProperty warehouseNameProperty() {
		return warehouseName;
	}

	public StringProperty nameProperty() {
		return name;
	}

	public StringProperty descriptionProperty() {
		return description;
	}

	public StringProperty baseUnitProperty() {
		return baseUnit;
	}

	public IntegerProperty cartonsPerPalletProperty() {
		return cartonsPerPallet;
	}

	public IntegerProperty quantityProperty() {
		return quantity;
	}

	public IntegerProperty minQuantityProperty() {
		return minQuantity;
	}

	public IntegerProperty maxQuantityProperty() {
		return maxQuantity;
	}

	public DoubleProperty costPriceProperty() {
		return costPrice;
	}

	public DoubleProperty sellingPriceProperty() {
		return sellingPrice;
	}

	public StringProperty imagePathProperty() {
		return imagePath;
	}

	public String getStockStatus() {
		if (quantity.get() < minQuantity.get())
			return "LOW";
		else if (quantity.get() <= maxQuantity.get())
			return "NORMAL";
		else
			return "HIGH";
	}
}
