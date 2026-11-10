package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("Signin.fxml"));
        Scene scene = new Scene(root , 480 ,640);
        if (getClass().getResource("application.css") != null) {
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        }
        stage.setTitle("javaTestSC");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
