package com.sobol.supplier.infrastructure;

import com.sobol.supplier.application.SupplierException;
import com.sobol.supplier.application.SupplierRepository;
import com.sobol.supplier.domain.Supplier;
import java.sql.*;
import java.util.*;
import static com.sobol.supplier.application.SupplierException.Reason.*;

/** Owns fresh supplied connections. The composition root supplies the guarded development boundary. */
public final class JdbcSupplierRepository implements SupplierRepository {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    private final Connections connections;
    public JdbcSupplierRepository(Connections connections) { this.connections = Objects.requireNonNull(connections); }

    @Override public List<Supplier> findAll() {
        try (Connection c = connections.open();
             PreparedStatement p = c.prepareStatement("SELECT supplier_id, name, phone, email, city, address, contact_person, notes FROM supplier ORDER BY supplier_id DESC");
             ResultSet r = p.executeQuery()) {
            List<Supplier> rows = new ArrayList<>();
            while (r.next()) rows.add(new Supplier(r.getInt("supplier_id"), new Supplier.Details(
                    r.getString("name"), r.getString("phone"), r.getString("email"), r.getString("city"),
                    r.getString("address"), r.getString("contact_person"), r.getString("notes"))));
            return List.copyOf(rows);
        } catch (SQLException | IllegalArgumentException failure) { throw storage(); }
    }

    @Override public Supplier insert(Supplier.Details details) {
        try (Connection c = connections.open()) {
            // Generated identity must be available and mapped before committing creation.
            c.setAutoCommit(false);
            try {
                Supplier created;
                try (PreparedStatement p = c.prepareStatement("INSERT INTO supplier(name, phone, email, city, address, contact_person, notes) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    bind(p, details);
                    if (p.executeUpdate() != 1) throw storage();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) throw storage();
                        created = new Supplier(keys.getInt(1), details);
                    }
                }
                c.commit();
                return created;
            } catch (SQLException | RuntimeException failure) { throw rollback(c, failure); }
        } catch (SQLException failure) { throw storage(); }
    }

    @Override public Optional<Supplier> update(int id, Supplier.Details details) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                boolean exists;
                try (PreparedStatement p = c.prepareStatement("SELECT supplier_id FROM supplier WHERE supplier_id=? FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet r = p.executeQuery()) { exists = r.next(); }
                }
                if (!exists) { c.rollback(); return Optional.empty(); }
                Supplier updated = new Supplier(id, details);
                try (PreparedStatement p = c.prepareStatement("UPDATE supplier SET name=?, phone=?, email=?, city=?, address=?, contact_person=?, notes=? WHERE supplier_id=?")) {
                    bind(p, details); p.setInt(8, id);
                    int count = p.executeUpdate();
                    // Existence is locked: both matched-row (1) and unchanged-row (0) modes succeed.
                    if (count != 0 && count != 1) throw storage();
                }
                c.commit();
                return Optional.of(updated);
            } catch (SQLException | RuntimeException failure) { throw rollback(c, failure); }
        } catch (SQLException failure) { throw storage(); }
    }

    private void bind(PreparedStatement p, Supplier.Details d) throws SQLException {
        p.setString(1, d.name()); p.setString(2, d.phone());
        String[] optional = {d.email(), d.city(), d.address(), d.contactPerson(), d.notes()};
        for (int i = 0; i < optional.length; i++) {
            if (optional[i] == null) p.setNull(i + 3, Types.VARCHAR);
            else p.setString(i + 3, optional[i]);
        }
    }

    private SupplierException rollback(Connection c, Exception failure) {
        try { c.rollback(); }
        catch (SQLException rollbackFailure) { return storage(); }
        if (failure instanceof SQLException sql && sql.getErrorCode() == 1062) return new SupplierException(DUPLICATE_NAME);
        return storage();
    }
    private SupplierException storage() { return new SupplierException(STORAGE_FAILURE); }
}
