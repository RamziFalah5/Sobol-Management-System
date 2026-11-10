package application;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.File;

public class SalesRepsController {

	@FXML
	private TableView<SalesRepRow> table;

	@FXML
	private TableColumn<SalesRepRow, String> colImage;
	@FXML
	private TableColumn<SalesRepRow, String> colName;
	@FXML
	private TableColumn<SalesRepRow, String> colPhone;
	@FXML
	private TableColumn<SalesRepRow, String> colBranch;
	@FXML
	private TableColumn<SalesRepRow, Double> colRate;
	@FXML
	private TableColumn<SalesRepRow, Double> colCommission;

	@FXML
	private TableColumn<SalesRepRow, String> colHire;

	@FXML
	private Button btnAnchor;

	private final ObservableList<SalesRepRow> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		// values
		colName.setCellValueFactory(d -> d.getValue().fullNameProperty());
		colPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
		colBranch.setCellValueFactory(d -> d.getValue().branchNameProperty());
		colRate.setCellValueFactory(d -> d.getValue().dailyRateProperty().asObject());
		colCommission.setCellValueFactory(d -> d.getValue().commissionRateProperty().asObject());

		// ✅ hire date -> String (حل نهائي)
		colHire.setCellValueFactory(d -> {
			if (d.getValue().getHireDate() == null)
				return new SimpleStringProperty("");
			return new SimpleStringProperty(d.getValue().getHireDate().toString());
		});

		// ✅ photo column (ImageView)
		colImage.setCellValueFactory(d -> d.getValue().imagePathProperty());
		colImage.setCellFactory(tc -> new TableCell<>() {

			private final ImageView iv = new ImageView();

			{
				iv.setFitWidth(42);
				iv.setFitHeight(42);
				iv.setPreserveRatio(true);
				setStyle("-fx-alignment: CENTER;");
			}

			@Override
			protected void updateItem(String path, boolean empty) {
				super.updateItem(path, empty);

				if (empty) {
					setGraphic(null);
					setText(null);
					return;
				}

				if (path == null || path.trim().isEmpty() || !new File(path).exists()) {
					iv.setImage(null);
				} else {
					iv.setImage(new Image(new File(path).toURI().toString(), 42, 42, true, true));
				}

				setGraphic(iv);
				setText(null);
			}
		});

		// ✅ Center alignment for all columns
		center(colImage);
		center(colName);
		center(colPhone);
		center(colBranch);
		center(colRate);
		center(colCommission);
		center(colHire);

		load();
	}

	private void center(TableColumn<?, ?> col) {
		col.setStyle("-fx-alignment: CENTER;");
	}

	private void load() {
		list.setAll(SalesRepDAO.getAllSalesReps());
		table.setItems(list);
	}

	@FXML
	private void onEdit() {
		SalesRepRow sel = table.getSelectionModel().getSelectedItem();
		if (sel == null) {
			alert("Edit", "Please select a sales representative to edit.");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EditSalesRep.fxml"));
			Parent view = loader.load();
			EditSalesRepController c = loader.getController();
			c.setRep(sel);

			StackPane content = (StackPane) btnAnchor.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onDelete() {
		SalesRepRow sel = table.getSelectionModel().getSelectedItem();
		if (sel == null) {
			alert("Delete", "Please select the employee you want to delete.");
			return;
		}

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Delete");
		confirm.setHeaderText(null);
		confirm.setContentText("Are you sure you want to delete this employee?");
		if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
			return;

		if (SalesRepDAO.deleteSalesRep(sel.getEmployeeId()))
			load();
		else
			alert("Delete", "Failed to delete employee.");
	}

	@FXML
	private void onAttendance() {
		go("SalesAttendance.fxml");
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
