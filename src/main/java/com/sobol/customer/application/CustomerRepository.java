package com.sobol.customer.application;

import com.sobol.customer.domain.Customer;
import java.util.List;
import java.util.Optional;

/** Non-null exact email uniqueness; repeated customer names are allowed. Uniqueness is enforced at write time; failures never become empty results. */
public interface CustomerRepository {
    List<Customer> findAll();
    Customer insert(Customer.Details details);
    /** Empty means missing; present includes a successful unchanged existing row. */
    Optional<Customer> update(int id, Customer.Details details);
}
