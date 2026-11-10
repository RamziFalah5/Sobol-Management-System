package application;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class PostgresTransitionDesktopTest {
    @BeforeAll static void start() throws Exception {
        assertEquals("false", System.getenv("SOBOL_PG_ENABLED"));
        var ready = new CompletableFuture<Void>();
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.complete(null); }); ready.get(30, TimeUnit.SECONDS);
    }
    @AfterAll static void stop() { Platform.exit(); }
    @Test void actualShellShowsUnavailableStateWithoutLoadingLegacyDashboardOrOtherModules() throws Exception {
        var result = new CompletableFuture<Void>();
        Platform.runLater(() -> {
            try {
                var auth = new AuthUiTestSupport(false); auth.login(1);
                var loader = auth.context.loader("System.fxml");
                Parent root = loader.load(); new Scene(root);
                StackPane content = (StackPane) loader.getNamespace().get("contentArea");
                assertInstanceOf(javafx.scene.layout.VBox.class, content.getChildren().getFirst());
                for (String id : new String[]{"btnSales", "btnImports", "btnBranches", "btnEmployee", "btnVehicles", "btnReports", "btnSettings"}) {
                    ToggleButton button = (ToggleButton) loader.getNamespace().get(id);
                    assertNotNull(button, id); button.fire(); assertTrue(button.isSelected()); assertUnavailable(content);
                }
                result.complete(null);
            } catch (Throwable failure) { result.completeExceptionally(failure); }
        });
        result.get(30, TimeUnit.SECONDS);
    }
    private void assertUnavailable(StackPane content) {
        assertEquals(1, content.getChildren().size());
        Label label = assertInstanceOf(Label.class, content.getChildren().getFirst());
        assertEquals("This module is temporarily unavailable.", label.getText());
    }
}
