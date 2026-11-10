package application;

import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;

public class AddEmployeeController {

	@FXML
	private RadioButton rbWorkers;
	@FXML
	private RadioButton rbDrivers;
	@FXML
	private RadioButton rbSales;

	@FXML
	private TextField txtFullName;
	@FXML
	private TextField txtPhone;
	@FXML
	private ComboBox<Branch> cbBranch;
	@FXML
	private DatePicker dpHireDate;
	@FXML
	private Label lblImageInfo;
	@FXML
	private StackPane typePane;

	@FXML
	private Button btnSave;

	@FXML
	private ImageView imgPreview;
	private String imagePath;

	private TextField txtDailyRate = new TextField();
	private TextField txtHourlyRate = new TextField();
	private RadioButton rbDaily = new RadioButton("Daily");
	private RadioButton rbHourly = new RadioButton("Hourly");
	private ToggleGroup workerRateGroup = new ToggleGroup();

	/* ================= DRIVERS ================= */
	private TextField txtDriverRate = new TextField();

	/* ✅ vehicle combo for drivers */
	private ComboBox<Vehicle> cbVehicle = new ComboBox<>();

	/* ================= SALES ================= */
	private TextField txtSalesRate = new TextField();
	private TextField txtCommission = new TextField();

	@FXML
	public void initialize() {

		ToggleGroup typeGroup = new ToggleGroup();
		rbWorkers.setToggleGroup(typeGroup);
		rbDrivers.setToggleGroup(typeGroup);
		rbSales.setToggleGroup(typeGroup);

		rbDaily.setToggleGroup(workerRateGroup);
		rbHourly.setToggleGroup(workerRateGroup);
		rbDaily.setSelected(true);

		dpHireDate.setValue(LocalDate.now());

		loadBranches();

		rbWorkers.setSelected(true);
		buildWorkersPane();
	}

	private void loadBranches() {
		try {
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

			if (!cbBranch.getItems().isEmpty())
				cbBranch.getSelectionModel().selectFirst();

		} catch (Exception e) {
			e.printStackTrace();
			alert("Error", "Failed to load branches.");
		}
	}

	@FXML
	private void onTypeChanged() {
		if (rbWorkers.isSelected()) {
			btnSave.setDisable(false);
			buildWorkersPane();
		} else if (rbDrivers.isSelected()) {
			buildDriversPane(); // will enable/disable save depending on vehicles
		} else if (rbSales.isSelected()) {
			btnSave.setDisable(false);
			buildSalesPane();
		}
	}

	@FXML
	private void chooseImage() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Choose Employee Image");
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
		LocalDate hireDate = dpHireDate.getValue();

		if (fullName.isEmpty() || phone.isEmpty()) {
			alert("Validation", "Full name and phone are required.");
			return;
		}

		Branch selectedBranch = cbBranch.getValue();
		if (selectedBranch == null) {
			alert("Validation", "Please select a branch.");
			return;
		}
		int branchId = selectedBranch.getBranchId();

		if (hireDate == null)
			hireDate = LocalDate.now();

		if (EmployeeDAO.nameExists(fullName)) {
			alert("Duplicate", "Employee name already exists.");
			return;
		}

		boolean ok = false;

		if (rbWorkers.isSelected())
			ok = saveWorker(fullName, phone, branchId, hireDate);
		else if (rbDrivers.isSelected())
			ok = saveDriver(fullName, phone, branchId, hireDate);
		else if (rbSales.isSelected())
			ok = saveSales(fullName, phone, branchId, hireDate);

		if (ok)
			goBack();
		else
			alert("Error", "Failed to save employee.");
	}

	private boolean saveWorker(String name, String phone, int branchId, LocalDate date) {
		if (rbDaily.isSelected()) {
			Double rate = parseDouble(txtDailyRate, "Daily rate");
			if (rate == null)
				return false;
			return EmployeeDAO.insertGeneralWorker(name, phone, branchId, date, imagePath, "DAILY", rate, null);
		} else {
			Double rate = parseDouble(txtHourlyRate, "Hourly rate");
			if (rate == null)
				return false;
			return EmployeeDAO.insertGeneralWorker(name, phone, branchId, date, imagePath, "HOURLY", null, rate);
		}
	}

	private boolean saveDriver(String name, String phone, int branchId, LocalDate date) {

		// must have vehicles
		if (cbVehicle.getItems() == null || cbVehicle.getItems().isEmpty()) {
			alert("No Vehicles", "No vehicles available. Please add a vehicle first.");
			return false;
		}

		Vehicle selected = cbVehicle.getValue();
		if (selected == null) {
			alert("Validation", "Please select a vehicle for this driver.");
			return false;
		}

		Double rate = parseDouble(txtDriverRate, "Driver daily rate");
		if (rate == null)
			return false;

		return EmployeeDAO.insertTruckDriver(name, phone, branchId, date, imagePath, rate, selected.getVehicleId());
	}

	private boolean saveSales(String name, String phone, int branchId, LocalDate date) {
		Double rate = parseDouble(txtSalesRate, "Sales daily rate");
		Double comm = parseDouble(txtCommission, "Commission");
		if (rate == null || comm == null)
			return false;
		return EmployeeDAO.insertSalesRep(name, phone, branchId, date, imagePath, rate, comm);
	}

	private Double parseDouble(TextField tf, String label) {
		try {
			String s = tf.getText().trim();
			if (s.isEmpty())
				throw new Exception();
			double v = Double.parseDouble(s);
			if (v <= 0)
				throw new Exception();
			return v;
		} catch (Exception e) {
			alert("Validation", label + " must be a positive number.");
			return null;
		}
	}

	private Integer parseInt(TextField tf, String label) {
		try {
			String s = tf.getText().trim();
			if (s.isEmpty())
				throw new Exception();
			int v = Integer.parseInt(s);
			if (v <= 0)
				throw new Exception();
			return v;
		} catch (Exception e) {
			alert("Validation", label + " must be a positive number.");
			return null;
		}
	}

	@FXML
	private void onCancel(javafx.event.ActionEvent event) {
		goBack();
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Employee.fxml"));
			StackPane content = (StackPane) txtFullName.getScene().getRoot().lookup("#contentArea");
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

	/* ================= UI BUILDERS ================= */

	private void buildWorkersPane() {
		VBox box = new VBox(10);
		Label title = new Label("General Worker");

		txtDailyRate.setPromptText("Daily rate");
		txtHourlyRate.setPromptText("Hourly rate");

		VBox fields = new VBox(10, txtDailyRate);

		workerRateGroup.selectedToggleProperty().addListener((obs, o, n) -> {
			fields.getChildren().clear();
			fields.getChildren().add(rbHourly.isSelected() ? txtHourlyRate : txtDailyRate);
		});

		box.getChildren().addAll(title, new HBox(10, rbDaily, rbHourly), fields);
		typePane.getChildren().setAll(box);
	}

	private void buildDriversPane() {

		// load available vehicles (not assigned to another driver)
		cbVehicle.setItems(FXCollections.observableArrayList(VehicleDAO.getAvailableVehiclesForDrivers()));

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

		txtDriverRate.setPromptText("Daily rate");

		VBox box = new VBox(12);
		box.getChildren().addAll(new Label("Truck Driver"), txtDriverRate, new Label("Vehicle"), cbVehicle);

		typePane.getChildren().setAll(box);

		// If no vehicles -> prevent add
		if (cbVehicle.getItems().isEmpty()) {
			btnSave.setDisable(true);
			alert("No Vehicles", "No vehicles available. Please add a vehicle first.");
		} else {
			btnSave.setDisable(false);
			cbVehicle.getSelectionModel().selectFirst();
		}
	}

	private void buildSalesPane() {
		txtSalesRate.setPromptText("Daily rate");
		txtCommission.setPromptText("Commission %");
		typePane.getChildren().setAll(new VBox(10, new Label("Sales Representative"), txtSalesRate, txtCommission));
	}
}
