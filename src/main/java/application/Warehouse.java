package application;

import javafx.beans.property.*;

public class Warehouse {

	private final IntegerProperty warehouseId = new SimpleIntegerProperty();
	private final IntegerProperty branchId = new SimpleIntegerProperty();
	private final StringProperty name = new SimpleStringProperty();
	private final IntegerProperty palletCapacity = new SimpleIntegerProperty();

	public Warehouse(int warehouseId, int branchId, String name, int palletCapacity) {
		this.warehouseId.set(warehouseId);
		this.branchId.set(branchId);
		this.name.set(name);
		this.palletCapacity.set(palletCapacity);
	}

	// ===== getters =====
	public int getWarehouseId() {
		return warehouseId.get();
	}

	public int getBranchId() {
		return branchId.get();
	}

	public String getName() {
		return name.get();
	}

	public int getPalletCapacity() {
		return palletCapacity.get();
	}

	// ===== properties (for TableView) =====
	public IntegerProperty warehouseIdProperty() {
		return warehouseId;
	}

	public IntegerProperty branchIdProperty() {
		return branchId;
	}

	public StringProperty nameProperty() {
		return name;
	}

	public IntegerProperty palletCapacityProperty() {
		return palletCapacity;
	}
	@Override
	public String toString() {
		return getName();
	}
}
