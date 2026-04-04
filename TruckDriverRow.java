package application;

import javafx.beans.property.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

public class TruckDriverRow {

	private final IntegerProperty employeeId = new SimpleIntegerProperty();
	private final ObjectProperty<ImageView> photo = new SimpleObjectProperty<>();

	private final StringProperty fullName = new SimpleStringProperty();
	private final StringProperty phone = new SimpleStringProperty();
	private final IntegerProperty branchId = new SimpleIntegerProperty();
	private final StringProperty branchName = new SimpleStringProperty();

	private final StringProperty hireDate = new SimpleStringProperty();

	private final DoubleProperty dailyRate = new SimpleDoubleProperty();

	private final IntegerProperty vehicleId = new SimpleIntegerProperty();
	private final StringProperty vehicleName = new SimpleStringProperty();

	private final StringProperty imagePath = new SimpleStringProperty();

	public TruckDriverRow(int employeeId, String fullName, String phone, int branchId, String branchName,
			String hireDate, double dailyRate, int vehicleId, String vehicleName, String imagePath) {

		this.employeeId.set(employeeId);
		this.fullName.set(fullName);
		this.phone.set(phone);
		this.branchId.set(branchId);
		this.branchName.set(branchName);
		this.hireDate.set(hireDate);
		this.dailyRate.set(dailyRate);
		this.vehicleId.set(vehicleId);
		this.vehicleName.set(vehicleName);
		this.imagePath.set(imagePath);

		this.photo.set(buildImageView(imagePath));
	}

	private ImageView buildImageView(String path) {
		ImageView iv = new ImageView();
		iv.setFitWidth(44);
		iv.setFitHeight(44);
		iv.setPreserveRatio(true);

		try {
			if (path != null && !path.trim().isEmpty()) {
				File f = new File(path);
				if (f.exists()) {
					iv.setImage(new Image(f.toURI().toString(), 44, 44, true, true));
				}
			}
		} catch (Exception ignored) {
		}

		return iv;
	}

	public int getEmployeeId() {
		return employeeId.get();
	}

	public String getFullName() {
		return fullName.get();
	}

	public String getPhone() {
		return phone.get();
	}

	public int getBranchId() {
		return branchId.get();
	}

	public String getBranchName() {
		return branchName.get();
	}

	public String getHireDate() {
		return hireDate.get();
	}

	public double getDailyRate() {
		return dailyRate.get();
	}

	public int getVehicleId() {
		return vehicleId.get();
	}

	public String getVehicleName() {
		return vehicleName.get();
	}

	public String getImagePath() {
		return imagePath.get();
	}

	public IntegerProperty employeeIdProperty() {
		return employeeId;
	}

	public ObjectProperty<ImageView> photoProperty() {
		return photo;
	}

	public StringProperty fullNameProperty() {
		return fullName;
	}

	public StringProperty phoneProperty() {
		return phone;
	}

	public IntegerProperty branchIdProperty() {
		return branchId;
	}

	public StringProperty branchNameProperty() {
		return branchName;
	}

	public StringProperty hireDateProperty() {
		return hireDate;
	}

	public DoubleProperty dailyRateProperty() {
		return dailyRate;
	}

	public IntegerProperty vehicleIdProperty() {
		return vehicleId;
	}

	public StringProperty vehicleNameProperty() {
		return vehicleName;
	}

	public StringProperty imagePathProperty() {
		return imagePath;
	}

	public void refreshPhoto() {
		this.photo.set(buildImageView(this.imagePath.get()));
	}
}
