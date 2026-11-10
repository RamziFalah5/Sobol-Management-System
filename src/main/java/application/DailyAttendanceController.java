package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyAttendanceController {

	@FXML
	private TableView<DailyAttendanceRow> table;
	@FXML
	private TableColumn<DailyAttendanceRow, String> colName;
	@FXML
	private TableColumn<DailyAttendanceRow, Boolean> colPresent;

	@FXML
	private Label lblDate;
	@FXML
	private Button btnSave;

	private final ObservableList<DailyAttendanceRow> list = FXCollections.observableArrayList();
	private final LocalDate today = LocalDate.now();

	@FXML
	public void initialize() {
		lblDate.setText("Date: " + today);

		colName.setCellValueFactory(d -> d.getValue().fullNameProperty());
		colPresent.setCellValueFactory(d -> d.getValue().presentProperty());
		colPresent.setCellFactory(CheckBoxTableCell.forTableColumn(colPresent));

		loadToday();
	}

	private void loadToday() {
		list.clear();
		for (GeneralWorkerRow w : loadDailyWorkers()) {
			Boolean existing = getRecordedPresence(w.getEmployeeId(), today);
			boolean present = (existing == null) ? true : existing; // ✅ default حاضر
			list.add(new DailyAttendanceRow(w.getEmployeeId(), w.getFullName(), present));
		}
		table.setItems(list);
	}

	@FXML
	private void onSave(javafx.event.ActionEvent e) {
		List<DailyAttendanceRow> rows = new ArrayList<>(list);
		if (persistDailyAttendance(today, rows)) {
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

	List<GeneralWorkerRow> loadDailyWorkers() {
		return GeneralWorkerDAO.getDailyWorkers();
	}

	Boolean getRecordedPresence(int employeeId, LocalDate date) {
		return AttendanceDAO.getPresent(employeeId, date);
	}

	boolean persistDailyAttendance(LocalDate date, List<DailyAttendanceRow> rows) {
		return AttendanceDAO.saveDaily(date, rows);
	}

	void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("DailyWorkers.fxml"));
			StackPane content = (StackPane) btnSave.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
