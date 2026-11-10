package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class EditCustomerController {

	@FXML
	private Label lblId;
	@FXML
	private TextField txtName, txtPhone, txtEmail, txtCity, txtAddress;
	@FXML
	private TextArea txtNotes;

	private int customerId;

	public void setCustomer(Customer c) {
		customerId = c.getCustomerId();
		lblId.setText("ID: " + customerId);

		txtName.setText(c.getFullName());
		txtPhone.setText(c.getPhone());
		txtEmail.setText(c.getEmail());
		txtCity.setText(c.getCity());
		txtAddress.setText(c.getAddress());
		txtNotes.setText(c.getNotes());
	}

	@FXML
	private void onSave() {

		String name = txtName.getText().trim();
		String phone = txtPhone.getText().trim();
		String email = txtEmail.getText().trim();
		String city = txtCity.getText().trim();
		String address = txtAddress.getText().trim();
		String notes = txtNotes.getText().trim();

		if (name.isEmpty()) {
			alert("Validation", "Full name is required.");
			return;
		}
		if (phone.isEmpty()) {
			alert("Validation", "Phone is required.");
			return;
		}

		if (!email.isEmpty() && !isValidEmail(email)) {
			alert("Validation", "Email format is not valid.");
			return;
		}

		if (CustomerDAO.nameExistsForEdit(name, customerId)) {
			alert("Duplicate", "Another customer already uses this name.");
			return;
		}

		if (!email.isEmpty() && CustomerDAO.emailExistsForEdit(email, customerId)) {
			alert("Duplicate", "Another customer already uses this email.");
			return;
		}

		CustomerDAO.CustomerFormData d = new CustomerDAO.CustomerFormData();
		d.fullName = name;
		d.phone = phone;
		d.email = email;
		d.city = city;
		d.address = address;
		d.notes = notes;

		if (CustomerDAO.updateCustomer(customerId, d)) {
			goBack();
		} else {
			alert("Error", "Failed to update customer.");
		}
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	private boolean isValidEmail(String e) {
		return e.contains("@") && e.contains(".") && e.indexOf("@") < e.lastIndexOf(".");
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Customers.fxml"));
			StackPane content = (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
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
