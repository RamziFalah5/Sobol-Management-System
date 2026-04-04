package application;

public class GeneralWorker extends Employee {
	public enum Mode {
		DAILY, HOURLY
	}

	private Mode mode;
	private double dailyRate;
	private double hourlyRate;

	public Mode getMode() {
		return mode;
	}

	public double getDailyRate() {
		return dailyRate;
	}

	public double getHourlyRate() {
		return hourlyRate;
	}

	public GeneralWorker(Mode mode, double dailyRate, double hourlyRate) {
		this.mode = mode;
		this.dailyRate = dailyRate;
		this.hourlyRate = hourlyRate;
	}
}
