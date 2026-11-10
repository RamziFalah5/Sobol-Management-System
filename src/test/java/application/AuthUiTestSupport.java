package application;

import com.sobol.auth.application.*;
import com.sobol.auth.application.AuthTestSupport.*;
import com.sobol.auth.domain.Account;
import javafx.fxml.FXMLLoader;
import java.lang.reflect.Proxy;
import java.util.*;

/** Package-local synthetic composition. Never part of the shipped application. */
final class AuthUiTestSupport {
    final Time time = new Time();
    final Memory repository = new Memory();
    final SessionManager sessions = new SessionManager(repository, time);
    final List<String> messages = new ArrayList<>();
    final AuthModule.Context context;
    FXMLLoader latest;
    AuthUiTestSupport(boolean registration) { this(registration, Runnable::run); }
    AuthUiTestSupport(boolean registration, java.util.concurrent.Executor worker) {
        var service = AuthTestSupport.service(repository, new Hasher(), sessions, time, registration);
        context = new AuthModule.Context(service, sessions, worker) {
            @Override Object controller(Class<?> type) {
                if (type == Controller1.class) return new Controller1(this) {
                    @Override void message(String message) { messages.add(message); }
                };
                if (type == CategoryController.class) return new CategoryController(new com.sobol.category.application.CategoryService(empty(com.sobol.category.application.CategoryRepository.class)));
                if (type == SuppliersController.class) return new SuppliersController(new com.sobol.supplier.application.SupplierService(empty(com.sobol.supplier.application.SupplierRepository.class)));
                if (type == CustomersController.class) return new CustomersController(new com.sobol.customer.application.CustomerService(empty(com.sobol.customer.application.CustomerRepository.class)));
                return super.controller(type);
            }
            @Override FXMLLoader loader(String page) { latest = super.loader(page); return latest; }
        };
        repository.add(1, "first@example.invalid", Account.Status.ACTIVE, Account.Role.MEMBER);
        repository.add(2, "second@example.invalid", Account.Status.ACTIVE, Account.Role.ADMIN);
    }
    void login(long id) { context.service.login(repository.rows.get(id).login(), AuthTestSupport.input()); }
    @SuppressWarnings("unchecked") private static <T> T empty(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getReturnType() == List.class) return List.of();
            throw new AssertionError("Unexpected persistence operation " + method.getName());
        });
    }
}
