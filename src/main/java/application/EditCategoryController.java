package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class EditCategoryController {

    @FXML private Label lblCategoryId;
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;

    private int categoryId = -1;

    public void setCategory(Category c) {
        categoryId = -1;
        if (c == null || c.getId() <= 0) {
            throw new IllegalArgumentException("A valid selected category is required.");
        }
        this.categoryId = c.getId();
        lblCategoryId.setText(String.valueOf(c.getId()));
        txtName.setText(c.nameProperty().get());
        txtDescription.setText(c.descriptionProperty().get() == null ? "" : c.descriptionProperty().get());
    }

    @FXML
    private void onSave() {

        if (categoryId <= 0) {
            showAlert("Validation", "Please select a category before editing.");
            return;
        }

        String name = txtName.getText().trim();
        String desc = txtDescription.getText().trim();

        if (name.isEmpty()) {
            showAlert("Validation", "Category name is required.");
            return;
        }

        if (nameExistsForEdit(name, categoryId)) {
            showAlert("Duplicate",
                    "Another category already uses this name.");
            return;
        }

        if (updateCategory(categoryId, name, desc)) {
            goBack();
        } else {
            showAlert("Error", "Failed to update category.");
        }
    }

    @FXML
    private void onCancel() {
        goBack();
    }

    boolean nameExistsForEdit(String name, int currentId) {
        return CategoryDAO.categoryNameExistsForEdit(name, currentId);
    }

    boolean updateCategory(int id, String name, String description) {
        return CategoryDAO.updateCategory(id, name, description);
    }

    void goBack() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("Category.fxml"));
            StackPane contentArea =
                    (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
