package com.sobol.category.infrastructure;

import com.sobol.category.application.CategoryException;
import com.sobol.category.application.CategoryRepository;
import com.sobol.category.domain.Category;
import java.sql.*;
import java.util.*;
import static com.sobol.category.application.CategoryException.Reason.*;

/** Owns each supplied connection/transaction. Production wiring supplies the existing guarded DBAuth. */
public final class JdbcCategoryRepository implements CategoryRepository {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    private final Connections connections;

    public JdbcCategoryRepository(Connections connections) { this.connections = Objects.requireNonNull(connections); }

    @Override public List<Category> findAll() {
        try (Connection c = connections.open();
             PreparedStatement p = c.prepareStatement("SELECT category_id, name, description FROM category");
             ResultSet r = p.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (r.next()) categories.add(new Category(r.getInt("category_id"), r.getString("name"), r.getString("description")));
            return List.copyOf(categories);
        } catch (SQLException failure) { throw storage(); }
        catch (IllegalArgumentException failure) { throw new CategoryException(STORAGE_FAILURE); }
    }

    @Override public Category insert(String name, String description) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                Category created;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO category(name, description) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, name); p.setString(2, description);
                    if (executeWrite(p) != 1) throw new CategoryException(STORAGE_FAILURE);
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) throw new CategoryException(STORAGE_FAILURE);
                        created = new Category(keys.getInt(1), name, description);
                    }
                }
                c.commit();
                return created;
            } catch (SQLException | RuntimeException failure) { throw rollback(c, failure); }
        } catch (CategoryException failure) { throw sanitized(failure); }
        catch (SQLException failure) { throw storage(); }
    }

    @Override public Optional<Category> update(int id, String name, String description) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                // A locking existence read distinguishes missing from an unchanged-row count of zero.
                // The row cannot be deleted between this check and UPDATE within this transaction.
                boolean exists;
                try (PreparedStatement p = c.prepareStatement("SELECT category_id FROM category WHERE category_id=? FOR UPDATE")) {
                    p.setInt(1, id);
                    try (ResultSet r = p.executeQuery()) { exists = r.next(); }
                }
                if (!exists) {
                    c.rollback();
                    return Optional.empty();
                }
                Category updated = new Category(id, name, description);
                try (PreparedStatement p = c.prepareStatement("UPDATE category SET name=?, description=? WHERE category_id=?")) {
                    p.setString(1, updated.name()); p.setString(2, updated.description()); p.setInt(3, id);
                    int rows = executeWrite(p);
                    // Connector/J may report matched rows (1) or changed rows (0 for an unchanged row).
                    if (rows != 0 && rows != 1) throw new CategoryException(STORAGE_FAILURE);
                }
                c.commit();
                return Optional.of(updated);
            } catch (SQLException | RuntimeException failure) { throw rollback(c, failure); }
        } catch (CategoryException failure) { throw sanitized(failure); }
        catch (SQLException failure) { throw storage(); }
    }

    private CategoryException rollback(Connection c, Exception failure) {
        try { c.rollback(); }
        catch (SQLException rollbackFailure) { return storage(); }
        if (failure instanceof CategoryException category) return category;
        return new CategoryException(STORAGE_FAILURE);
    }

    private int executeWrite(PreparedStatement p) throws SQLException {
        try { return p.executeUpdate(); }
        catch (SQLException failure) {
            // Only the actual INSERT/UPDATE may report a name conflict.
            // Commit/read/close failures remain unconfirmed storage failures.
            if (failure.getErrorCode() == 1062) throw new CategoryException(DUPLICATE_NAME);
            throw failure;
        }
    }

    private CategoryException sanitized(CategoryException failure) {
        // Recreate the safe outcome after JDBC scopes close; never retain suppressed driver diagnostics.
        return new CategoryException(failure.getSuppressed().length == 0 ? failure.reason() : STORAGE_FAILURE);
    }

    private CategoryException storage() { return new CategoryException(STORAGE_FAILURE); }
}
