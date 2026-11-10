package application;

import com.sobol.customer.application.CustomerService;
import com.sobol.database.PostgresDatabase;
import com.sobol.customer.infrastructure.JdbcCustomerRepository;

/** Explicit composition shared by Customer CRUD and sale-customer lookup FXML controllers. */
final class CustomerModule {
    private CustomerModule() { }
    static CustomerService service() {
        var access = AuthModule.protect(PostgresDatabase::get);
        return new CustomerService(new JdbcCustomerRepository(access::open));
    }
}
