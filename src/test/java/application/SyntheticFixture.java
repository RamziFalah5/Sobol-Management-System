package application;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Semantic values are fixed; a per-case namespace isolates generated IDs and unique keys. */
final class SyntheticFixture implements AutoCloseable {
    static final LocalDate DATE = LocalDate.of(2026, 1, 15);
    final Connection c;
    final String prefix = "SOBOL_IT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    final List<Integer> branches = new ArrayList<>();
    final List<Integer> vehicles = new ArrayList<>();
    int branch, category, supplier, customer, warehouse, emptyWarehouse, nearWarehouse;
    int normal, low, zero, nearProduct, daily, hourly, inactive, driver, representative, assignedVehicle, freeVehicle, user;

    private SyntheticFixture(Connection c) { this.c = c; }
    static SyntheticFixture create(Connection c) throws Exception {
        SyntheticFixture f = new SyntheticFixture(c);
        try { f.seed(); return f; }
        catch (Exception failure) {
            try { f.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    private void seed() throws Exception {
        branch = insert("branch", "name,city,warehouses_count,total_pallet_capacity", prefix + "_branch", "Synthetic City", 3, 113);
        branches.add(branch);
        category = insert("category", "name,description", prefix + "_category", "Artificial fixture only");
        supplier = insert("supplier", "name,phone,email", prefix + "_supplier", "0000000000", prefix + "_supplier@example.invalid");
        customer = insert("customer", "full_name,phone,email", prefix + "_customer", "0000000000", prefix + "_customer@example.invalid");
        warehouse = insert("warehouse", "branch_id,name,pallet_capacity", branch, prefix + "_main", 100);
        emptyWarehouse = insert("warehouse", "branch_id,name,pallet_capacity", branch, prefix + "_empty", 3);
        nearWarehouse = insert("warehouse", "branch_id,name,pallet_capacity", branch, prefix + "_near", 10);
        normal = product("normal", warehouse, 20, 5, 100, "1.25");
        low = product("low", warehouse, 3, 5, 100, "3.50");
        zero = product("zero", warehouse, 0, 5, 100, "2.00");
        nearProduct = product("near", nearWarehouse, 81, 5, 100, "4.00");
        assignedVehicle = vehicle("assigned"); freeVehicle = vehicle("free");
        daily = employee("daily", "GENERAL_WORKER", "DAILY", null, 1);
        hourly = employee("hourly", "GENERAL_WORKER", "HOURLY", null, 1);
        inactive = employee("inactive", "GENERAL_WORKER", "DAILY", null, 0);
        driver = employee("driver", "TRUCK_DRIVER", null, assignedVehicle, 1);
        representative = employee("representative", "SALES_REP", null, null, 1);
        insert("attendance", "employee_id,work_date,present,hours_worked", daily, DATE, 1, null);
        insert("attendance", "employee_id,work_date,present,hours_worked", hourly, DATE, 1, 6);
        insert("attendance", "employee_id,work_date,present,hours_worked", driver, DATE, 0, null);
        insert("import_shipment", "product_id,supplier_id,branch_id,warehouse_id,quantity,unit_cost,total_cost,shipment_date,reference_no",
                normal, supplier, branch, warehouse, 2, new BigDecimal("0.50"), new BigDecimal("1.00"), DATE, prefix + "_seed");
        int order = insert("sales_order", "customer_id,employee_id,order_date,total_amount", customer, representative, DATE, new BigDecimal("6.00"));
        insert("sales_order_item", "order_id,product_id,quantity,unit_price,line_total", order, normal, 2, new BigDecimal("1.25"), new BigDecimal("2.50"));
        insert("sales_order_item", "order_id,product_id,quantity,unit_price,line_total", order, low, 1, new BigDecimal("3.50"), new BigDecimal("3.50"));
        // A runtime synthetic hash is never logged, written to source or used as a database credential.
        user = insert("user_account", "name,email,phone,password_hash", prefix + "_user", prefix + "@example.invalid", "0000000000",
                PasswordUtil.sha256(prefix + "_artificial_login"));
        update("INSERT INTO user_database(user_id,db_name) VALUES(?,?)", user, prefix + "_metadata_only");
    }

    int product(String label, int wh, int quantity, int min, int max, String price) throws SQLException {
        return insert("product", "category_id,branch_id,warehouse_id,name,base_unit,cartons_per_pallet,quantity,min_quantity,max_quantity,cost_price,selling_price",
                category, branch, wh, prefix + "_" + label, "CARTON", 10, quantity, min, max, new BigDecimal("0.50"), new BigDecimal(price));
    }

    int employee(String label, String type, String mode, Integer vehicle, int active) throws SQLException {
        return insert("employee", "full_name,phone,branch_id,hire_date,employee_type,worker_mode,daily_rate,hourly_rate,driver_daily_rate,sales_daily_rate,commission_rate,vehicle_id,is_active",
                prefix + "_" + label, "0000000000", branch, DATE, type, mode,
                "DAILY".equals(mode) ? 50 : null, "HOURLY".equals(mode) ? 8 : null,
                "TRUCK_DRIVER".equals(type) ? 60 : null, "SALES_REP".equals(type) ? 45 : null,
                "SALES_REP".equals(type) ? 2 : null, vehicle, active);
    }

    int vehicle(String label) throws SQLException {
        String shortId = prefix.substring(9) + label;
        int id = insert("vehicle", "name,plate_number,vin,vehicle_type,manufacturer,model,model_year,status,capacity_pallets,max_load_kg,fuel_type,transmission,purchase_date,last_service_date,insurance_expiry",
                prefix + "_" + label, "TEST-" + shortId, "SYNTHETIC-" + shortId, "TRUCK", "Synthetic", "Fixture", 2026,
                "ACTIVE", 10, 1000, "DIESEL", "MANUAL", DATE, DATE, DATE.plusYears(1));
        vehicles.add(id); return id;
    }

    int insert(String table, String columns, Object... values) throws SQLException {
        if (!DevDatabase.TABLES.contains(table) || !columns.matches("[a-z_,]+")) throw new IllegalArgumentException();
        String slots = String.join(",", Collections.nCopies(values.length, "?"));
        try (PreparedStatement p = c.prepareStatement("INSERT INTO " + table + "(" + columns + ") VALUES(" + slots + ")", Statement.RETURN_GENERATED_KEYS)) {
            bind(p, values); p.executeUpdate();
            try (ResultSet rs = p.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Synthetic fixture key was not returned.");
                return rs.getInt(1);
            }
        }
    }

    static void bind(PreparedStatement p, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            Object v = values[i]; p.setObject(i + 1, v instanceof LocalDate date ? java.sql.Date.valueOf(date) : v);
        }
    }

    int update(String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { bind(p, values); return p.executeUpdate(); }
    }

    int integer(String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, values);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) throw new SQLException("Expected fixture query row missing.");
                return rs.getInt(1);
            }
        }
    }

    BigDecimal decimal(String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, values);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) throw new SQLException("Expected fixture amount missing.");
                return rs.getBigDecimal(1);
            }
        }
    }

    int stock(int product) throws SQLException { return integer("SELECT quantity FROM product WHERE product_id=?", product); }
    int orders() throws SQLException { return integer("SELECT COUNT(*) FROM sales_order WHERE customer_id=?", customer); }
    int shipments() throws SQLException { return integer("SELECT COUNT(*) FROM import_shipment WHERE supplier_id=?", supplier); }

    @Override public void close() throws SQLException {
        if (!c.getAutoCommit()) throw new SQLException("Cleanup refused: fixture connection has an unresolved transaction.");
        c.setAutoCommit(false);
        try {
            // Only owned root IDs and their test-created descendants; never LIKE, TRUNCATE or global reset.
            if (customer != 0) {
                update("DELETE i FROM sales_order_item i JOIN sales_order o ON o.order_id=i.order_id WHERE o.customer_id=?", customer);
                update("DELETE FROM sales_order WHERE customer_id=?", customer);
            }
            if (supplier != 0) update("DELETE FROM import_shipment WHERE supplier_id=?", supplier);
            for (int id : branches) {
                update("DELETE a FROM attendance a JOIN employee e ON e.employee_id=a.employee_id WHERE e.branch_id=?", id);
                update("DELETE FROM employee WHERE branch_id=?", id);
            }
            if (category != 0) update("DELETE FROM product WHERE category_id=?", category);
            for (int id : branches) {
                update("DELETE FROM warehouse WHERE branch_id=?", id);
                update("DELETE FROM branch WHERE branch_id=?", id);
            }
            for (int id : vehicles) update("DELETE FROM vehicle WHERE vehicle_id=?", id);
            if (user != 0) {
                update("DELETE FROM user_database WHERE user_id=?", user);
                update("DELETE FROM user_account WHERE user_id=?", user);
            }
            if (customer != 0) update("DELETE FROM customer WHERE customer_id=?", customer);
            if (supplier != 0) update("DELETE FROM supplier WHERE supplier_id=?", supplier);
            if (category != 0) update("DELETE FROM category WHERE category_id=?", category);
            c.commit();
        } catch (SQLException failure) { c.rollback(); throw failure; }
        finally { c.setAutoCommit(true); }
    }
}
