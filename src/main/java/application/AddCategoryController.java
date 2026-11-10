package application;

import com.sobol.category.application.CategoryException;
import com.sobol.category.application.CategoryService;
import java.util.Objects;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

public class AddCategoryController {

    private final CategoryService service;

    public AddCategoryController() {
        this(CategoryModule.service());
    }

    AddCategoryController(CategoryService service) {
        this.service = Objects.requireNonNull(service);
    }

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;

    @FXML
    private void onAdd() {

        try {
            service.createCategory(txtName.getText(), txtDescription.getText());
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
