package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class AddCategoryController {

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;

    @FXML
    private void onAdd() {

        String name = txtName.getText().trim();
        String desc = txtDescription.getText().trim();

        if (name.isEmpty()) {
            showAlert("Validation", "Category name is required.");
            return;
        }

        // 🔒 duplicate check
        if (CategoryDAO.categoryNameExists(name)) {
            showAlert("Duplicate",
                    "Category name already exists. Please choose another name.");
            return;
        }

        // ✅ insert
        if (CategoryDAO.insertCategory(name, desc)) {
            goBack();
        } else {
            showAlert("Error", "Failed to add category.");
        }
    }

    @FXML
    private void onCancel() {
        goBack();
    }

    private void goBack() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("Category.fxml"));
            StackPane contentArea =
                    (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
