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
import java.sql.Date;
import java.time.LocalDate;

public class EditHourlyWorkerController {

	@FXML
	private Label lblId;
	@FXML
	private TextField txtName, txtPhone, txtRate;
	@FXML
	private ComboBox<Branch> cbBranch;
	@FXML
	private DatePicker dpHire;

	@FXML
	private ImageView imgPreview;
	@FXML
	private Label lblImg;

	@FXML
	private Button btnSave;

	private int employeeId;
	private String imagePath;

	@FXML
	public void initialize() {

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
	}

	/* ===== receive selected worker ===== */
	public void setWorker(GeneralWorkerRow w) {

		employeeId = w.getEmployeeId();
		lblId.setText("ID: " + employeeId);

		txtName.setText(w.getFullName());
		txtPhone.setText(w.getPhone());
		txtRate.setText(String.valueOf(w.getRate()));

		dpHire.setValue(LocalDate.parse(w.getHireDate()));

		for (Branch b : cbBranch.getItems()) {
			if (b.getBranchId() == w.getBranchId()) {
				cbBranch.getSelectionModel().select(b);
				break;
			}
		}

		imagePath = w.getImagePath();
		if (imagePath != null && !imagePath.isBlank()) {
			File f = new File(imagePath);
			if (f.exists()) {
				imgPreview.setImage(new Image(f.toURI().toString()));
				lblImg.setText(f.getName());
			}
		}
	}

	/* ===== image ===== */
	@FXML
	private void chooseImage() {

		FileChooser fc = new FileChooser();
		fc.setTitle("Choose Worker Image");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

		File f = fc.showOpenDialog(txtName.getScene().getWindow());

		if (f != null) {
			imagePath = f.getAbsolutePath();
			lblImg.setText(f.getName());
			imgPreview.setImage(new Image(f.toURI().toString()));
		}
	}

	/* ===== save ===== */
	@FXML
	private void onSave(javafx.event.ActionEvent e) {

		String name = txtName.getText().trim();
		String phone = txtPhone.getText().trim();
		String rateText = txtRate.getText().trim();

		if (name.isEmpty() || phone.isEmpty() || rateText.isEmpty()) {
			alert("Validation", "Please fill all fields.");
			return;
		}

		Branch br = cbBranch.getValue();
		if (br == null) {
			alert("Validation", "Please select a branch.");
			return;
		}

		if (dpHire.getValue() == null) {
			alert("Validation", "Please select hire date.");
			return;
		}

		double rate;
		try {
			rate = Double.parseDouble(rateText);
			if (rate <= 0)
				throw new Exception();
		} catch (Exception ex) {
			alert("Validation", "Hourly rate must be a positive number.");
			return;
		}

		if (GeneralWorkerDAO.nameExistsForEdit(name, employeeId)) {
			alert("Duplicate", "Name already exists.");
			return;
		}

		boolean ok = GeneralWorkerDAO.updateHourlyWorker(employeeId, name, phone, br.getBranchId(),
				Date.valueOf(dpHire.getValue()), imagePath, rate);

		if (!ok) {
			alert("Error", "Failed to update worker.");
			return;
		}

		goBack();
	}

	@FXML
	private void onCancel(javafx.event.ActionEvent e) {
		goBack();
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("HourlyWorkers.fxml"));
			StackPane content = (StackPane) btnSave.getScene().getRoot().lookup("#contentArea");

			content.getChildren().setAll(view);

		} catch (Exception ex) {
			ex.printStackTrace();
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
