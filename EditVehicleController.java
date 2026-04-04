package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;

public class EditVehicleController {

	@FXML
	private Label lblId;

	@FXML
	private TextField txtName, txtPlate, txtVin, txtManufacturer, txtModel;
	@FXML
	private ComboBox<String> cbType, cbStatus, cbFuel, cbTrans;
	@FXML
	private Spinner<Integer> spYear, spPallets, spMaxLoad;
	@FXML
	private DatePicker dpPurchase, dpLastService, dpInsurance;
	@FXML
	private TextArea txtNotes;

	@FXML
	private Label lblImg;
	@FXML
	private ImageView imgPreview;

	private int vehicleId;
	private String imagePath;

	@FXML
	public void initialize() {
		cbType.getItems().setAll("TRUCK", "VAN");
		cbStatus.getItems().setAll("ACTIVE", "IDLE", "MAINTENANCE");
		cbFuel.getItems().setAll("DIESEL", "GASOLINE", "ELECTRIC", "HYBRID");
		cbTrans.getItems().setAll("MANUAL", "AUTOMATIC");

		spYear.setValueFactory(
				new SpinnerValueFactory.IntegerSpinnerValueFactory(1990, 2050, LocalDate.now().getYear()));
		spPallets.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 20));
		spMaxLoad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 200000, 20000));
	}

	public void setVehicle(Vehicle v) {
		this.vehicleId = v.getVehicleId();
		lblId.setText("ID: " + vehicleId);

		txtName.setText(v.getName());
		txtPlate.setText(v.getPlateNumber());
		txtVin.setText(v.getVin());

		cbType.setValue(v.getVehicleType());
		cbStatus.setValue(v.getStatus());
		cbFuel.setValue(v.getFuelType());
		cbTrans.setValue(v.getTransmission());

		txtManufacturer.setText(v.getManufacturer());
		txtModel.setText(v.getModel());
		spYear.getValueFactory().setValue(v.getModelYear());

		spPallets.getValueFactory().setValue(v.getCapacityPallets());
		spMaxLoad.getValueFactory().setValue(v.getMaxLoadKg());

		dpPurchase.setValue(LocalDate.parse(v.getPurchaseDate()));
		dpLastService.setValue(LocalDate.parse(v.getLastServiceDate()));
		dpInsurance.setValue(LocalDate.parse(v.getInsuranceExpiry()));

		txtNotes.setText(v.getNotes());

		imagePath = v.getImagePath();
		if (imagePath != null && !imagePath.isBlank()) {
			File f = new File(imagePath);
			if (f.exists())
				imgPreview.setImage(new Image(f.toURI().toString()));
		}
		if (lblImg != null)
			lblImg.setText(imagePath == null ? "(optional)" : new File(imagePath).getName());
	}

	@FXML
	private void chooseImage() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Choose Vehicle Image");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
		File f = fc.showOpenDialog(txtName.getScene().getWindow());
		if (f != null) {
			imagePath = f.getAbsolutePath();
			lblImg.setText(f.getName());
			imgPreview.setImage(new Image(f.toURI().toString()));
		}
	}

	@FXML
	private void onSave() {
		VehicleDAO.VehicleFormData d = readAndValidate();
		if (d == null)
			return;

		if (VehicleDAO.nameExistsForEdit(d.name, vehicleId)) {
			alert("Duplicate", "Vehicle name already exists.");
			return;
		}
		if (VehicleDAO.plateExistsForEdit(d.plateNumber, vehicleId)) {
			alert("Duplicate", "Plate number already exists.");
			return;
		}
		if (VehicleDAO.vinExistsForEdit(d.vin, vehicleId)) {
			alert("Duplicate", "VIN already exists.");
			return;
		}

		if (VehicleDAO.updateVehicle(vehicleId, d))
			goBack();
		else
			alert("Error", "Failed to update vehicle.");
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	private VehicleDAO.VehicleFormData readAndValidate() {
		String name = txtName.getText().trim();
		String plate = txtPlate.getText().trim();
		String vin = txtVin.getText().trim();
		String manufacturer = txtManufacturer.getText().trim();
		String model = txtModel.getText().trim();

		if (name.isEmpty() || plate.isEmpty() || vin.isEmpty() || manufacturer.isEmpty() || model.isEmpty()) {
			alert("Validation", "Please fill all required fields.");
			return null;
		}
		if (dpPurchase.getValue() == null || dpLastService.getValue() == null || dpInsurance.getValue() == null) {
			alert("Validation", "Please choose all dates.");
			return null;
		}

		VehicleDAO.VehicleFormData d = new VehicleDAO.VehicleFormData();
		d.name = name;
		d.plateNumber = plate;
		d.vin = vin;

		d.vehicleType = cbType.getValue();
		d.status = cbStatus.getValue();
		d.fuelType = cbFuel.getValue();
		d.transmission = cbTrans.getValue();

		d.manufacturer = manufacturer;
		d.model = model;
		d.modelYear = spYear.getValue();

		d.capacityPallets = spPallets.getValue();
		d.maxLoadKg = spMaxLoad.getValue();

		d.purchaseDate = dpPurchase.getValue().toString();
		d.lastServiceDate = dpLastService.getValue().toString();
		d.insuranceExpiry = dpInsurance.getValue().toString();

		d.imagePath = imagePath;
		d.notes = (txtNotes.getText() == null) ? "" : txtNotes.getText().trim();
		return d;
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Vehicles.fxml"));
			StackPane content = (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
