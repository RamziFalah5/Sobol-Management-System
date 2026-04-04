package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HourlyAttendanceController {

	@FXML
	private TableView<HourlyAttendanceRow> table;
	@FXML
	private TableColumn<HourlyAttendanceRow, String> colName;
	@FXML
	private TableColumn<HourlyAttendanceRow, Number> colHours;

	@FXML
	private Label lblDate;
	@FXML
	private Button btnSave;

	private final ObservableList<HourlyAttendanceRow> list = FXCollections.observableArrayList();
	private final LocalDate today = LocalDate.now();

	@FXML
	public void initialize() {
		lblDate.setText("Date: " + today);

		colName.setCellValueFactory(d -> d.getValue().fullNameProperty());
		colHours.setCellValueFactory(d -> d.getValue().hoursProperty());

		colHours.setCellFactory(col -> new TableCell<>() {
			private final Spinner<Integer> sp = new Spinner<>(0, 24, 3);
			{
				sp.setEditable(true);
			}

			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					HourlyAttendanceRow row = getTableView().getItems().get(getIndex());
					sp.getValueFactory().setValue(row.getHours());
					sp.valueProperty().addListener((obs, o, n) -> row.hoursProperty().set(n));
					setGraphic(sp);
				}
			}
		});

		loadToday();
	}

	private void loadToday() {
		list.clear();
		for (GeneralWorkerRow w : GeneralWorkerDAO.getHourlyWorkers()) {
			Integer existing = AttendanceDAO.getHours(w.getEmployeeId(), today);
			int hours = (existing == null) ? 3 : existing; // ✅ default 3
			list.add(new HourlyAttendanceRow(w.getEmployeeId(), w.getFullName(), hours));
		}
		table.setItems(list);
	}

	@FXML
	private void onSave(javafx.event.ActionEvent e) {
		List<HourlyAttendanceRow> rows = new ArrayList<>(list);
		if (AttendanceDAO.saveHourly(today, rows)) {
			alert("Saved", "Attendance saved for " + today + " (updated if existed).");
			goBack();
		} else {
			alert("Error", "Failed to save attendance.");
		}
	}

	@FXML
	private void onCancel(javafx.event.ActionEvent e) {
		goBack();
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("HourlyWorkers.fxml"));
			StackPane content = (StackPane) btnSave.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception ex) {
			ex.printStackTrace();
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
