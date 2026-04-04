package application;

import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.List;

public class Branch {

	private final IntegerProperty branchId = new SimpleIntegerProperty();
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty city = new SimpleStringProperty();
	private final StringProperty phone = new SimpleStringProperty();

	private final IntegerProperty warehousesCount = new SimpleIntegerProperty(0);

	private final IntegerProperty totalCapacity = new SimpleIntegerProperty(0);

	private final List<Warehouse> warehouses = new ArrayList<>();

	public Branch(int branchId, String name, String city, String phone, int warehousesCount, int totalCapacity) {

		this.branchId.set(branchId);
		this.name.set(name);
		this.city.set(city);
		this.phone.set(phone);
		this.warehousesCount.set(warehousesCount);
		this.totalCapacity.set(totalCapacity);
	}

	public Branch(String name, String city, String phone) {
		this.name.set(name);
		this.city.set(city);
		this.phone.set(phone);
	}

	public int getBranchId() {
		return branchId.get();
	}

	public String getName() {
		return name.get();
	}

	public String getCity() {
		return city.get();
	}

	public String getPhone() {
		return phone.get();
	}

	public int getWarehousesCount() {
		return warehousesCount.get();
	}

	public int getTotalCapacity() {
		return totalCapacity.get();
	}

	public IntegerProperty branchIdProperty() {
		return branchId;
	}

	public StringProperty nameProperty() {
		return name;
	}

	public StringProperty cityProperty() {
		return city;
	}

	public StringProperty phoneProperty() {
		return phone;
	}

	public IntegerProperty warehousesCountProperty() {
		return warehousesCount;
	}

	public IntegerProperty totalCapacityProperty() {
		return totalCapacity;
	}

	public List<Warehouse> getWarehouses() {
		return warehouses;
	}
	@Override
	public String toString() {
		return getName();
	}
}
