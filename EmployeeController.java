package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class EmployeeController {

	@FXML
	private Button workers;

	// ================= OPEN CATEGORIES =================

	@FXML
	private void openGeneralWorkers() {
		loadPage("GeneralWorkersMenu.fxml");
	}

	@FXML
	private void openTruckDrivers() {
		loadPage("Drivers.fxml");
	}

	@FXML
	private void openAdministrators() {
		loadPage("FieldAdministrators.fxml");
	}

	@FXML
	private void openSalesRepresentatives() {
		loadPage("SalesReps.fxml");
	}

	// ================= ADD EMPLOYEE =================

	@FXML
	private void addEmployee() {
		loadPage("AddEmployee.fxml");
	}

	// ================= HELPER =================

	private void loadPage(String fxml) {
		try {
			Parent view = FXMLLoader.load(getClass().getResource(fxml));

			StackPane contentArea = (StackPane) workers.getScene().getRoot().lookup("#contentArea");

			contentArea.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
