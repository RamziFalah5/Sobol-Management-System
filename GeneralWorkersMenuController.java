package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class GeneralWorkersMenuController {

	@FXML
	private Button btnDaily;

	@FXML
	private void openDaily() {
		go("DailyWorkers.fxml");
	}

	@FXML
	private void openHourly() {
		go("HourlyWorkers.fxml");
	}

	private void go(String fxml) {
		try {
			Parent view = FXMLLoader.load(getClass().getResource(fxml));
			StackPane content = (StackPane) btnDaily.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
