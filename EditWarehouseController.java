package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class EditWarehouseController {

	@FXML
	private Label lblId;

	@FXML
	private TextField txtName;

	@FXML
	private TextField txtCapacity; // عدد المشاتيح

	private int warehouseId;
	private int branchId;
	private int oldCapacity;

	// يناديها WarehousesController
	public void setWarehouse(Warehouse w) {
		this.warehouseId = w.getWarehouseId();
		this.branchId = w.getBranchId();
		this.oldCapacity = w.getPalletCapacity();

		lblId.setText("Warehouse ID: " + warehouseId);
		txtName.setText(w.getName());
		txtCapacity.setText(String.valueOf(w.getPalletCapacity()));
	}

	@FXML
	private void onSave() {

		String name = txtName.getText().trim();
		String capText = txtCapacity.getText().trim();

		if (name.isEmpty() || capText.isEmpty()) {
			alert("Validation", "Warehouse name and pallet capacity are required.");
			return;
		}

		int newCapacity;
		try {
			newCapacity = Integer.parseInt(capText);
			if (newCapacity <= 0)
				throw new NumberFormatException();
		} catch (Exception e) {
			alert("Validation", "Pallet capacity must be a positive number.");
			return;
		}

		// 🔒 duplicate name protection (edit)
		if (WarehouseDAO.warehouseNameExistsForEdit(branchId, name, warehouseId)) {
			alert("Duplicate", "Another warehouse already uses this name in this branch.");
			return;
		}

		int diff = newCapacity - oldCapacity;

		if (WarehouseDAO.updateWarehouse(warehouseId, branchId, name, newCapacity, diff)) {

			goBack();

		} else {
			alert("Error", "Failed to update warehouse.");
		}
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Warehouses.fxml"));
			StackPane content = (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void alert(String title, String msg) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(title);
		a.setHeaderText(null);
		a.setContentText(msg);
		a.showAndWait();
	}
}
