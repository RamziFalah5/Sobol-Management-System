package application;

import com.sobol.customer.application.CustomerService;
import com.sobol.customer.infrastructure.JdbcCustomerRepository;

/** Explicit composition shared by Customer CRUD and sale-customer lookup FXML controllers. */
final class CustomerModule {
    private CustomerModule() { }
    static CustomerService service() {
        return new CustomerService(new JdbcCustomerRepository(DBAuth::get));
    }
}
