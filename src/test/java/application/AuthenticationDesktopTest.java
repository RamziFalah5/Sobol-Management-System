package application;

import com.sobol.auth.application.*;
import com.sobol.auth.domain.Account;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real FXML, events and scene changes; synthetic repositories and explicit injected composition. */
class AuthenticationDesktopTest {
    @BeforeAll static void start() throws Exception {
        assertEquals("false", System.getenv("SOBOL_PG_ENABLED"));
        var ready = new CompletableFuture<Void>();
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.complete(null); }); ready.get(30, TimeUnit.SECONDS);
    }
    @AfterAll static void stop() { Platform.exit(); }
    @AfterEach void closeWindows() throws Exception { fx(() -> { for (var window : java.util.List.copyOf(Window.getWindows())) ((Stage) window).close(); }); }
    interface Work { void run() throws Exception; }
    static void fx(Work work) throws Exception {
        var result = new CompletableFuture<Void>();
        Platform.runLater(() -> { try { work.run(); result.complete(null); } catch (Throwable failure) { result.completeExceptionally(failure); } });
        result.get(30, TimeUnit.SECONDS);
    }
    static final class Screen {
        final AuthUiTestSupport fixture;
        Stage stage;
        Screen(boolean registration) { this(registration, Runnable::run); }
        Screen(boolean registration, java.util.concurrent.Executor worker) { fixture = new AuthUiTestSupport(registration, worker); }
        void show(String page) throws Exception { stage = new Stage(); fixture.context.show(stage, page); }
        void credentials(String email, String value) {
            ((TextField) stage.getScene().lookup("#email")).setText(email);
            ((PasswordField) stage.getScene().lookup("#password")).setText(value);
        }
        Button button(String id) { return (Button) stage.getScene().lookup("#" + id); }
    }
    @Test void loginClearsPasswordAndEntersRealAuthenticatedHome() throws Exception {
        var s = new Screen(false);
        fx(() -> { s.show("Signin.fxml"); s.credentials("first@example.invalid", new String(AuthTestSupport.input())); s.button("loginBtn").fire(); assertEquals("", ((PasswordField) s.stage.getScene().lookup("#password")).getText()); });
        fx(() -> { assertNotNull(s.stage.getScene().lookup("#contentArea")); assertTrue(s.stage.getScene().getRoot().lookupAll(".label").stream().map(Label.class::cast).anyMatch(l -> l.getText().equals("Welcome, Synthetic 1"))); assertTrue(s.fixture.messages.isEmpty()); });
    }
    @Test void invalidDisabledAndUnavailableNeverEnterShell() throws Exception {
        var s = new Screen(false);
        fx(() -> { s.show("Signin.fxml"); s.credentials("first@example.invalid", "wrong"); s.button("loginBtn").fire(); });
        fx(() -> { assertNotNull(s.stage.getScene().lookup("#loginBtn")); assertThrows(AuthenticationException.class, s.fixture.sessions::capture); s.fixture.repository.add(1, "first@example.invalid", Account.Status.DISABLED, Account.Role.MEMBER); s.credentials("first@example.invalid", new String(AuthTestSupport.input())); s.button("loginBtn").fire(); });
        fx(() -> { assertEquals(s.fixture.messages.get(0), s.fixture.messages.get(1)); s.fixture.repository.unavailable = true; s.button("loginBtn").fire(); });
        fx(() -> { assertNotNull(s.stage.getScene().lookup("#loginBtn")); assertThrows(AuthenticationException.class, s.fixture.sessions::capture); assertEquals(3, s.fixture.messages.size()); });
    }
    @Test void anonymousDirectFxmlAndLegacySceneMethodCannotBypassLogin() throws Exception {
        var s = new Screen(false);
        fx(() -> { assertThrows(LoadException.class, () -> s.fixture.context.loader("System.fxml").load()); s.show("Signin.fxml"); Controller1 controller = s.fixture.latest.getController(); controller.switchToScene1(new ActionEvent(s.button("loginBtn"), null)); assertNotNull(s.stage.getScene().lookup("#loginBtn")); assertEquals(1, s.fixture.messages.size()); });
    }
    @Test void logoutClearsBusinessStateRejectsOldControllerAndAllowsDifferentIdentity() throws Exception {
        var s = new Screen(false);
        fx(() -> { s.fixture.login(1); s.show("System.fxml"); var old = s.stage.getScene(); var button = (ToggleButton) old.lookup("#btnCategory"); CategorySession.setSelectedCategory(42, "Synthetic"); BranchSession.setSelectedBranchId(42); s.button("logoutButton").fire(); assertFalse(CategorySession.hasSelectedCategory()); assertEquals(0, BranchSession.getSelectedBranchId()); assertNotNull(s.stage.getScene().lookup("#loginBtn")); button.fire(); assertTrue(((javafx.scene.layout.StackPane) old.lookup("#contentArea")).getChildren().isEmpty()); s.credentials("second@example.invalid", new String(AuthTestSupport.input())); s.button("loginBtn").fire(); });
        fx(() -> { assertEquals(2, s.fixture.sessions.require(s.fixture.sessions.capture()).accountId()); assertTrue(s.stage.getScene().getRoot().lookupAll(".label").stream().map(Label.class::cast).anyMatch(l -> l.getText().equals("Welcome, Synthetic 2"))); });
    }
    @Test void accountRevocationAndIdleExpiryClearProtectedContentOnNextNavigation() throws Exception {
        var s = new Screen(false);
        fx(() -> { s.fixture.login(1); s.show("System.fxml"); s.fixture.repository.add(1, "first@example.invalid", Account.Status.DISABLED, Account.Role.MEMBER); ((ToggleButton) s.stage.getScene().lookup("#btnCategory")).fire(); assertNotNull(s.stage.getScene().lookup("#loginBtn")); s.fixture.repository.add(1, "first@example.invalid", Account.Status.ACTIVE, Account.Role.MEMBER); s.fixture.login(1); s.fixture.context.show(s.stage, "System.fxml"); s.fixture.time.advance(900); ((ToggleButton) s.stage.getScene().lookup("#btnDashboard")).fire(); assertNotNull(s.stage.getScene().lookup("#loginBtn")); });
    }
    @Test void registrationClosedAndOptInPendingHaveNoAuthenticatedSession() throws Exception {
        for (boolean enabled : new boolean[]{false, true}) {
            var s = new Screen(enabled);
            fx(() -> { s.show("Main1.fxml"); assertInstanceOf(PasswordField.class, s.stage.getScene().lookup("#password")); ((TextField) s.stage.getScene().lookup("#name")).setText("Synthetic registration"); s.credentials("new@example.invalid", new String(AuthTestSupport.input())); s.button("register").fire(); });
            fx(() -> { assertThrows(AuthenticationException.class, s.fixture.sessions::capture); assertEquals(enabled ? 1 : 0, s.fixture.repository.writes); if (enabled) { assertNotNull(s.stage.getScene().lookup("#loginBtn")); assertEquals(Account.Status.PENDING, s.fixture.repository.rows.get(3L).status()); } else assertNotNull(s.stage.getScene().lookup("#register")); s.stage.close(); });
        }
    }
    @Test void queuedAuthenticationDoesNotBlockUiAndClosedWindowCannotRetainItsResult() throws Exception {
        var tasks = new java.util.ArrayDeque<Runnable>(); var s = new Screen(false, tasks::add);
        fx(() -> { s.show("Signin.fxml"); s.credentials("first@example.invalid", new String(AuthTestSupport.input()));
            s.button("loginBtn").fire(); assertTrue(s.button("loginBtn").isDisabled()); assertEquals(1,tasks.size());
            Controller1 controller = s.fixture.latest.getController(); controller.switchToScene3(new ActionEvent(s.button("loginBtn"),null));
            assertNotNull(s.stage.getScene().lookup("#loginBtn")); assertEquals(1,s.fixture.messages.size()); s.stage.close(); });
        tasks.remove().run();
        fx(() -> assertThrows(AuthenticationException.class,s.fixture.sessions::capture));
    }
    @Test void staleLoginCallbackCannotInvalidateANewerIdentity() throws Exception {
        var tasks = new java.util.ArrayDeque<Runnable>(); var s = new Screen(false, tasks::add);
        fx(() -> { s.show("Signin.fxml"); s.credentials("first@example.invalid",new String(AuthTestSupport.input())); s.button("loginBtn").fire();
            tasks.remove().run(); // Callback is queued, then a newer verified identity supersedes it.
            s.fixture.login(2); s.fixture.context.show(s.stage,"Signin.fxml"); });
        fx(() -> assertEquals(2,s.fixture.sessions.require(s.fixture.sessions.capture()).accountId()));
    }
    @Test void backgroundRevocationPollDoesNotKeepAnIdleSessionAlive() throws Exception {
        var s = new Screen(false);
        fx(() -> { s.fixture.login(1); s.show("System.fxml"); s.fixture.time.advance(899); ((ControllerSystem)s.fixture.latest.getController()).poll(); });
        fx(() -> { assertNotNull(s.stage.getScene().lookup("#contentArea")); s.fixture.time.advance(1); ((ControllerSystem)s.fixture.latest.getController()).poll(); });
        fx(() -> assertNotNull(s.stage.getScene().lookup("#loginBtn")));
    }
    @Test void threeRealMasterDataScreensAreReachableFromAuthenticatedShell() throws Exception {
        var s = new Screen(false);
        fx(() -> { s.fixture.login(1); s.show("System.fxml"); for (String id : new String[]{"btnCategory", "btnSupplier", "btnCustomer"}) { ((ToggleButton) s.stage.getScene().lookup("#" + id)).fire(); assertTrue(((ToggleButton) s.stage.getScene().lookup("#" + id)).isSelected()); assertFalse(((javafx.scene.layout.StackPane) s.stage.getScene().lookup("#contentArea")).getChildren().getFirst() instanceof Label); } });
    }
}
