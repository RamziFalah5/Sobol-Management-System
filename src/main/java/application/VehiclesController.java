package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.File;

public class VehiclesController {

	@FXML
	private TableView<Vehicle> vehiclesTable;

	@FXML
	private TableColumn<Vehicle, String> colImg;
	@FXML
	private TableColumn<Vehicle, Integer> colId;
	@FXML
	private TableColumn<Vehicle, String> colName;
	@FXML
	private TableColumn<Vehicle, String> colPlate;
	@FXML
	private TableColumn<Vehicle, String> colType;
	@FXML
	private TableColumn<Vehicle, String> colStatus;
	@FXML
	private TableColumn<Vehicle, Integer> colPallets;
	@FXML
	private TableColumn<Vehicle, Integer> colLoad;

	@FXML
	private TextField txtSearch;

	private ObservableList<Vehicle> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		colId.setCellValueFactory(d -> d.getValue().vehicleIdProperty().asObject());
		colName.setCellValueFactory(d -> d.getValue().nameProperty());
		colPlate.setCellValueFactory(d -> d.getValue().plateNumberProperty());
		colType.setCellValueFactory(d -> d.getValue().vehicleTypeProperty());
		colStatus.setCellValueFactory(d -> d.getValue().statusProperty());
		colPallets.setCellValueFactory(d -> d.getValue().capacityPalletsProperty().asObject());
		colLoad.setCellValueFactory(d -> d.getValue().maxLoadKgProperty().asObject());

		// ================= IMAGE COLUMN =================
		colImg.setCellValueFactory(d -> d.getValue().imagePathProperty());
		colImg.setCellFactory(col -> new TableCell<>() {

			private final ImageView iv = new ImageView();

			{
				iv.setFitWidth(50);
				iv.setFitHeight(38);
				iv.setPreserveRatio(true);
			}

			@Override
			protected void updateItem(String path, boolean empty) {
				super.updateItem(path, empty);

				if (empty) {
					setGraphic(null);
				} else {
					if (path != null && !path.isBlank()) {
						File f = new File(path);
						if (f.exists())
							iv.setImage(new Image(f.toURI().toString()));
						else
							iv.setImage(null);
					} else {
						iv.setImage(null);
					}
					setGraphic(iv);
				}
			}
		});

		// ✅ إخفاء عمود ID من الواجهة فقط
		colId.setVisible(false);

		// ✅ توسيط النص داخل الأعمدة (CSS FX)
		centerColumn(colName);
		centerColumn(colPlate);
		centerColumn(colType);
		centerColumn(colStatus);
		centerColumn(colPallets);
		centerColumn(colLoad);

		loadVehicles();

		txtSearch.textProperty().addListener((obs, o, n) -> filter(n));
	}

	// ================= CENTER COLUMN =================
	private void centerColumn(TableColumn<?, ?> col) {
		col.setStyle("-fx-alignment: CENTER;");
	}

	private void loadVehicles() {
		list.setAll(VehicleDAO.getAllVehicles());
		vehiclesTable.setItems(list);
	}

	private void filter(String key) {

		if (key == null || key.isBlank()) {
			vehiclesTable.setItems(list);
			return;
		}

		String k = key.toLowerCase();
		ObservableList<Vehicle> filtered = FXCollections.observableArrayList();

		for (Vehicle v : list) {
			if (v.getName().toLowerCase().contains(k) || v.getPlateNumber().toLowerCase().contains(k)
					|| v.getVehicleType().toLowerCase().contains(k)) {
				filtered.add(v);
			}
		}

		vehiclesTable.setItems(filtered);
	}

	@FXML
	private void openAdd() {
		go("AddVehicle.fxml");
	}

	@FXML
	private void openEdit() {

		Vehicle selected = vehiclesTable.getSelectionModel().getSelectedItem();

		if (selected == null) {
			alert("Edit Vehicle", "Please select a vehicle to edit.");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EditVehicle.fxml"));
			Parent view = loader.load();

			EditVehicleController c = loader.getController();
			c.setVehicle(selected);

			StackPane content = (StackPane) vehiclesTable.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void deleteVehicle() {

		Vehicle selected = vehiclesTable.getSelectionModel().getSelectedItem();

		if (selected == null) {
			alert("Delete Vehicle", "Please select a vehicle to delete.");
			return;
		}

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Delete Vehicle");
		confirm.setHeaderText(null);
		confirm.setContentText("Are you sure you want to delete this vehicle?");

		if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
			return;

		if (VehicleDAO.deleteVehicle(selected.getVehicleId())) {
			loadVehicles();
		}
	}

	private void go(String fxml) {
		try {
			Parent view = FXMLLoader.load(getClass().getResource(fxml));
			StackPane content = (StackPane) vehiclesTable.getScene().getRoot().lookup("#contentArea");
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
