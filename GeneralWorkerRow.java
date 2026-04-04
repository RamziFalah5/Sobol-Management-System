package application;

import javafx.beans.property.*;

public class GeneralWorkerRow {
	private final IntegerProperty employeeId = new SimpleIntegerProperty();
	private final StringProperty fullName = new SimpleStringProperty();
	private final StringProperty phone = new SimpleStringProperty();
	private final IntegerProperty branchId = new SimpleIntegerProperty();
	private final StringProperty branchName = new SimpleStringProperty();

	// rate = daily_rate أو hourly_rate حسب الشاشة
	private final DoubleProperty rate = new SimpleDoubleProperty();

	private final StringProperty hireDate = new SimpleStringProperty(); // yyyy-mm-dd
	private final StringProperty imagePath = new SimpleStringProperty();

	public GeneralWorkerRow(int id, String name, String phone, int branchId, String branchName, double rate,
			String hireDate, String imagePath) {
		this.employeeId.set(id);
		this.fullName.set(name);
		this.phone.set(phone);
		this.branchId.set(branchId);
		this.branchName.set(branchName);
		this.rate.set(rate);
		this.hireDate.set(hireDate);
		this.imagePath.set(imagePath);
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

	public String getHireDate() {
		return hireDate.get();
	}

	public String getImagePath() {
		return imagePath.get();
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

	public StringProperty hireDateProperty() {
		return hireDate;
	}

	public StringProperty imagePathProperty() {
		return imagePath;
	}
}
