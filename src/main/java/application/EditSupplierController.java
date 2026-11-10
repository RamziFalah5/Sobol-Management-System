package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class EditSupplierController {

	@FXML
	private Label lblId;
	@FXML
	private TextField txtName, txtPhone, txtEmail, txtCity, txtAddress, txtContact;
	@FXML
	private TextArea txtNotes;

	private int supplierId;

	public void setSupplier(Supplier s) {

		supplierId = s.getSupplierId();
		lblId.setText("ID: " + supplierId);

		txtName.setText(s.getName());
		txtPhone.setText(s.getPhone());
		txtEmail.setText(s.getEmail());
		txtCity.setText(s.getCity());
		txtAddress.setText(s.getAddress());
		txtContact.setText(s.getContactPerson());
		txtNotes.setText(s.getNotes());
	}

	@FXML
	private void onSave() {

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

		if (SupplierDAO.nameExistsForEdit(name, supplierId)) {
			alert("Duplicate", "Another supplier already uses this name.");
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

		if (SupplierDAO.updateSupplier(supplierId, d)) {
			goBack();
		} else {
			alert("Error", "Failed to update supplier.");
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
