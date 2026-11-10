package application;

public class SalesRepresentative extends Employee {
	private double dailyRate;
	private double commissionRate;

	public SalesRepresentative(double dailyRate, double commissionRate) {
		this.dailyRate = dailyRate;
		this.commissionRate = commissionRate;
	}

	public double getDailyRate() {
		return dailyRate;
	}

	public double getCommissionRate() {
		return commissionRate;
	}
}
