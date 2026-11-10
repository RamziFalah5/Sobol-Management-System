package application;

import javafx.beans.property.*;

public class DailyAttendanceRow {
	private final IntegerProperty employeeId = new SimpleIntegerProperty();
	private final StringProperty fullName = new SimpleStringProperty();
	private final BooleanProperty present = new SimpleBooleanProperty(true);

	public DailyAttendanceRow(int empId, String name, boolean present) {
		this.employeeId.set(empId);
		this.fullName.set(name);
		this.present.set(present);
	}

	public int getEmployeeId() {
		return employeeId.get();
	}

	public String getFullName() {
		return fullName.get();
	}

	public boolean isPresent() {
		return present.get();
	}

	public IntegerProperty employeeIdProperty() {
		return employeeId;
	}

	public StringProperty fullNameProperty() {
		return fullName;
	}

	public BooleanProperty presentProperty() {
		return present;
	}
}
