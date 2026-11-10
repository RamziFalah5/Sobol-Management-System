package application;

import com.sobol.auth.application.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Arrays;

/** FXML adapter: validation, KDF and persistence belong to AuthenticationService. */
public class Controller1 {
    @FXML private TextField name;
    @FXML private TextField email;
    @FXML private TextField phone;
    @FXML private PasswordField password;
    private final AuthModule.Context auth;
    private boolean busy;
    private long operation;
    public Controller1() { this(AuthModule.context()); }
    Controller1(AuthModule.Context auth) { this.auth = auth; }

    @FXML public void switchToScene1(ActionEvent event) {
        try { auth.sessions.capture(); auth.show(stage(event), "System.fxml"); }
        catch (AuthenticationException | IOException failure) { auth.sessions.logout(); message("Sign in is required."); }
    }
    @FXML public void switchToScene2(ActionEvent event) { publicPage(event, "Signin.fxml"); }
    @FXML public void switchToScene3(ActionEvent event) { publicPage(event, "Main1.fxml"); }
    private void publicPage(ActionEvent event, String page) {
        if (busy) { message("Please wait for the current request."); return; }
        operation++; auth.sessions.logout(); clearBusinessSelection();
        if (password != null) password.clear();
        try { auth.show(stage(event), page); }
        catch (IOException failure) { message("Screen cannot be opened."); }
    }
    @FXML public void onLogin(ActionEvent event) { submit(event, false); }
    @FXML public void onRegister(ActionEvent event) { submit(event, true); }
    private void submit(ActionEvent event, boolean registration) {
        if (busy) return;
        String login = text(email), displayName = text(name), contact = text(phone);
        char[] secret = password == null ? new char[0] : password.getText().toCharArray();
        if (password != null) password.clear();
        Stage window = stage(event);
        if (window == null || !window.isShowing()) { Arrays.fill(secret, (char) 0); return; }
        var originalScene = window.getScene();
        long request = ++operation;
        busy = true;
        Node button = event.getSource() instanceof Node n ? n : null;
        if (button != null) button.setDisable(true);
        try {
            auth.worker.execute(() -> {
                AuthenticationException error = null;
                SessionManager.Handle authenticated = null;
                try {
                    if (registration) auth.service.register(login, displayName, contact, secret);
                    else authenticated = auth.service.login(login, secret);
                } catch (AuthenticationException failure) { error = failure; }
                finally { Arrays.fill(secret, (char) 0); }
                AuthenticationException result = error;
                SessionManager.Handle completedSession = authenticated;
                Platform.runLater(() -> {
                    busy = false;
                    if (button != null) button.setDisable(false);
                    if (request != operation || window.getScene() != originalScene || !window.isShowing()) {
                        if (completedSession != null) auth.sessions.invalidate(completedSession);
                        return;
                    }
                    if (result != null) { message(result.getMessage()); return; }
                    try {
                        if (registration) {
                            message("If this account can be registered, it is pending owner approval. Sign in after approval.");
                            auth.show(window, "Signin.fxml");
                        } else {
                            auth.sessions.capture(); clearBusinessSelection(); auth.show(window, "System.fxml");
                        }
                    } catch (AuthenticationException | IOException failure) {
                        if (completedSession != null) auth.sessions.invalidate(completedSession);
                        message("The screen could not be opened. Please sign in again.");
                    }
                });
            });
        } catch (RuntimeException failure) {
            Arrays.fill(secret, (char) 0); busy = false; if (button != null) button.setDisable(false);
            auth.sessions.logout(); message("Authentication is temporarily unavailable.");
        }
    }
    static void clearBusinessSelection() { CategorySession.clear(); BranchSession.setSelectedBranchId(0); ShipmentSession.clear(); }
    private static String text(TextField field) { return field == null ? "" : field.getText(); }
    private static Stage stage(ActionEvent event) { return (Stage) ((Node) event.getSource()).getScene().getWindow(); }
    void message(String text) {
        var alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle("Sobol sign in");
        alert.setHeaderText(null); alert.setContentText(text); alert.showAndWait();
    }
}
