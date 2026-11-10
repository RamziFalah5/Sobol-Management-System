package application;

import java.time.LocalDate;

public abstract class Employee {
	protected int employeeId;
	protected String fullName;
	protected String phone;
	protected int branchId;
	protected LocalDate hireDate;
	protected String imagePath;

	public int getEmployeeId() {
		return employeeId;
	}

	public String getFullName() {
		return fullName;
	}

	public String getPhone() {
		return phone;
	}

	public int getBranchId() {
		return branchId;
	}

	public LocalDate getHireDate() {
		return hireDate;
	}

	public String getImagePath() {
		return imagePath;
	}
}
