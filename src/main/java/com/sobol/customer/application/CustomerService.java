package com.sobol.customer.application;

import com.sobol.customer.domain.Customer;
import java.util.List;
import java.util.Objects;
import static com.sobol.customer.application.CustomerException.Reason.*;

/** Customer use cases shared by CRUD forms and customer selection for a sale. */
public final class CustomerService {
    private final CustomerRepository repository;
    public CustomerService(CustomerRepository repository) { this.repository = Objects.requireNonNull(repository); }
    public List<Customer> listCustomers() { return List.copyOf(repository.findAll()); }

    public Customer createCustomer(String fullName, String phone, String email, String city, String address,
                                   String notes) {
        return repository.insert(validate(fullName, phone, email, city, address, notes));
    }

    public Customer updateCustomer(int id, String fullName, String phone, String email, String city, String address,
                                   String notes) {
        if (id <= 0) throw new CustomerException(INVALID_ID);
        return repository.update(id, validate(fullName, phone, email, city, address, notes))
                .orElseThrow(() -> new CustomerException(NOT_FOUND));
    }

    private Customer.Details validate(String fullName, String phone, String email, String city, String address,
                                      String notes) {
        try { return new Customer.Details(fullName, phone, email, city, address, notes); }
        catch (Customer.InvalidField failure) {
            throw new CustomerException(switch (failure.field()) {
                case NAME -> INVALID_NAME;
                case PHONE -> INVALID_PHONE;
                case EMAIL -> INVALID_EMAIL;
                case CITY -> INVALID_CITY;
                case ADDRESS -> INVALID_ADDRESS;
                case NOTES -> INVALID_NOTES;
            });
        }
    }
}
