package application;

import javafx.application.Platform;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in desktop smoke check. No login, CRUD, or database connection is attempted. */
class ApplicationStartupTest {
    @Test void originalEntryPointShowsSigninWithDatabaseDisabled() throws Exception {
        assertEquals("false", System.getenv("SOBOL_DB_ENABLED"));
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            ready.complete(null);
        });
        ready.get(30, TimeUnit.SECONDS);
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage stage = new Stage();
            try {
                new Main().start(stage);
                assertTrue(stage.isShowing());
                assertNotNull(stage.getScene().lookup("#email"));
                assertInstanceOf(PasswordField.class, stage.getScene().lookup("#password"));
                result.complete(null);
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            } finally {
                stage.close();
            }
        });
        try {
            result.get(30, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }
}
