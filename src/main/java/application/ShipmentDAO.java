package application;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShipmentDAO {

	//list all shipment
	public static List<Shipment> getAllShipments() {

		List<Shipment> list = new ArrayList<>();

		String sql = """
				    SELECT s.shipment_id,
				           s.product_id, s.supplier_id, s.branch_id, s.warehouse_id,
				           p.name AS product_name,
				           sup.name AS supplier_name,
				           b.name AS branch_name,
				           w.name AS warehouse_name,
				           s.quantity, s.unit_cost, s.total_cost,
				           s.shipment_date, s.payment_type, s.shipping_method,
				           s.reference_no, s.notes
				    FROM import_shipment s
				    JOIN product p ON p.product_id = s.product_id
				    JOIN supplier sup ON sup.supplier_id = s.supplier_id
				    JOIN branch b ON b.branch_id = s.branch_id
				    JOIN warehouse w ON w.warehouse_id = s.warehouse_id
				    ORDER BY s.shipment_id DESC
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				LocalDate date = rs.getDate("shipment_date").toLocalDate();

				list.add(new Shipment(rs.getInt("shipment_id"), rs.getInt("product_id"), rs.getInt("supplier_id"),
						rs.getInt("branch_id"), rs.getInt("warehouse_id"), rs.getString("product_name"),
						rs.getString("supplier_name"), rs.getString("branch_name"), rs.getString("warehouse_name"),
						rs.getInt("quantity"), rs.getDouble("unit_cost"), rs.getDouble("total_cost"), date,
						rs.getString("payment_type"), rs.getString("shipping_method"), rs.getString("reference_no"),
						rs.getString("notes")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	//all product
	public static List<ProductPickRow> getAllProductsForShipment() {

		List<ProductPickRow> list = new ArrayList<>();

		String sql = """
				    SELECT p.product_id,
				           p.image_path,
				           p.name AS product_name,
				           c.name AS category_name,
				           p.branch_id, p.warehouse_id,
				           b.name AS branch_name,
				           w.name AS warehouse_name,
				           p.cartons_per_pallet,
				           p.quantity,
				           p.cost_price
				    FROM product p
				    JOIN category c ON c.category_id = p.category_id
				    JOIN branch b ON b.branch_id = p.branch_id
				    JOIN warehouse w ON w.warehouse_id = p.warehouse_id
				    ORDER BY p.product_id DESC
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new ProductPickRow(rs.getInt("product_id"), rs.getString("image_path"),
						rs.getString("product_name"), rs.getString("category_name"), rs.getInt("branch_id"),
						rs.getInt("warehouse_id"), rs.getString("branch_name"), rs.getString("warehouse_name"),
						rs.getInt("cartons_per_pallet"), rs.getInt("quantity"), rs.getDouble("cost_price")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// ================= CORE: ADD SHIPMENT (TRANSACTION) =================
	public static boolean addShipment(ShipmentFormData d) {

		// product quantity
		String qProduct = """
				    SELECT quantity, cartons_per_pallet
				    FROM product
				    WHERE product_id = ?
				""";

		// returns the warehouse pallet capacity and the number of pallets currently used n that warehouse
		String qCapacity = """
				    SELECT w.pallet_capacity AS cap,
				           COALESCE(SUM(CEIL(p.quantity * 1.0 / NULLIF(p.cartons_per_pallet,0))),0) AS used_other
				    FROM warehouse w
				    LEFT JOIN product p
				           ON p.warehouse_id = w.warehouse_id
				          AND p.product_id <> ?
				    WHERE w.warehouse_id = ?
				    GROUP BY w.pallet_capacity
				""";

		String ins = """
				    INSERT INTO import_shipment
				    (product_id, supplier_id, branch_id, warehouse_id,
				     quantity, unit_cost, total_cost,
				     shipment_date, payment_type, shipping_method, reference_no, notes)
				    VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
				""";

		// 4) update product quantity
		String updProduct = """
				    UPDATE product
				    SET branch_id = ?, warehouse_id = ?, quantity = ?
				    WHERE product_id = ?
				""";

		try (Connection c = DBAuth.get()) {
			c.setAutoCommit(false);

			int currentQty;
			int cartons;

			try (PreparedStatement ps = c.prepareStatement(qProduct)) {
				ps.setInt(1, d.productId);
				ResultSet rs = ps.executeQuery();
				if (!rs.next()) {
					c.rollback();
					return false;
				}
				currentQty = rs.getInt("quantity");
				cartons = rs.getInt("cartons_per_pallet");
			}

			if (cartons <= 0) {
				c.rollback();
				return false;
			}

			int newQty = currentQty + d.quantity;

			int newPallets = (int) Math.ceil(newQty * 1.0 / cartons);

			int cap;
			double usedOther;

			try (PreparedStatement ps = c.prepareStatement(qCapacity)) {
				ps.setInt(1, d.productId);
				ps.setInt(2, d.warehouseId);
				ResultSet rs = ps.executeQuery();
				if (!rs.next()) {
					c.rollback();
					return false;
				}
				cap = rs.getInt("cap");
				usedOther = rs.getDouble("used_other");
			}

			if (usedOther + newPallets > cap) {
				c.rollback();
				return false;
			}

			// insert shipment
			try (PreparedStatement ps = c.prepareStatement(ins)) {

				ps.setInt(1, d.productId);
				ps.setInt(2, d.supplierId);
				ps.setInt(3, d.branchId);
				ps.setInt(4, d.warehouseId);

				ps.setInt(5, d.quantity);
				ps.setDouble(6, d.unitCost);
				ps.setDouble(7, d.totalCost);

				ps.setDate(8, Date.valueOf(d.shipmentDate));
				ps.setString(9, d.paymentType);
				ps.setString(10, d.shippingMethod);

				if (d.referenceNo == null || d.referenceNo.isBlank())
					ps.setNull(11, Types.VARCHAR);
				else
					ps.setString(11, d.referenceNo);

				if (d.notes == null || d.notes.isBlank())
					ps.setNull(12, Types.VARCHAR);
				else
					ps.setString(12, d.notes);

				ps.executeUpdate();
			}

			// update product
			try (PreparedStatement ps = c.prepareStatement(updProduct)) {
				ps.setInt(1, d.branchId);
				ps.setInt(2, d.warehouseId);
				ps.setInt(3, newQty);
				ps.setInt(4, d.productId);
				ps.executeUpdate();
			}

			c.commit();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public static class ShipmentFormData {
		public int productId;
		public int supplierId;
		public int branchId;
		public int warehouseId;

		public int quantity;
		public double unitCost;
		public double totalCost;

		public LocalDate shipmentDate;

		public String paymentType;
		public String shippingMethod;
		public String referenceNo;
		public String notes;
	}

	// ================= Shipments By Supplier =================
	public static List<Shipment> getShipmentsBySupplier(int supplierId) {

		List<Shipment> list = new ArrayList<>();

		String sql = """
			    SELECT s.shipment_id,
			           s.product_id, s.supplier_id, s.branch_id, s.warehouse_id,
			           p.name AS product_name,
			           sup.name AS supplier_name,
			           b.name AS branch_name,
			           w.name AS warehouse_name,
			           s.quantity, s.unit_cost, s.total_cost,
			           s.shipment_date, s.payment_type, s.shipping_method,
			           s.reference_no, s.notes
			    FROM import_shipment s
			    JOIN product p ON p.product_id = s.product_id
			    JOIN supplier sup ON sup.supplier_id = s.supplier_id
			    JOIN branch b ON b.branch_id = s.branch_id
			    JOIN warehouse w ON w.warehouse_id = s.warehouse_id
			    WHERE s.supplier_id = ?
			    ORDER BY s.shipment_id DESC
			""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, supplierId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {

					LocalDate date = rs.getDate("shipment_date").toLocalDate();

					list.add(new Shipment(
							rs.getInt("shipment_id"),
							rs.getInt("product_id"),
							rs.getInt("supplier_id"),
							rs.getInt("branch_id"),
							rs.getInt("warehouse_id"),
							rs.getString("product_name"),
							rs.getString("supplier_name"),
							rs.getString("branch_name"),
							rs.getString("warehouse_name"),
							rs.getInt("quantity"),
							rs.getDouble("unit_cost"),
							rs.getDouble("total_cost"),
							date,
							rs.getString("payment_type"),
							rs.getString("shipping_method"),
							rs.getString("reference_no"),
							rs.getString("notes")
					));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// ================= Total Cost By Supplier =================
	public static double getTotalCostBySupplier(int supplierId) {

		String sql = """
			    SELECT COALESCE(SUM(total_cost), 0) AS total
			    FROM import_shipment
			    WHERE supplier_id = ?
			""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, supplierId);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getDouble("total");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0.0;
	}


}
