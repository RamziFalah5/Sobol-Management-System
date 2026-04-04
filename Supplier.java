package application;

import javafx.beans.property.*;

public class Supplier {

	private final IntegerProperty supplierId = new SimpleIntegerProperty();
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty phone = new SimpleStringProperty();
	private final StringProperty email = new SimpleStringProperty();
	private final StringProperty city = new SimpleStringProperty();
	private final StringProperty address = new SimpleStringProperty();
	private final StringProperty contactPerson = new SimpleStringProperty();
	private final StringProperty notes = new SimpleStringProperty();

	public Supplier() {
	}

	public Supplier(int supplierId, String name, String phone, String email, String city, String address,
			String contactPerson, String notes) {
		this.supplierId.set(supplierId);
		this.name.set(name);
		this.phone.set(phone);
		this.email.set(email);
		this.city.set(city);
		this.address.set(address);
		this.contactPerson.set(contactPerson);
		this.notes.set(notes);
	}

	public int getSupplierId() {
		return supplierId.get();
	}

	public String getName() {
		return name.get();
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

	public String getContactPerson() {
		return contactPerson.get();
	}

	public String getNotes() {
		return notes.get();
	}

	public void setSupplierId(int v) {
		supplierId.set(v);
	}

	public void setName(String v) {
		name.set(v);
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

	public void setContactPerson(String v) {
		contactPerson.set(v);
	}

	public void setNotes(String v) {
		notes.set(v);
	}

	public IntegerProperty supplierIdProperty() {
		return supplierId;
	}

	public StringProperty nameProperty() {
		return name;
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

	public StringProperty contactPersonProperty() {
		return contactPerson;
	}

	public StringProperty notesProperty() {
		return notes;
	}
	@Override
	public String toString() {
		return getName();
	}
}
