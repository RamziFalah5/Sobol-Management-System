package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class DriversController {

	@FXML
	private TableView<TruckDriverRow> table;

	@FXML
	private TableColumn<TruckDriverRow, ImageView> colPhoto;
	@FXML
	private TableColumn<TruckDriverRow, String> colName;
	@FXML
	private TableColumn<TruckDriverRow, String> colPhone;
	@FXML
	private TableColumn<TruckDriverRow, String> colBranch;
	@FXML
	private TableColumn<TruckDriverRow, String> colVehicle;
	@FXML
	private TableColumn<TruckDriverRow, Double> colRate;
	@FXML
	private TableColumn<TruckDriverRow, String> colHire;

	@FXML
	private Button btnAnchor;

	private final ObservableList<TruckDriverRow> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		colPhoto.setCellValueFactory(d -> d.getValue().photoProperty());
		colName.setCellValueFactory(d -> d.getValue().fullNameProperty());
		colPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
		colBranch.setCellValueFactory(d -> d.getValue().branchNameProperty());
		colVehicle.setCellValueFactory(d -> d.getValue().vehicleNameProperty());
		colRate.setCellValueFactory(d -> d.getValue().dailyRateProperty().asObject());
		colHire.setCellValueFactory(d -> d.getValue().hireDateProperty());

		// ✅ Center all cells (بديل عن .table-cell css)
		centerColumn(colName);
		centerColumn(colPhone);
		centerColumn(colBranch);
		centerColumn(colVehicle);
		centerColumn(colRate);
		centerColumn(colHire);
		centerImageColumn(colPhoto);

		load();
	}

	private void load() {
		list.setAll(TruckDriverDAO.getAllDrivers());
		table.setItems(list);
	}

	private <T> void centerColumn(TableColumn<TruckDriverRow, T> col) {
		col.setCellFactory(tc -> new TableCell<>() {
			@Override
			protected void updateItem(T item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? "" : String.valueOf(item));
				setGraphic(null);
				setStyle("-fx-alignment: CENTER;");
			}
		});
	}

	private void centerImageColumn(TableColumn<TruckDriverRow, ImageView> col) {
		col.setCellFactory(tc -> new TableCell<>() {
			@Override
			protected void updateItem(ImageView item, boolean empty) {
				super.updateItem(item, empty);
				setText(null);
				setGraphic(empty ? null : item);
				setStyle("-fx-alignment: CENTER;");
			}
		});
	}

	@FXML
	private void onEdit() {
		TruckDriverRow sel = table.getSelectionModel().getSelectedItem();
		if (sel == null) {
			alert("Edit", "Please select a driver to edit.");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EditDriver.fxml"));
			Parent view = loader.load();

			EditDriverController c = loader.getController();
			c.setDriver(sel);

			StackPane content = (StackPane) btnAnchor.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onDelete() {
		TruckDriverRow sel = table.getSelectionModel().getSelectedItem();
		if (sel == null) {
			alert("Delete", "Please select the employee you want to delete.");
			return;
		}

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Delete Driver");
		confirm.setHeaderText(null);
		confirm.setContentText("Are you sure you want to delete this driver?");
		if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
			return;

		if (TruckDriverDAO.deleteDriver(sel.getEmployeeId())) {
			load();
		} else {
			alert("Delete", "Failed to delete driver.");
		}
	}

	@FXML
	private void onAttendance() {
		go("DriverAttendance.fxml");
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
