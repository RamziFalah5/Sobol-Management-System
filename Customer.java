package application;

import javafx.beans.property.*;

public class Customer {

	private final IntegerProperty customerId = new SimpleIntegerProperty();
	private final StringProperty fullName = new SimpleStringProperty();
	private final StringProperty phone = new SimpleStringProperty();
	private final StringProperty email = new SimpleStringProperty();
	private final StringProperty city = new SimpleStringProperty();
	private final StringProperty address = new SimpleStringProperty();
	private final StringProperty notes = new SimpleStringProperty();

	public Customer() {
	}

	public Customer(int id, String fullName, String phone, String email, String city, String address, String notes) {
		this.customerId.set(id);
		this.fullName.set(fullName);
		this.phone.set(phone);
		this.email.set(email);
		this.city.set(city);
		this.address.set(address);
		this.notes.set(notes);
	}

	public int getCustomerId() {
		return customerId.get();
	}

	public String getFullName() {
		return fullName.get();
	}

	public String getPhone() {
		return phone.get();
	}

	public String getEmail() {
		return email.get();
	}

	public String getCity() {
		return city.get();
	}

	public String getAddress() {
		return address.get();
	}

	public String getNotes() {
		return notes.get();
	}

	public void setCustomerId(int v) {
		customerId.set(v);
	}

	public void setFullName(String v) {
		fullName.set(v);
	}

	public void setPhone(String v) {
		phone.set(v);
	}

	public void setEmail(String v) {
		email.set(v);
	}

	public void setCity(String v) {
		city.set(v);
	}

	public void setAddress(String v) {
		address.set(v);
	}

	public void setNotes(String v) {
		notes.set(v);
	}

	public IntegerProperty customerIdProperty() {
		return customerId;
	}

	public StringProperty fullNameProperty() {
		return fullName;
	}

	public StringProperty phoneProperty() {
		return phone;
	}

	public StringProperty emailProperty() {
		return email;
	}

	public StringProperty cityProperty() {
		return city;
	}

	public StringProperty addressProperty() {
		return address;
	}

	public StringProperty notesProperty() {
		return notes;
	}
}
