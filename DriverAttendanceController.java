package application;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;

public class DriverAttendanceController {

	public static class DriverAttendanceRow {
		private final TruckDriverRow driver;
		private final CheckBox present = new CheckBox();

		public DriverAttendanceRow(TruckDriverRow driver) {
			this.driver = driver;
			present.setSelected(true); // ✅ default all present
		}

		public TruckDriverRow getDriver() {
			return driver;
		}

		public String getName() {
			return driver.getFullName();
		}

		public String getBranch() {
			return driver.getBranchName();
		}

		public CheckBox getPresent() {
			return present;
		}
	}

	@FXML
	private TableView<DriverAttendanceRow> table;
	@FXML
	private TableColumn<DriverAttendanceRow, String> colName;
	@FXML
	private TableColumn<DriverAttendanceRow, String> colBranch;
	@FXML
	private TableColumn<DriverAttendanceRow, CheckBox> colPresent;

	@FXML
	private Button btnAnchor;

	private final ObservableList<DriverAttendanceRow> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		// ✅ لو صار injection غلط رح تفهمه مباشرة بدل ما يعطيك NPE غبي
		if (colName == null || colBranch == null || colPresent == null || table == null) {
			System.out.println("FXML Injection Failed! Check fx:id names in DriverAttendance.fxml");
			return;
		}

		colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
		colBranch.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBranch()));
		colPresent.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPresent()));

		// center text columns
		centerString(colName);
		centerString(colBranch);

		// center checkbox column
		colPresent.setCellFactory(tc -> new TableCell<>() {
			@Override
			protected void updateItem(CheckBox item, boolean empty) {
				super.updateItem(item, empty);
				setText(null);
				setGraphic(empty ? null : item);
				setStyle("-fx-alignment: CENTER;");
			}
		});

		load();
	}

	private void load() {
		list.clear();
		for (TruckDriverRow r : TruckDriverDAO.getAllDrivers()) {
			list.add(new DriverAttendanceRow(r));
		}
		table.setItems(list);
	}

	private void centerString(TableColumn<DriverAttendanceRow, String> col) {
		col.setCellFactory(tc -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? "" : item);
				setGraphic(null);
				setStyle("-fx-alignment: CENTER;");
			}
		});
	}

	@FXML
	private void onSave() {

		LocalDate today = LocalDate.now();
		boolean anyFail = false;

		for (DriverAttendanceRow r : list) {
			boolean present = r.getPresent().isSelected();

			boolean ok = AttendanceDAO.upsertAttendance(r.getDriver().getEmployeeId(), today, present, null // ✅
																											// drivers:
																											// no hours
			);

			if (!ok)
				anyFail = true;
		}

		if (anyFail) {
			alert("Attendance", "Some attendance records failed to save.");
			return;
		}

		alert("Attendance", "Attendance saved successfully.");
		goBack();
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Drivers.fxml"));
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
