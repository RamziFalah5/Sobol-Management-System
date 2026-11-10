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

public class SalesAttendanceController {

	public static class SalesAttendanceRow {
		private final SalesRepRow rep;
		private final CheckBox present = new CheckBox();

		public SalesAttendanceRow(SalesRepRow rep) {
			this.rep = rep;
			present.setSelected(true); // ✅ default present
		}

		public SalesRepRow getRep() {
			return rep;
		}

		public String getName() {
			return rep.getFullName();
		}

		public String getBranch() {
			return rep.getBranchName();
		}

		public CheckBox getPresent() {
			return present;
		}
	}

	@FXML
	private TableView<SalesAttendanceRow> table;
	@FXML
	private TableColumn<SalesAttendanceRow, String> colName;
	@FXML
	private TableColumn<SalesAttendanceRow, String> colBranch;
	@FXML
	private TableColumn<SalesAttendanceRow, CheckBox> colPresent;

	@FXML
	private Button btnAnchor;

	private final ObservableList<SalesAttendanceRow> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
		colBranch.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBranch()));
		colPresent.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPresent()));

		// center checkbox
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
		for (SalesRepRow r : SalesRepDAO.getAllSalesReps()) {
			list.add(new SalesAttendanceRow(r));
		}
		table.setItems(list);
	}

	@FXML
	private void onSave() {

		LocalDate today = LocalDate.now();
		boolean anyFail = false;

		for (SalesAttendanceRow r : list) {
			boolean present = r.getPresent().isSelected();

			boolean ok = AttendanceDAO.upsertAttendance(r.getRep().getEmployeeId(), today, present, null // ✅ sales: no
																											// hours
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
			Parent view = FXMLLoader.load(getClass().getResource("SalesReps.fxml"));
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
