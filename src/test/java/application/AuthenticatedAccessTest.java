package application;

import com.sobol.auth.application.*;
import com.sobol.auth.domain.Account;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AuthenticatedAccessTest {
    @Test void productionNoArgumentControllersCannotBeConstructedAnonymously() {
        for (Runnable constructor : new Runnable[]{() -> new CategoryController(), () -> new AddCategoryController(), () -> new EditCategoryController(), () -> new SuppliersController(), () -> new AddSupplierController(), () -> new EditSupplierController(), () -> new CustomersController(), () -> new AddCustomerController(), () -> new EditCustomerController()})
            assertThrows(AuthenticationException.class, constructor::run);
    }
    @Test void retainedConnectionBoundaryRejectsLogoutReloginAndStatusChangesBeforeOpeningDataConnection() throws Exception {
        var time = new AuthTestSupport.Time(); var rows = new AuthTestSupport.Memory();
        rows.add(1, "one@example.invalid", Account.Status.ACTIVE, Account.Role.MEMBER);
        rows.add(2, "two@example.invalid", Account.Status.ACTIVE, Account.Role.ADMIN);
        var sessions = new SessionManager(rows, time);
        var service = AuthTestSupport.service(rows, new AuthTestSupport.Hasher(), sessions, time, false);
        var opened = new AtomicInteger();
        AuthModule.ConnectionSource source = () -> { opened.incrementAndGet(); return null; };
        assertThrows(AuthenticationException.class, () -> AuthModule.protect(sessions, source));
        service.login("one@example.invalid", AuthTestSupport.input()); var first = AuthModule.protect(sessions, source);
        first.open(); assertEquals(1, opened.get()); sessions.logout(); assertThrows(SQLException.class, first::open);
        service.login("two@example.invalid", AuthTestSupport.input()); assertThrows(SQLException.class, first::open);
        var second = AuthModule.protect(sessions, source); rows.add(2, "two@example.invalid", Account.Status.DISABLED, Account.Role.ADMIN);
        assertThrows(SQLException.class, second::open); assertEquals(1, opened.get());
    }
}
