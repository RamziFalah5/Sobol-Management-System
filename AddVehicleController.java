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

public class AddVehicleController {

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

	private String imagePath;

	@FXML
	public void initialize() {
		cbType.getItems().setAll("TRUCK", "VAN");
		cbStatus.getItems().setAll("ACTIVE", "IDLE", "MAINTENANCE");
		cbFuel.getItems().setAll("DIESEL", "GASOLINE", "ELECTRIC", "HYBRID");
		cbTrans.getItems().setAll("MANUAL", "AUTOMATIC");

		cbType.getSelectionModel().selectFirst();
		cbStatus.getSelectionModel().selectFirst();
		cbFuel.getSelectionModel().selectFirst();
		cbTrans.getSelectionModel().selectFirst();

		spYear.setValueFactory(
				new SpinnerValueFactory.IntegerSpinnerValueFactory(1990, 2050, LocalDate.now().getYear()));
		spPallets.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 20));
		spMaxLoad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 200000, 20000));

		dpPurchase.setValue(LocalDate.now());
		dpLastService.setValue(LocalDate.now());
		dpInsurance.setValue(LocalDate.now().plusMonths(6));
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
	private void onAdd() {
		VehicleDAO.VehicleFormData d = readAndValidate(false, 0);
		if (d == null)
			return;

		// duplicate checks
		if (VehicleDAO.nameExists(d.name)) {
			alert("Duplicate", "Vehicle name already exists.");
			return;
		}
		if (VehicleDAO.plateExists(d.plateNumber)) {
			alert("Duplicate", "Plate number already exists.");
			return;
		}
		if (VehicleDAO.vinExists(d.vin)) {
			alert("Duplicate", "VIN already exists.");
			return;
		}

		if (VehicleDAO.insertVehicle(d))
			goBack();
		else
			alert("Error", "Failed to add vehicle.");
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	private VehicleDAO.VehicleFormData readAndValidate(boolean isEdit, int id) {

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
		d.imagePath = imagePath; // optional
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
