package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class EditCategoryController {

    @FXML private Label lblCategoryId;
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;

    private int categoryId;

    public void setCategory(Category c) {
        this.categoryId = c.getId();
        lblCategoryId.setText(String.valueOf(c.getId()));
        txtName.setText(c.nameProperty().get());
        txtDescription.setText(c.descriptionProperty().get());
    }

    @FXML
    private void onSave() {

        String name = txtName.getText().trim();
        String desc = txtDescription.getText().trim();

        if (name.isEmpty()) {
            showAlert("Validation", "Category name is required.");
            return;
        }

        if (CategoryDAO.categoryNameExistsForEdit(name, categoryId)) {
            showAlert("Duplicate",
                    "Another category already uses this name.");
            return;
        }

        if (CategoryDAO.updateCategory(categoryId, name, desc)) {
            goBack();
        } else {
            showAlert("Error", "Failed to update category.");
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
