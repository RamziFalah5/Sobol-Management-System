package application;

import com.sobol.category.application.CategoryException;
import com.sobol.category.application.CategoryService;
import java.util.Objects;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class EditCategoryController {

    private final CategoryService service;

    public EditCategoryController() {
        this(CategoryModule.service());
    }

    EditCategoryController(CategoryService service) {
        this.service = Objects.requireNonNull(service);
    }

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

        try {
            service.updateCategory(categoryId, txtName.getText(), txtDescription.getText());
            goBack();
        } catch (CategoryException failure) {
            String title = switch (failure.reason()) {
                case INVALID_NAME, INVALID_ID -> "Validation";
                case DUPLICATE_NAME -> "Duplicate";
                case NOT_FOUND -> "Not Found";
                case STORAGE_FAILURE -> "Error";
            };
            showAlert(title, failure.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        goBack();
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
