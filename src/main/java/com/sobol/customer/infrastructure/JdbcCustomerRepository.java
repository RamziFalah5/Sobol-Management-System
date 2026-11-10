package com.sobol.customer.infrastructure;

import com.sobol.customer.application.CustomerException;
import com.sobol.customer.application.CustomerRepository;
import com.sobol.customer.domain.Customer;
import java.sql.*;
import com.sobol.database.PostgresErrors;
import java.util.*;
import static com.sobol.customer.application.CustomerException.Reason.*;

/** Owns fresh supplied connections. The composition root supplies the guarded development boundary. */
public final class JdbcCustomerRepository implements CustomerRepository {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    private final Connections connections;
    public JdbcCustomerRepository(Connections connections) { this.connections = Objects.requireNonNull(connections); }

    @Override public List<Customer> findAll() {
        try (Connection c = connections.open();
             PreparedStatement p = c.prepareStatement("SELECT customer_id, full_name, phone, email, city, address, notes FROM sobol.customer ORDER BY customer_id DESC");
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
                try (PreparedStatement p = c.prepareStatement("INSERT INTO sobol.customer(full_name, phone, email, city, address, notes) VALUES (?, ?, ?, ?, ?, ?)", new String[]{"customer_id"})) {
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
        } catch (CustomerException failure) { throw sanitized(failure); }
        catch (SQLException failure) { throw storage(); }
    }

    @Override public Optional<Customer> update(int id, Customer.Details details) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                boolean exists;
                try (PreparedStatement p = c.prepareStatement("SELECT customer_id FROM sobol.customer WHERE customer_id=? FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet r = p.executeQuery()) { exists = r.next(); }
                }
                if (!exists) { c.rollback(); return Optional.empty(); }
                Customer updated = new Customer(id, details);
                try (PreparedStatement p = c.prepareStatement("UPDATE sobol.customer SET full_name=?, phone=?, email=?, city=?, address=?, notes=? WHERE customer_id=?")) {
                    bind(p, details); p.setInt(7, id);
                    int count = executeWrite(p);
                    // PostgreSQL counts unchanged matched rows too; a locked existing row must match once.
                    if (count != 1) throw storage();
                }
                c.commit();
                return Optional.of(updated);
            } catch (SQLException | RuntimeException failure) { throw rollback(c, failure); }
        } catch (CustomerException failure) { throw sanitized(failure); }
        catch (SQLException failure) { throw storage(); }
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
    // Classify only an actual write with the exact PostgreSQL protocol constraint identity.
    private int executeWrite(PreparedStatement p) throws SQLException {
        try { return p.executeUpdate(); }
        catch (SQLException failure) {
            if (PostgresErrors.unique(failure, "customer", "uq_customer_email")) throw new CustomerException(DUPLICATE_EMAIL);
            throw failure;
        }
    }

    private CustomerException sanitized(CustomerException failure) {
        // Resource closure can attach raw SQL diagnostics as suppressed exceptions.
        // Recreate only the safe reason after ALL JDBC resource scopes have closed.
        return new CustomerException(failure.getSuppressed().length == 0 ? failure.reason() : STORAGE_FAILURE);
    }

    private CustomerException storage() { return new CustomerException(STORAGE_FAILURE); }
}
