package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class AddWarehouseController {

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtCapacity; // عدد المشاتيح

    private int branchId;

    @FXML
    public void initialize() {
        branchId = BranchSession.getSelectedBranchId();
    }

    @FXML
    private void onAdd() {

        String name = txtName.getText().trim();
        String capText = txtCapacity.getText().trim();

        if (name.isEmpty() || capText.isEmpty()) {
            alert("Validation", "Warehouse name and pallet capacity are required.");
            return;
        }

        int capacity;
        try {
            capacity = Integer.parseInt(capText);
            if (capacity <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            alert("Validation", "Pallet capacity must be a positive number.");
            return;
        }

        // 🔒 duplicate name protection (same branch)
        if (WarehouseDAO.warehouseNameExists(branchId, name)) {
            alert("Duplicate",
                  "A warehouse with this name already exists in this branch.");
            return;
        }

        if (WarehouseDAO.insertWarehouse(branchId, name, capacity)) {
            goBack();
        } else {
            alert("Error", "Failed to add warehouse.");
        }
    }

    @FXML
    private void onCancel() {
        goBack();
    }

    private void goBack() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("Warehouses.fxml"));
            StackPane content =
                    (StackPane) txtName.getScene()
                            .getRoot().lookup("#contentArea");
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
