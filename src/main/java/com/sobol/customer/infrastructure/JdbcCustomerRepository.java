package com.sobol.customer.infrastructure;

import com.sobol.customer.application.CustomerException;
import com.sobol.customer.application.CustomerRepository;
import com.sobol.customer.domain.Customer;
import java.sql.*;
import java.util.*;
import static com.sobol.customer.application.CustomerException.Reason.*;

/** Owns fresh supplied connections. The composition root supplies the guarded development boundary. */
public final class JdbcCustomerRepository implements CustomerRepository {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    private final Connections connections;
    public JdbcCustomerRepository(Connections connections) { this.connections = Objects.requireNonNull(connections); }

    @Override public List<Customer> findAll() {
        try (Connection c = connections.open();
             PreparedStatement p = c.prepareStatement("SELECT customer_id, full_name, phone, email, city, address, notes FROM customer ORDER BY customer_id DESC");
             ResultSet r = p.executeQuery()) {
            List<Customer> rows = new ArrayList<>();
            while (r.next()) rows.add(new Customer(r.getInt("customer_id"), new Customer.Details(
                    r.getString("full_name"), r.getString("phone"), r.getString("email"), r.getString("city"),
                    r.getString("address"), r.getString("notes"))));
            return List.copyOf(rows);
        } catch (SQLException | IllegalArgumentException failure) { throw storage(); }
    }

    @Override public Customer insert(Customer.Details details) {
        try (Connection c = connections.open()) {
            // Generated identity must be available and mapped before committing creation.
            c.setAutoCommit(false);
            try {
                Customer created;
                try (PreparedStatement p = c.prepareStatement("INSERT INTO customer(full_name, phone, email, city, address, notes) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    bind(p, details);
                    if (executeWrite(p) != 1) throw storage();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) throw storage();
                        created = new Customer(keys.getInt(1), details);
                    }
                }
                c.commit();
                return created;
            } catch (SQLException | RuntimeException failure) { throw rollback(c, failure); }
        } catch (SQLException failure) { throw storage(); }
    }

    @Override public Optional<Customer> update(int id, Customer.Details details) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                boolean exists;
                try (PreparedStatement p = c.prepareStatement("SELECT customer_id FROM customer WHERE customer_id=? FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet r = p.executeQuery()) { exists = r.next(); }
                }
                if (!exists) { c.rollback(); return Optional.empty(); }
                Customer updated = new Customer(id, details);
                try (PreparedStatement p = c.prepareStatement("UPDATE customer SET full_name=?, phone=?, email=?, city=?, address=?, notes=? WHERE customer_id=?")) {
                    bind(p, details); p.setInt(7, id);
                    int count = executeWrite(p);
                    // Existence is locked: both matched-row (1) and unchanged-row (0) modes succeed.
                    if (count != 0 && count != 1) throw storage();
                }
                c.commit();
                return Optional.of(updated);
            } catch (SQLException | RuntimeException failure) { throw rollback(c, failure); }
        } catch (SQLException failure) { throw storage(); }
    }

    private void bind(PreparedStatement p, Customer.Details d) throws SQLException {
        p.setString(1, d.fullName()); p.setString(2, d.phone());
        String[] optional = {d.email(), d.city(), d.address(), d.notes()};
        for (int i = 0; i < optional.length; i++) {
            if (optional[i] == null) p.setNull(i + 3, Types.VARCHAR);
            else p.setString(i + 3, optional[i]);
        }
    }

    private CustomerException rollback(Connection c, Exception failure) {
        try { c.rollback(); }
        catch (SQLException rollbackFailure) { return storage(); }
        if (failure instanceof CustomerException customer) return customer;
        return storage();
    }
    // ER_DUP_ENTRY contains the violated key name in the server diagnostic.
    // Inspect only a known trailing constraint token, never a substring in the duplicate value.
    // Unknown/localized diagnostics fail closed as storage failures rather than guessing.
    private int executeWrite(PreparedStatement p) throws SQLException {
        try { return p.executeUpdate(); }
        catch (SQLException failure) {
            if (failure.getErrorCode() == 1062 && failure.getMessage() != null) {
                var key = java.util.regex.Pattern.compile(
                        "\\bfor key (['`])((?:sobol_dev\\.)?(?:customer\\.)?uq_customer_(?:name|email))\\1\\s*\\z")
                        .matcher(failure.getMessage());
                if (key.find()) throw new CustomerException(key.group(2).endsWith("uq_customer_email") ? DUPLICATE_EMAIL : DUPLICATE_NAME);
            }
            throw failure;
        }
    }

    private CustomerException storage() { return new CustomerException(STORAGE_FAILURE); }
}
