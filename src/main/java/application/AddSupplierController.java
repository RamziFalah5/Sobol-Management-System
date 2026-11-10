package application;

import com.sobol.supplier.application.SupplierService;
import com.sobol.supplier.application.SupplierException;
import java.util.Objects;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class AddSupplierController {

	private final SupplierService service;
	public AddSupplierController() { this(SupplierModule.service()); }
	AddSupplierController(SupplierService service) { this.service = Objects.requireNonNull(service); }

	@FXML
	private TextField txtName, txtPhone, txtEmail, txtCity, txtAddress, txtContact;
	@FXML
	private TextArea txtNotes;

	@FXML
	private void onAdd() {
		try {
			service.createSupplier(txtName.getText(), txtPhone.getText(), txtEmail.getText(),
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
