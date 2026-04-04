package application;

public class TruckDriver extends Employee {
	private double dailyRate;

	public TruckDriver(double dailyRate) {
		this.dailyRate = dailyRate;
	}

	public double getDailyRate() {
		return dailyRate;
	}
}
