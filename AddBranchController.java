package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.sql.SQLIntegrityConstraintViolationException;

public class AddBranchController {

    @FXML private TextField txtName;
    @FXML private TextField txtCity;
    @FXML private TextField txtPhone;

    @FXML
    private void onAdd() {

        String name = txtName.getText().trim();
        String city = txtCity.getText().trim();
        String phone = txtPhone.getText().trim();

        if (name.isEmpty() || city.isEmpty()) {
            alert("Validation", "Branch name and city are required.");
            return;
        }

        // 🔒 First protection (logic)
        if (BranchDAO.branchNameExists(name)) {
            alert("Duplicate", "Branch name already exists.");
            return;
        }

        try {
            if (BranchDAO.insertBranch(name, city, phone)) {
                goBack();
            } else {
                alert("Error", "Failed to add branch.");
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
            StackPane content =
                    (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
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
