package application;

import javafx.beans.property.*;

public class HourlyAttendanceRow {
	private final IntegerProperty employeeId = new SimpleIntegerProperty();
	private final StringProperty fullName = new SimpleStringProperty();
	private final IntegerProperty hours = new SimpleIntegerProperty(3);

	public HourlyAttendanceRow(int empId, String name, int hours) {
		this.employeeId.set(empId);
		this.fullName.set(name);
		this.hours.set(hours);
	}

	public int getEmployeeId() {
		return employeeId.get();
	}

	public String getFullName() {
		return fullName.get();
	}

	public int getHours() {
		return hours.get();
	}

	public IntegerProperty employeeIdProperty() {
		return employeeId;
	}

	public StringProperty fullNameProperty() {
		return fullName;
	}

	public IntegerProperty hoursProperty() {
		return hours;
	}
}
