package application;

import javafx.beans.property.*;

public class WorkerRow {
	private final IntegerProperty employeeId = new SimpleIntegerProperty();
	private final StringProperty fullName = new SimpleStringProperty();
	private final StringProperty phone = new SimpleStringProperty();

	private final IntegerProperty branchId = new SimpleIntegerProperty();
	private final StringProperty branchName = new SimpleStringProperty();

	private final DoubleProperty rate = new SimpleDoubleProperty(); // daily or hourly

	public WorkerRow(int id, String name, String phone, int branchId, String branchName, double rate) {
		this.employeeId.set(id);
		this.fullName.set(name);
		this.phone.set(phone);
		this.branchId.set(branchId);
		this.branchName.set(branchName);
		this.rate.set(rate);
	}

	public int getEmployeeId() {
		return employeeId.get();
	}

	public String getFullName() {
		return fullName.get();
	}

	public String getPhone() {
		return phone.get();
	}

	public int getBranchId() {
		return branchId.get();
	}

	public String getBranchName() {
		return branchName.get();
	}

	public double getRate() {
		return rate.get();
	}

	public IntegerProperty employeeIdProperty() {
		return employeeId;
	}

	public StringProperty fullNameProperty() {
		return fullName;
	}

	public StringProperty phoneProperty() {
		return phone;
	}

	public IntegerProperty branchIdProperty() {
		return branchId;
	}

	public StringProperty branchNameProperty() {
		return branchName;
	}

	public DoubleProperty rateProperty() {
		return rate;
	}
}
