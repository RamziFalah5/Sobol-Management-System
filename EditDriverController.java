package application;

import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;

public class EditDriverController {

	@FXML
	private TextField txtFullName;
	@FXML
	private TextField txtPhone;
	@FXML
	private ComboBox<Branch> cbBranch;
	@FXML
	private DatePicker dpHireDate;
	@FXML
	private TextField txtRate;

	@FXML
	private ComboBox<Vehicle> cbVehicle;

	@FXML
	private Label lblImageInfo;
	@FXML
	private ImageView imgPreview;
	@FXML
	private Button btnAnchor;

	private int employeeId;
	private String imagePath;
	private int currentVehicleId;

	public void setDriver(TruckDriverRow r) {
		this.employeeId = r.getEmployeeId();
		this.imagePath = r.getImagePath();
		this.currentVehicleId = r.getVehicleId();

		txtFullName.setText(r.getFullName());
		txtPhone.setText(r.getPhone());
		dpHireDate.setValue(LocalDate.parse(r.getHireDate()));
		txtRate.setText(String.valueOf(r.getDailyRate()));

		loadBranches(r.getBranchId());
		loadVehiclesForEdit(currentVehicleId);

		if (imagePath != null && !imagePath.trim().isEmpty()) {
			File f = new File(imagePath);
			if (f.exists()) {
				imgPreview.setImage(new Image(f.toURI().toString()));
				lblImageInfo.setText(f.getName());
			}
		}
	}

	private void loadBranches(int selectedBranchId) {
		cbBranch.setItems(FXCollections.observableArrayList(BranchDAO.getAllBranches()));

		cbBranch.setCellFactory(list -> new ListCell<>() {
			@Override
			protected void updateItem(Branch b, boolean empty) {
				super.updateItem(b, empty);
				setText(empty || b == null ? "" : b.getName());
			}
		});
		cbBranch.setButtonCell(new ListCell<>() {
			@Override
			protected void updateItem(Branch b, boolean empty) {
				super.updateItem(b, empty);
				setText(empty || b == null ? "" : b.getName());
			}
		});

		for (Branch b : cbBranch.getItems()) {
			if (b.getBranchId() == selectedBranchId) {
				cbBranch.getSelectionModel().select(b);
				break;
			}
		}
	}

	private void loadVehiclesForEdit(int currentVehicleId) {
		cbVehicle.setItems(FXCollections.observableArrayList(VehicleDAO.getVehiclesForDriverEdit(currentVehicleId)));

		cbVehicle.setCellFactory(list -> new ListCell<>() {
			@Override
			protected void updateItem(Vehicle v, boolean empty) {
				super.updateItem(v, empty);
				setText(empty || v == null ? "" : v.getName());
			}
		});
		cbVehicle.setButtonCell(new ListCell<>() {
			@Override
			protected void updateItem(Vehicle v, boolean empty) {
				super.updateItem(v, empty);
				setText(empty || v == null ? "" : v.getName());
			}
		});

		for (Vehicle v : cbVehicle.getItems()) {
			if (v.getVehicleId() == currentVehicleId) {
				cbVehicle.getSelectionModel().select(v);
				break;
			}
		}
	}

	@FXML
	private void chooseImage() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Choose Driver Image");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

		File f = fc.showOpenDialog(txtFullName.getScene().getWindow());
		if (f != null) {
			imagePath = f.getAbsolutePath();
			lblImageInfo.setText(f.getName());
			imgPreview.setImage(new Image(f.toURI().toString()));
		}
	}

	@FXML
	private void onSave() {

		String fullName = txtFullName.getText().trim();
		String phone = txtPhone.getText().trim();
		Branch branch = cbBranch.getValue();
		LocalDate hireDate = dpHireDate.getValue();

		if (fullName.isEmpty() || phone.isEmpty()) {
			alert("Validation", "Full name and phone are required.");
			return;
		}
		if (branch == null) {
			alert("Validation", "Please select a branch.");
			return;
		}
		if (hireDate == null) {
			alert("Validation", "Please select hire date.");
			return;
		}

		if (EmployeeDAO.nameExistsForEdit(fullName, employeeId)) {
			alert("Duplicate", "Employee name already exists.");
			return;
		}

		Double rate;
		try {
			rate = Double.parseDouble(txtRate.getText().trim());
			if (rate <= 0)
				throw new Exception();
		} catch (Exception e) {
			alert("Validation", "Driver daily rate must be a positive number.");
			return;
		}

		Vehicle vehicle = cbVehicle.getValue();
		if (vehicle == null) {
			alert("Validation", "Please select a vehicle.");
			return;
		}

		boolean ok = EmployeeDAO.updateTruckDriver(employeeId, fullName, phone, branch.getBranchId(), hireDate,
				imagePath, rate, vehicle.getVehicleId());

		if (!ok) {
			alert("Error", "Failed to update driver. Vehicle may be assigned to another driver.");
			return;
		}

		goBack();
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Drivers.fxml"));
			StackPane content = (StackPane) btnAnchor.getScene().getRoot().lookup("#contentArea");
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
