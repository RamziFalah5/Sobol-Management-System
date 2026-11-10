package com.sobol.auth.infrastructure;

import com.sobol.auth.application.*;
import com.sobol.auth.domain.Account;
import com.sobol.database.PostgresErrors;
import java.sql.*;
import java.util.*;
import static com.sobol.auth.application.AuthenticationException.Reason.*;

/** Only pending Member registration and credential/security-state reads. Owns every connection. */
public final class JdbcAccountRepository implements AccountRepository {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    private final Connections connections;
    private static final String COLUMNS = "account_id, login, display_name, status, role, auth_version";
    public JdbcAccountRepository(Connections connections) { this.connections = Objects.requireNonNull(connections); }
    @Override public Optional<Credential> findCredential(String login) {
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(
                "SELECT " + COLUMNS + ", password_hash FROM sobol.account WHERE login=?")) {
            p.setString(1, login);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return Optional.empty();
                Credential value = new Credential(map(r), r.getString("password_hash"));
                if (r.next()) throw storage();
                return Optional.of(value);
            }
        } catch (SQLException | RuntimeException failure) { throw storage(); }
    }
    @Override public Optional<Account> findById(long id) {
        try (Connection c = connections.open(); PreparedStatement p = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM sobol.account WHERE account_id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return Optional.empty();
                Account value = map(r); if (r.next()) throw storage(); return Optional.of(value);
            }
        } catch (SQLException | RuntimeException failure) { throw storage(); }
    }
    @Override public Account createPending(String login, String displayName, String phone, String encoded) {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                Account value;
                try (PreparedStatement p = c.prepareStatement("""
                        INSERT INTO sobol.account(login, display_name, phone, password_hash)
                        VALUES (?, ?, ?, ?) RETURNING account_id, login, display_name, status, role, auth_version
                        """)) {
                    p.setString(1, login); p.setString(2, displayName);
                    if (phone == null) p.setNull(3, Types.VARCHAR); else p.setString(3, phone);
                    p.setString(4, encoded);
                    try (ResultSet r = write(p)) {
                        if (!r.next()) throw storage(); value = map(r);
                        if (value.status() != Account.Status.PENDING || value.role() != Account.Role.MEMBER || r.next()) throw storage();
                    }
                }
                c.commit(); return value;
            } catch (SQLException | RuntimeException failure) {
                try { c.rollback(); } catch (SQLException rollbackFailure) { throw storage(); }
                if (failure instanceof AuthenticationException safe) throw safe;
                throw storage();
            }
        } catch (AuthenticationException failure) {
            throw new AuthenticationException(failure.getSuppressed().length == 0 ? failure.reason() : STORAGE_FAILURE);
        } catch (SQLException | RuntimeException failure) { throw storage(); }
    }
    private ResultSet write(PreparedStatement p) throws SQLException {
        try { return p.executeQuery(); }
        catch (SQLException failure) {
            if (PostgresErrors.unique(failure, "account", "uq_account_login")) throw new AuthenticationException(DUPLICATE_LOGIN);
            throw failure;
        }
    }
    private Account map(ResultSet r) throws SQLException {
        return new Account(r.getLong("account_id"), r.getString("login"), r.getString("display_name"),
                Account.Status.valueOf(r.getString("status")), Account.Role.valueOf(r.getString("role")), r.getLong("auth_version"));
    }
    private AuthenticationException storage() { return new AuthenticationException(STORAGE_FAILURE); }
}
