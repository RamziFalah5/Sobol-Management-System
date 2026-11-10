package application;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SettingsController {
    @FXML private TextField txtUser, txtEmail;

    @FXML private void saveSettings() {
        System.out.println("Save settings: " + txtUser.getText() + " | " + txtEmail.getText());
    }
}
