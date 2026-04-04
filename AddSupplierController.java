package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class AddSupplierController {

	@FXML
	private TextField txtName, txtPhone, txtEmail, txtCity, txtAddress, txtContact;
	@FXML
	private TextArea txtNotes;

	@FXML
	private void onAdd() {

		String name = txtName.getText().trim();
		String phone = txtPhone.getText().trim();
		String email = txtEmail.getText().trim();

		if (name.isEmpty()) {
			alert("Validation", "Name is required.");
			return;
		}
		if (phone.isEmpty()) {
			alert("Validation", "Phone is required.");
			return;
		}

		if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
			alert("Validation", "Email format is not valid.");
			return;
		}

		if (SupplierDAO.nameExists(name)) {
			alert("Duplicate", "Supplier name already exists.");
			return;
		}

		SupplierDAO.SupplierFormData d = new SupplierDAO.SupplierFormData();
		d.name = name;
		d.phone = phone;
		d.email = email;
		d.city = txtCity.getText().trim();
		d.address = txtAddress.getText().trim();
		d.contactPerson = txtContact.getText().trim();
		d.notes = txtNotes.getText().trim();

		if (SupplierDAO.insertSupplier(d)) {
			goBack();
		} else {
			alert("Error", "Failed to add supplier.");
		}
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Suppliers.fxml"));
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
