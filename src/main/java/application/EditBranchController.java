package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.sql.SQLIntegrityConstraintViolationException;

public class EditBranchController {

    @FXML
    private Label lblId;
    @FXML
    private TextField txtName;
    @FXML
    private TextField txtCity;
    @FXML
    private TextField txtPhone;

    private int branchId;

    public void setBranch(Branch b) {
        branchId = b.getBranchId();
        lblId.setText("Branch ID: " + branchId);
        txtName.setText(b.getName());
        txtCity.setText(b.getCity());
        txtPhone.setText(b.getPhone());
    }

    @FXML
    private void onSave() {

        String name = txtName.getText().trim();
        String city = txtCity.getText().trim();
        String phone = txtPhone.getText().trim();

        if (name.isEmpty() || city.isEmpty()) {
            alert("Validation", "Branch name and city are required.");
            return;
        }

        // 🔒 First protection (logic)
        if (BranchDAO.branchNameExistsForEdit(name, branchId)) {
            alert("Duplicate", "Another branch already uses this name.");
            return;
        }

        try {
            if (BranchDAO.updateBranch(branchId, name, city, phone)) {
                goBack();
            } else {
                alert("Error", "Failed to update branch.");
            }
        } catch (Exception e) {

            // 🔒 Second protection (DB UNIQUE)
            if (e.getCause() instanceof SQLIntegrityConstraintViolationException) {
                alert("Duplicate", "Branch name already exists.");
            } else {
                e.printStackTrace();
                alert("Error", "Unexpected error occurred.");
            }
        }
    }

    @FXML
    private void onCancel() {
        goBack();
    }

    private void goBack() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("Branches.fxml"));
            StackPane contentArea =
                    (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
            contentArea.getChildren().setAll(view);
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
