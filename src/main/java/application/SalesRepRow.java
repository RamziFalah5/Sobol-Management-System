package application;

import javafx.beans.property.*;
import java.time.LocalDate;

public class SalesRepRow {

	private final IntegerProperty employeeId = new SimpleIntegerProperty();
	private final StringProperty fullName = new SimpleStringProperty();
	private final StringProperty phone = new SimpleStringProperty();

	private final IntegerProperty branchId = new SimpleIntegerProperty();
	private final StringProperty branchName = new SimpleStringProperty();

	private final ObjectProperty<LocalDate> hireDate = new SimpleObjectProperty<>();
	private final StringProperty imagePath = new SimpleStringProperty();

	private final DoubleProperty dailyRate = new SimpleDoubleProperty();
	private final DoubleProperty commissionRate = new SimpleDoubleProperty();

	public SalesRepRow(int id, String name, String phone, int branchId, String branchName, LocalDate hireDate,
			String imagePath, double dailyRate, double commissionRate) {

		this.employeeId.set(id);
		this.fullName.set(name);
		this.phone.set(phone);

		this.branchId.set(branchId);
		this.branchName.set(branchName);

		this.hireDate.set(hireDate);
		this.imagePath.set(imagePath);

		this.dailyRate.set(dailyRate);
		this.commissionRate.set(commissionRate);
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

	public LocalDate getHireDate() {
		return hireDate.get();
	}

	public String getImagePath() {
		return imagePath.get();
	}

	public double getDailyRate() {
		return dailyRate.get();
	}

	public double getCommissionRate() {
		return commissionRate.get();
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

	public ObjectProperty<LocalDate> hireDateProperty() {
		return hireDate;
	}

	public StringProperty imagePathProperty() {
		return imagePath;
	}

	public DoubleProperty dailyRateProperty() {
		return dailyRate;
	}

	public DoubleProperty commissionRateProperty() {
		return commissionRate;
	}
}
