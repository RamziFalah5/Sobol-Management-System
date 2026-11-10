package application;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        AuthModule.context().sessions.logout();
        stage.setTitle("Sobol Management System");
        stage.setOnCloseRequest(event -> AuthModule.context().sessions.logout());
        AuthModule.context().show(stage, "Signin.fxml");
    }

    @Override public void stop() { AuthModule.context().sessions.logout(); }

    public static void main(String[] args) {
        launch(args);
    }
}
