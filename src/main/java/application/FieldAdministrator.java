package application;

public class FieldAdministrator extends Employee {
	private double dailyRate;
	private int managedBranchId;

	public FieldAdministrator(double dailyRate, int managedBranchId) {
		this.dailyRate = dailyRate;
		this.managedBranchId = managedBranchId;
	}

	public double getDailyRate() {
		return dailyRate;
	}

	public int getManagedBranchId() {
		return managedBranchId;
	}
}
