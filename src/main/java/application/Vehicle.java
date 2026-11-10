package application;

import javafx.beans.property.*;

public class Vehicle {

	private final IntegerProperty vehicleId = new SimpleIntegerProperty();
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty plateNumber = new SimpleStringProperty();
	private final StringProperty vin = new SimpleStringProperty();

	private final StringProperty vehicleType = new SimpleStringProperty();
	private final StringProperty manufacturer = new SimpleStringProperty();
	private final StringProperty model = new SimpleStringProperty();
	private final IntegerProperty modelYear = new SimpleIntegerProperty();

	private final StringProperty status = new SimpleStringProperty();

	private final IntegerProperty capacityPallets = new SimpleIntegerProperty();
	private final IntegerProperty maxLoadKg = new SimpleIntegerProperty();

	private final StringProperty fuelType = new SimpleStringProperty();
	private final StringProperty transmission = new SimpleStringProperty();

	private final StringProperty purchaseDate = new SimpleStringProperty();
	private final StringProperty lastServiceDate = new SimpleStringProperty();
	private final StringProperty insuranceExpiry = new SimpleStringProperty();

	private final StringProperty imagePath = new SimpleStringProperty();
	private final StringProperty notes = new SimpleStringProperty();

	public Vehicle(int id, String name, String plate, String vin, String type, String manufacturer, String model,
			int year, String status, int pallets, int maxLoadKg, String fuel, String transmission, String purchaseDate,
			String lastServiceDate, String insuranceExpiry, String imagePath, String notes) {

		this.vehicleId.set(id);
		this.name.set(name);
		this.plateNumber.set(plate);
		this.vin.set(vin);

		this.vehicleType.set(type);
		this.manufacturer.set(manufacturer);
		this.model.set(model);
		this.modelYear.set(year);

		this.status.set(status);

		this.capacityPallets.set(pallets);
		this.maxLoadKg.set(maxLoadKg);

		this.fuelType.set(fuel);
		this.transmission.set(transmission);

		this.purchaseDate.set(purchaseDate);
		this.lastServiceDate.set(lastServiceDate);
		this.insuranceExpiry.set(insuranceExpiry);

		this.imagePath.set(imagePath);
		this.notes.set(notes);
	}

	public int getVehicleId() {
		return vehicleId.get();
	}

	public String getName() {
		return name.get();
	}

	public String getPlateNumber() {
		return plateNumber.get();
	}

	public String getVin() {
		return vin.get();
	}

	public String getVehicleType() {
		return vehicleType.get();
	}

	public String getManufacturer() {
		return manufacturer.get();
	}

	public String getModel() {
		return model.get();
	}

	public int getModelYear() {
		return modelYear.get();
	}

	public String getStatus() {
		return status.get();
	}

	public int getCapacityPallets() {
		return capacityPallets.get();
	}

	public int getMaxLoadKg() {
		return maxLoadKg.get();
	}

	public String getFuelType() {
		return fuelType.get();
	}

	public String getTransmission() {
		return transmission.get();
	}

	public String getPurchaseDate() {
		return purchaseDate.get();
	}

	public String getLastServiceDate() {
		return lastServiceDate.get();
	}

	public String getInsuranceExpiry() {
		return insuranceExpiry.get();
	}

	public String getImagePath() {
		return imagePath.get();
	}

	public String getNotes() {
		return notes.get();
	}

	public IntegerProperty vehicleIdProperty() {
		return vehicleId;
	}

	public StringProperty nameProperty() {
		return name;
	}

	public StringProperty plateNumberProperty() {
		return plateNumber;
	}

	public StringProperty vinProperty() {
		return vin;
	}

	public StringProperty vehicleTypeProperty() {
		return vehicleType;
	}

	public StringProperty manufacturerProperty() {
		return manufacturer;
	}

	public StringProperty modelProperty() {
		return model;
	}

	public IntegerProperty modelYearProperty() {
		return modelYear;
	}

	public StringProperty statusProperty() {
		return status;
	}

	public IntegerProperty capacityPalletsProperty() {
		return capacityPallets;
	}

	public IntegerProperty maxLoadKgProperty() {
		return maxLoadKg;
	}

	public StringProperty fuelTypeProperty() {
		return fuelType;
	}

	public StringProperty transmissionProperty() {
		return transmission;
	}

	public StringProperty purchaseDateProperty() {
		return purchaseDate;
	}

	public StringProperty lastServiceDateProperty() {
		return lastServiceDate;
	}

	public StringProperty insuranceExpiryProperty() {
		return insuranceExpiry;
	}

	public StringProperty imagePathProperty() {
		return imagePath;
	}

	public StringProperty notesProperty() {
		return notes;
	}
}
