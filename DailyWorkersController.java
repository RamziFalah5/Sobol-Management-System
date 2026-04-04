package application;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class DailyWorkersController {

	@FXML
	private TableView<GeneralWorkerRow> table;

	@FXML
	private TableColumn<GeneralWorkerRow, ImageView> colImage;
	@FXML
	private TableColumn<GeneralWorkerRow, String> colName;
	@FXML
	private TableColumn<GeneralWorkerRow, String> colPhone;
	@FXML
	private TableColumn<GeneralWorkerRow, String> colBranch;
	@FXML
	private TableColumn<GeneralWorkerRow, Double> colRate;
	@FXML
	private TableColumn<GeneralWorkerRow, String> colHire;

	@FXML
	private Button btnAnchor;

	private final ObservableList<GeneralWorkerRow> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		colImage.setCellValueFactory(d -> {
			ImageView iv = new ImageView();
			iv.setFitWidth(40);
			iv.setFitHeight(40);
			iv.setPreserveRatio(true);

			String path = d.getValue().getImagePath();
			Image img;

			if (path != null && !path.isBlank()) {
				img = new Image("file:" + path, true);
			} else {
				img = new Image(getClass().getResourceAsStream("/images/user.png"));
			}

			iv.setImage(img);
			return new ReadOnlyObjectWrapper<>(iv);
		});

		colName.setCellValueFactory(d -> d.getValue().fullNameProperty());
		colPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
		colBranch.setCellValueFactory(d -> d.getValue().branchNameProperty());
		colRate.setCellValueFactory(d -> d.getValue().rateProperty().asObject());
		colHire.setCellValueFactory(d -> d.getValue().hireDateProperty());

		load();
	}

	private void load() {
		list.setAll(GeneralWorkerDAO.getDailyWorkers());
		table.setItems(list);
	}

	@FXML
	private void onEdit() {
		GeneralWorkerRow sel = table.getSelectionModel().getSelectedItem();

		if (sel == null) {
			alert("Edit", "Please select a worker to edit.");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EditDailyWorker.fxml"));
			Parent view = loader.load();

			EditDailyWorkerController c = loader.getController();
			c.setWorker(sel);

			StackPane content = (StackPane) btnAnchor.getScene().getRoot().lookup("#contentArea");

			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onDelete() {
		GeneralWorkerRow sel = table.getSelectionModel().getSelectedItem();

		if (sel == null) {
			alert("Delete", "Please select a worker to delete.");
			return;
		}

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Delete Worker");
		confirm.setHeaderText(null);
		confirm.setContentText("Are you sure you want to delete this worker?");

		if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
			return;

		if (GeneralWorkerDAO.deleteEmployee(sel.getEmployeeId()))
			load();
	}

	@FXML
	private void onAttendance() {
		go("DailyAttendance.fxml");
	}

	@FXML
	private void onView() {
		alert("View", "View is not implemented yet.");
	}

	private void go(String fxml) {
		try {
			Parent view = FXMLLoader.load(getClass().getResource(fxml));
			StackPane content = (StackPane) btnAnchor.getScene().getRoot().lookup("#contentArea");
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
