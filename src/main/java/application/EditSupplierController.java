package application;

import com.sobol.supplier.application.SupplierService;
import com.sobol.supplier.application.SupplierException;
import java.util.Objects;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class EditSupplierController {

	private final SupplierService service;
	public EditSupplierController() { this(SupplierModule.service()); }
	EditSupplierController(SupplierService service) { this.service = Objects.requireNonNull(service); }

	@FXML
	private Label lblId;
	@FXML
	private TextField txtName, txtPhone, txtEmail, txtCity, txtAddress, txtContact;
	@FXML
	private TextArea txtNotes;

	private int supplierId = -1;

	public void setSupplier(Supplier s) {

		supplierId = -1;
		if (s == null || s.getSupplierId() <= 0) throw new IllegalArgumentException("A valid selected supplier is required.");
		supplierId = s.getSupplierId();
		lblId.setText("ID: " + supplierId);

		txtName.setText(s.getName());
		txtPhone.setText(s.getPhone());
		txtEmail.setText(s.getEmail() == null ? "" : s.getEmail());
		txtCity.setText(s.getCity() == null ? "" : s.getCity());
		txtAddress.setText(s.getAddress() == null ? "" : s.getAddress());
		txtContact.setText(s.getContactPerson() == null ? "" : s.getContactPerson());
		txtNotes.setText(s.getNotes() == null ? "" : s.getNotes());
	}

	@FXML
	private void onSave() {
		try {
			service.updateSupplier(supplierId, txtName.getText(), txtPhone.getText(), txtEmail.getText(),
					txtCity.getText(), txtAddress.getText(), txtContact.getText(), txtNotes.getText());
			goBack();
		} catch (SupplierException failure) {
			String title = switch (failure.reason()) {
				case DUPLICATE_NAME -> "Duplicate";
				case NOT_FOUND -> "Not Found";
				case STORAGE_FAILURE -> "Error";
				default -> "Validation";
			};
			alert(title, failure.getMessage());
		}
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Suppliers.fxml"));
			StackPane content = (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
