package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportsDAO {

	public static class ReportStats {
		public int customers;
		public int suppliers;
		public int products;
		public int shipments;
		public int totalPalletCapacity;
		public int totalPalletUsed;
		public String mostFullWarehouseName;
		public double mostFullWarehouseUtil;
		public String mostEmptyWarehouseName;
		public double mostEmptyWarehouseUtil;
		public int lowStockCount;
		public int normalStockCount;
		public int highStockCount;
		public int zeroStockCount;
		public int totalUnits;
		public double inventoryCostValue;
		public double inventorySellValue;
		public int imports30DaysCount;
		public double imports30DaysCost;
		public int att30DaysTotal;
		public double att30DaysRate;
		public List<KeyCountRow> paymentBreakdown = new ArrayList<>();
		public List<KeyCountRow> employeeTypeBreakdown = new ArrayList<>();

		public List<WarehouseUsageRow> topWarehousesByUtil = new ArrayList<>();
		public List<SupplierSpendRow> topSuppliers = new ArrayList<>();
		public List<ProductImportRow> topProducts = new ArrayList<>();
	}

	public static class KeyCountRow {
		public String key;
		public int count;

		public KeyCountRow(String key, int count) {
			this.key = key;
			this.count = count;
		}
	}

	public static class WarehouseUsageRow {
		public int warehouseId;
		public String warehouseName;
		public String branchName;
		public int capacity;
		public int used;
		public double utilization;

		public WarehouseUsageRow(int id, String wn, String bn, int cap, int used) {
			this.warehouseId = id;
			this.warehouseName = wn;
			this.branchName = bn;
			this.capacity = cap;
			this.used = used;
			this.utilization = (cap <= 0) ? 0 : (used * 1.0 / cap);
		}
	}

	public static class SupplierSpendRow {
		public int supplierId;
		public String name;
		public int shipmentsCount;
		public double totalCost;

		public SupplierSpendRow(int id, String name, int cnt, double total) {
			this.supplierId = id;
			this.name = name;
			this.shipmentsCount = cnt;
			this.totalCost = total;
		}

		public String getName() {
			return name;
		}

		public int getShipmentsCount() {
			return shipmentsCount;
		}

		public double getTotalCost() {
			return totalCost;
		}
	}

	public static class ProductImportRow {
		public int productId;
		public String name;
		public int totalQty;
		public double totalCost;

		public ProductImportRow(int id, String name, int qty, double cost) {
			this.productId = id;
			this.name = name;
			this.totalQty = qty;
			this.totalCost = cost;
		}

		public String getName() {
			return name;
		}

		public int getTotalQty() {
			return totalQty;
		}

		public double getTotalCost() {
			return totalCost;
		}
	}

	public static ReportStats loadStats() {

		ReportStats s = new ReportStats();

		try (Connection c = DBAuth.get()) {

			s.customers = count(c, "customer");
			s.suppliers = count(c, "supplier");
			s.products = count(c, "product");
			s.shipments = count(c, "import_shipment");

			loadInventoryValues(c, s);
			loadStockBuckets(c, s);

			List<WarehouseUsageRow> allWarehouses = loadWarehouseUsage(c, s);
			s.topWarehousesByUtil = topWarehouses(allWarehouses, 8);

			loadTopSuppliers(c, s);
			loadTopImportedProducts(c, s);

			loadPaymentBreakdown(c, s);
			loadEmployeeTypeBreakdown(c, s);

			loadImports30Days(c, s);
			loadAttendance30Days(c, s);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return s;
	}

	private static int count(Connection c, String table) throws Exception {
		String sql = "select count(*) from " + table;
		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			rs.next();
			return rs.getInt(1);
		}
	}

	private static void loadInventoryValues(Connection c, ReportStats s) throws Exception {
		String sql = """
				select coalesce(sum(quantity),0) as units,
				       coalesce(sum(quantity * cost_price),0) as cost_value,
				       coalesce(sum(quantity * selling_price),0) as sell_value
				from product
				""";
		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				s.totalUnits = rs.getInt("units");
				s.inventoryCostValue = rs.getDouble("cost_value");
				s.inventorySellValue = rs.getDouble("sell_value");
			}
		}
	}

	private static void loadStockBuckets(Connection c, ReportStats s) throws Exception {

		String low = "select count(*) from product where quantity < min_quantity";
		String normal = "select count(*) from product where quantity >= min_quantity and quantity <= max_quantity";
		String high = "select count(*) from product where quantity > max_quantity";
		String zero = "select count(*) from product where quantity = 0";

		s.lowStockCount = oneInt(c, low);
		s.normalStockCount = oneInt(c, normal);
		s.highStockCount = oneInt(c, high);
		s.zeroStockCount = oneInt(c, zero);
	}

	private static int oneInt(Connection c, String sql) throws Exception {
		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			rs.next();
			return rs.getInt(1);
		}
	}

	private static List<WarehouseUsageRow> loadWarehouseUsage(Connection c, ReportStats s) throws Exception {

		String sql = """
				select w.warehouse_id,
				       w.name as warehouse_name,
				       b.name as branch_name,
				       w.pallet_capacity as cap,
				       coalesce(sum(ceil(p.quantity * 1.0 / nullif(p.cartons_per_pallet,0))),0) as used
				from warehouse w
				join branch b on b.branch_id = w.branch_id
				left join product p on p.warehouse_id = w.warehouse_id
				group by w.warehouse_id, w.name, b.name, w.pallet_capacity
				""";

		List<WarehouseUsageRow> rows = new ArrayList<>();

		int totalCap = 0;
		int totalUsed = 0;

		WarehouseUsageRow mostFull = null;
		WarehouseUsageRow mostEmpty = null;

		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				int id = rs.getInt("warehouse_id");
				String wn = rs.getString("warehouse_name");
				String bn = rs.getString("branch_name");
				int cap = rs.getInt("cap");
				int used = (int) Math.round(rs.getDouble("used"));

				WarehouseUsageRow r = new WarehouseUsageRow(id, wn, bn, cap, used);
				rows.add(r);

				totalCap += cap;
				totalUsed += used;

				if (mostFull == null || r.utilization > mostFull.utilization)
					mostFull = r;
				if (mostEmpty == null || r.utilization < mostEmpty.utilization)
					mostEmpty = r;
			}
		}

		s.totalPalletCapacity = totalCap;
		s.totalPalletUsed = totalUsed;

		if (mostFull != null) {
			s.mostFullWarehouseName = mostFull.branchName + " - " + mostFull.warehouseName;
			s.mostFullWarehouseUtil = mostFull.utilization;
		}
		if (mostEmpty != null) {
			s.mostEmptyWarehouseName = mostEmpty.branchName + " - " + mostEmpty.warehouseName;
			s.mostEmptyWarehouseUtil = mostEmpty.utilization;
		}

		return rows;
	}

	private static List<WarehouseUsageRow> topWarehouses(List<WarehouseUsageRow> all, int limit) {
		all.sort((a, b) -> Double.compare(b.utilization, a.utilization));
		return all.subList(0, Math.min(limit, all.size()));
	}

	private static void loadTopSuppliers(Connection c, ReportStats s) throws Exception {

		String sql = """
				select sup.supplier_id,
				       sup.name,
				       count(sh.shipment_id) as cnt,
				       coalesce(sum(sh.total_cost),0) as total
				from supplier sup
				left join import_shipment sh on sh.supplier_id = sup.supplier_id
				group by sup.supplier_id, sup.name
				order by total desc
				limit 8
				""";

		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				s.topSuppliers.add(new SupplierSpendRow(rs.getInt("supplier_id"), rs.getString("name"),
						rs.getInt("cnt"), rs.getDouble("total")));
			}
		}
	}

	private static void loadTopImportedProducts(Connection c, ReportStats s) throws Exception {

		String sql = """
				select p.product_id,
				       p.name,
				       coalesce(sum(sh.quantity),0) as qty,
				       coalesce(sum(sh.total_cost),0) as total
				from import_shipment sh
				join product p on p.product_id = sh.product_id
				group by p.product_id, p.name
				order by qty desc
				limit 8
				""";

		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				s.topProducts.add(new ProductImportRow(rs.getInt("product_id"), rs.getString("name"), rs.getInt("qty"),
						rs.getDouble("total")));
			}
		}
	}

	private static void loadPaymentBreakdown(Connection c, ReportStats s) throws Exception {

		String sql = """
				select payment_type, count(*) as cnt
				from import_shipment
				group by payment_type
				""";

		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				s.paymentBreakdown.add(new KeyCountRow(rs.getString("payment_type"), rs.getInt("cnt")));
			}
		}
	}

	private static void loadEmployeeTypeBreakdown(Connection c, ReportStats s) throws Exception {

		String sql = """
				select employee_type, count(*) as cnt
				from employee
				group by employee_type
				""";

		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				s.employeeTypeBreakdown.add(new KeyCountRow(rs.getString("employee_type"), rs.getInt("cnt")));
			}
		}
	}

	private static void loadImports30Days(Connection c, ReportStats s) throws Exception {

		String sql = """
				select count(*) as cnt,
				       coalesce(sum(total_cost),0) as total
				from import_shipment
				where shipment_date >= (curdate() - interval 30 day)
				""";

		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				s.imports30DaysCount = rs.getInt("cnt");
				s.imports30DaysCost = rs.getDouble("total");
			}
		}
	}

	private static void loadAttendance30Days(Connection c, ReportStats s) throws Exception {

		String sql = """
				select count(*) as total,
				       coalesce(sum(present),0) as present_cnt
				from attendance
				where work_date >= (curdate() - interval 30 day)
				""";

		try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				int total = rs.getInt("total");
				int present = rs.getInt("present_cnt");
				s.att30DaysTotal = total;
				s.att30DaysRate = (total <= 0) ? 0 : (present * 1.0 / total);
			}
		}
	}
}
