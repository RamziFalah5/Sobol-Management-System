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

public class EditSalesRepController {

	@FXML
	private TextField txtFullName;
	@FXML
	private TextField txtPhone;
	@FXML
	private ComboBox<Branch> cbBranch;
	@FXML
	private DatePicker dpHireDate;

	@FXML
	private TextField txtDailyRate;
	@FXML
	private TextField txtCommission;

	@FXML
	private Label lblImageInfo;
	@FXML
	private ImageView imgPreview;

	@FXML
	private Button btnAnchor;

	private int employeeId;
	private String imagePath;

	@FXML
	public void initialize() {
		loadBranches();
	}

	public void setRep(SalesRepRow r) {
		employeeId = r.getEmployeeId();

		txtFullName.setText(r.getFullName());
		txtPhone.setText(r.getPhone());
		dpHireDate.setValue(r.getHireDate());

		txtDailyRate.setText(String.valueOf(r.getDailyRate()));
		txtCommission.setText(String.valueOf(r.getCommissionRate()));

		imagePath = r.getImagePath();
		if (imagePath != null && !imagePath.trim().isEmpty() && new File(imagePath).exists()) {
			lblImageInfo.setText(new File(imagePath).getName());
			imgPreview.setImage(new Image(new File(imagePath).toURI().toString()));
		} else {
			lblImageInfo.setText("No image selected");
			imgPreview.setImage(null);
		}

		// select current branch
		for (Branch b : cbBranch.getItems()) {
			if (b.getBranchId() == r.getBranchId()) {
				cbBranch.getSelectionModel().select(b);
				break;
			}
		}
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

		} catch (Exception e) {
			e.printStackTrace();
			alert("Error", "Failed to load branches.");
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
		Branch b = cbBranch.getValue();
		LocalDate hire = dpHireDate.getValue();

		if (fullName.isEmpty() || phone.isEmpty()) {
			alert("Validation", "Full name and phone are required.");
			return;
		}

		if (b == null) {
			alert("Validation", "Please select a branch.");
			return;
		}

		if (hire == null)
			hire = LocalDate.now();

		// duplicate name check for edit
		if (EmployeeDAO.nameExistsForEdit(fullName, employeeId)) {
			alert("Duplicate", "Employee name already exists.");
			return;
		}

		Double rate = parsePositiveDouble(txtDailyRate, "Daily rate");
		if (rate == null)
			return;

		Double comm = parsePositiveDouble(txtCommission, "Commission %");
		if (comm == null)
			return;

		boolean ok = SalesRepDAO.updateSalesRep(employeeId, fullName, phone, b.getBranchId(), hire, imagePath, rate,
				comm);

		if (ok) {
			alert("Edit", "Employee updated successfully.");
			goBack();
		} else {
			alert("Edit", "Failed to update employee (maybe duplicate name).");
		}
	}

	private Double parsePositiveDouble(TextField tf, String label) {
		String v = tf.getText().trim();
		if (v.isEmpty()) {
			alert("Validation", label + " is required.");
			return null;
		}
		try {
			double x = Double.parseDouble(v);
			if (x <= 0)
				throw new NumberFormatException();
			return x;
		} catch (Exception e) {
			alert("Validation", label + " must be a positive number.");
			return null;
		}
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("SalesReps.fxml"));
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
