package application;

import com.sobol.auth.application.*;
import com.sobol.auth.infrastructure.*;
import com.sobol.database.PostgresDatabase;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
import java.time.Clock;
import java.util.concurrent.*;

/** Application composition only. No credentials or test identity are embedded here. */
final class AuthModule {
    private AuthModule() { }
    private static final class Shared {
        static final Context CONTEXT = production();
        private static Context production() {
            var repository = new JdbcAccountRepository(PostgresDatabase::get);
            var clock = Clock.systemUTC();
            var sessions = new SessionManager(repository, clock);
            var service = new AuthenticationService(repository, new Pbkdf2PasswordHasher(), sessions,
                    new LoginThrottle(clock), () -> "true".equals(System.getenv("SOBOL_REGISTRATION_ENABLED")));
            Executor worker = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "sobol-auth"); thread.setDaemon(true); return thread;
            });
            return new Context(service, sessions, worker);
        }
    }
    static Context context() { return Shared.CONTEXT; }
    @FunctionalInterface interface ConnectionSource { Connection open() throws SQLException; }
    /** Captures this identity, so a retained form cannot silently act as the next user. */
    static ConnectionSource protect(ConnectionSource source) { return protect(context().sessions, source); }
    static ConnectionSource protect(SessionManager sessions, ConnectionSource source) {
        var handle = sessions.capture();
        return () -> {
            try { sessions.require(handle); }
            catch (AuthenticationException failure) { throw new SQLException("Authentication is required for this operation."); }
            return source.open();
        };
    }
    static class Context {
        final AuthenticationService service;
        final SessionManager sessions;
        final Executor worker;
        Context(AuthenticationService service, SessionManager sessions, Executor worker) {
            this.service = service; this.sessions = sessions; this.worker = worker;
        }
        Object controller(Class<?> type) {
            if (type == Controller1.class) return new Controller1(this);
            if (type == ControllerSystem.class) return new ControllerSystem(this);
            try { return type.getDeclaredConstructor().newInstance(); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException("Screen cannot be opened."); }
        }
        FXMLLoader loader(String page) {
            var loader = new FXMLLoader(AuthModule.class.getResource(page));
            loader.setControllerFactory(this::controller); return loader;
        }
        void show(Stage stage, String page) throws IOException {
            Parent root = loader(page).load();
            boolean shell = "System.fxml".equals(page);
            var scene = new Scene(root, shell ? 1180 : 480, shell ? 760 : 640);
            var css = AuthModule.class.getResource("application.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene); stage.centerOnScreen(); stage.show();
        }
    }
}
