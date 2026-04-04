package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardDAO {

	public static DashboardStats getStats() {

		DashboardStats s = new DashboardStats();

		try (Connection c = DBAuth.get()) {
			loadCounts(c, s);
			loadPallets(c, s);
			loadCosts(c, s);
			loadLast30Days(c, s);
			loadStockStatus(c, s);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return s;
	}

	// counts
	private static void loadCounts(Connection c, DashboardStats s) throws Exception {
		String sql = """
					select
					  (select count(*) from product)   as products,
					  (select count(*) from category)  as categories,
					  (select count(*) from supplier)  as suppliers,
					  (select count(*) from customer)  as customers,
					  (select count(*) from employee)  as employees,
					  (select count(*) from vehicle)   as vehicles,
					  (select count(*) from branch)    as branches,
					  (select count(*) from warehouse) as warehouses
				""";

		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				s.products = rs.getInt("products");
				s.categories = rs.getInt("categories");
				s.suppliers = rs.getInt("suppliers");
				s.customers = rs.getInt("customers");
				s.employees = rs.getInt("employees");
				s.vehicles = rs.getInt("vehicles");
				s.branches = rs.getInt("branches");
				s.warehouses = rs.getInt("warehouses");
			}
		}
	}

	// pallets
	private static void loadPallets(Connection c, DashboardStats s) throws Exception {

		String capSql = """
					select coalesce(sum(pallet_capacity),0)
					from warehouse
				""";

		String usedSql = """
					select coalesce(
					  sum(ceil(quantity * 1.0 / nullif(cartons_per_pallet,0))),0
					)
					from product
				""";

		try (PreparedStatement ps = c.prepareStatement(capSql); ResultSet rs = ps.executeQuery()) {
			if (rs.next())
				s.totalPalletCapacity = rs.getInt(1);
		}

		try (PreparedStatement ps = c.prepareStatement(usedSql); ResultSet rs = ps.executeQuery()) {
			if (rs.next())
				s.usedPallets = rs.getInt(1);
		}
	}

	// costs
	private static void loadCosts(Connection c, DashboardStats s) throws Exception {

		String inventorySql = """
					select coalesce(sum(quantity * cost_price),0)
					from product
				""";

		String importsSql = """
					select coalesce(sum(total_cost),0)
					from import_shipment
				""";

		try (PreparedStatement ps = c.prepareStatement(inventorySql); ResultSet rs = ps.executeQuery()) {
			if (rs.next())
				s.inventoryCost = rs.getDouble(1);
		}

		try (PreparedStatement ps = c.prepareStatement(importsSql); ResultSet rs = ps.executeQuery()) {
			if (rs.next())
				s.totalImportsCost = rs.getDouble(1);
		}
	}

	// last 30 daye
	private static void loadLast30Days(Connection c, DashboardStats s) throws Exception {

		String qtySql = """
					select coalesce(sum(quantity),0)
					from import_shipment
					where shipment_date >= curdate() - interval 30 day
				""";

		String cntSql = """
					select count(*)
					from import_shipment
					where shipment_date >= curdate() - interval 30 day
				""";

		try (PreparedStatement ps = c.prepareStatement(qtySql); ResultSet rs = ps.executeQuery()) {
			if (rs.next())
				s.shippedQty30 = rs.getInt(1);
		}

		try (PreparedStatement ps = c.prepareStatement(cntSql); ResultSet rs = ps.executeQuery()) {
			if (rs.next())
				s.shipments30 = rs.getInt(1);
		}
	}

	// stock status
	private static void loadStockStatus(Connection c, DashboardStats s) throws Exception {

		String sql = """
					select
					  sum(case when quantity < min_quantity then 1 else 0 end) as low_stock,
					  sum(case when quantity > max_quantity then 1 else 0 end) as high_stock,
					  sum(case when quantity between min_quantity and max_quantity then 1 else 0 end) as normal_stock
					from product
				""";

		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			if (rs.next()) {
				s.lowStock = rs.getInt("low_stock");
				s.highStock = rs.getInt("high_stock");
				s.normalStock = rs.getInt("normal_stock");
			}
		}
	}
}