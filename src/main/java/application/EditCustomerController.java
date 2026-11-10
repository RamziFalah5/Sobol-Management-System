package application;

import com.sobol.customer.application.CustomerService;
import com.sobol.customer.application.CustomerException;
import java.util.Objects;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class EditCustomerController {

	private final CustomerService service;
	public EditCustomerController() { this(CustomerModule.service()); }
	EditCustomerController(CustomerService service) { this.service = Objects.requireNonNull(service); }

	@FXML
	private Label lblId;
	@FXML
	private TextField txtName, txtPhone, txtEmail, txtCity, txtAddress;
	@FXML
	private TextArea txtNotes;

	private int customerId = -1;

	public void setCustomer(Customer c) {
		customerId = -1;
		if (c == null || c.getCustomerId() <= 0) throw new IllegalArgumentException("A valid selected customer is required.");
		customerId = c.getCustomerId();
		lblId.setText("ID: " + customerId);

		txtName.setText(c.getFullName());
		txtPhone.setText(c.getPhone());
		txtEmail.setText(c.getEmail() == null ? "" : c.getEmail());
		txtCity.setText(c.getCity() == null ? "" : c.getCity());
		txtAddress.setText(c.getAddress() == null ? "" : c.getAddress());
		txtNotes.setText(c.getNotes() == null ? "" : c.getNotes());
	}

	@FXML
	private void onSave() {
		try {
			service.updateCustomer(customerId, txtName.getText(), txtPhone.getText(), txtEmail.getText(),
					txtCity.getText(), txtAddress.getText(), txtNotes.getText());
			goBack();
		} catch (CustomerException failure) {
			String title = switch (failure.reason()) {
				case DUPLICATE_NAME, DUPLICATE_EMAIL -> "Duplicate";
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
			Parent view = createViewLoader("Customers.fxml").load();
			StackPane content = (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	FXMLLoader createViewLoader(String fxml) { return new FXMLLoader(getClass().getResource(fxml)); }

	void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
