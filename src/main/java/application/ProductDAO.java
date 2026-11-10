package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

	public static List<Product> getProductsByCategory(int categoryId) {
		List<Product> list = new ArrayList<>();

		String sql = """
				SELECT p.product_id, p.category_id,
				       p.branch_id, p.warehouse_id,
				       b.name AS branch_name,
				       w.name AS warehouse_name,
				       p.name, p.description, p.base_unit,
				       p.cartons_per_pallet, p.quantity, p.min_quantity, p.max_quantity,
				       p.cost_price, p.selling_price, p.image_path
				FROM product p
				JOIN branch b ON b.branch_id = p.branch_id
				JOIN warehouse w ON w.warehouse_id = p.warehouse_id
				WHERE p.category_id=?
				ORDER BY p.product_id DESC
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, categoryId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				list.add(new Product(rs.getInt("product_id"), rs.getInt("category_id"), rs.getInt("branch_id"),
						rs.getInt("warehouse_id"), rs.getString("branch_name"), rs.getString("warehouse_name"),
						rs.getString("name"), rs.getString("description"), rs.getString("base_unit"),
						rs.getInt("cartons_per_pallet"), rs.getInt("quantity"), rs.getInt("min_quantity"),
						rs.getInt("max_quantity"), rs.getDouble("cost_price"), rs.getDouble("selling_price"),
						rs.getString("image_path")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public static boolean productNameExists(int categoryId, String name) {
		String sql = "SELECT 1 FROM product WHERE category_id=? AND name=? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, categoryId);
			ps.setString(2, name);
			return ps.executeQuery().next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean productNameExistsForEdit(int categoryId, String name, int currentProductId) {
		String sql = "SELECT 1 FROM product WHERE category_id=? AND name=? AND product_id<>? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, categoryId);
			ps.setString(2, name);
			ps.setInt(3, currentProductId);
			return ps.executeQuery().next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean canFitInWarehouse(int warehouseId, int palletsToAdd, Integer excludeProductId) {

		String sql = """
				SELECT w.pallet_capacity AS cap,
				       COALESCE(SUM(CEIL(p.quantity * 1.0 / NULLIF(p.cartons_per_pallet,0))), 0) AS used
				FROM warehouse w
				LEFT JOIN product p
				       ON p.warehouse_id = w.warehouse_id
				      AND (? IS NULL OR p.product_id <> ?)
				WHERE w.warehouse_id = ?
				GROUP BY w.pallet_capacity
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			if (excludeProductId == null) {
				ps.setNull(1, Types.INTEGER);
				ps.setNull(2, Types.INTEGER);
			} else {
				ps.setInt(1, excludeProductId);
				ps.setInt(2, excludeProductId);
			}
			ps.setInt(3, warehouseId);

			ResultSet rs = ps.executeQuery();
			if (!rs.next())
				return false;

			int cap = rs.getInt("cap");
			double used = rs.getDouble("used");

			return (used + palletsToAdd) <= cap;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean insertProduct(ProductFormData d) {

		String sql = """
				INSERT INTO product
				(category_id, branch_id, warehouse_id,
				 name, description, base_unit,
				 cartons_per_pallet, quantity,
				 min_quantity, max_quantity,
				 cost_price, selling_price, image_path)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, d.categoryId);
			ps.setInt(2, d.branchId);
			ps.setInt(3, d.warehouseId);

			ps.setString(4, d.name);
			ps.setString(5, d.description);
			ps.setString(6, d.baseUnit);

			ps.setInt(7, d.cartonsPerPallet);
			ps.setInt(8, d.quantity);

			ps.setInt(9, d.minQuantity);
			ps.setInt(10, d.maxQuantity);

			ps.setDouble(11, d.costPrice);
			ps.setDouble(12, d.sellingPrice);

			ps.setString(13, d.imagePath);

			return ps.executeUpdate() > 0;

		} catch (SQLException e) {
			if (e.getErrorCode() == 1062)
				return false;
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean updateProduct(int productId, ProductFormData d) {

		String sql = """
				UPDATE product
				SET branch_id=?, warehouse_id=?,
				    name=?, description=?, base_unit=?,
				    cartons_per_pallet=?, quantity=?,
				    min_quantity=?, max_quantity=?,
				    cost_price=?, selling_price=?, image_path=?
				WHERE product_id=?
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, d.branchId);
			ps.setInt(2, d.warehouseId);

			ps.setString(3, d.name);
			ps.setString(4, d.description);
			ps.setString(5, d.baseUnit);

			ps.setInt(6, d.cartonsPerPallet);
			ps.setInt(7, d.quantity);

			ps.setInt(8, d.minQuantity);
			ps.setInt(9, d.maxQuantity);

			ps.setDouble(10, d.costPrice);
			ps.setDouble(11, d.sellingPrice);

			ps.setString(12, d.imagePath);

			ps.setInt(13, productId);

			return ps.executeUpdate() > 0;

		} catch (SQLException e) {
			if (e.getErrorCode() == 1062)
				return false;
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean deleteProduct(int productId) {
		String sql = "DELETE FROM product WHERE product_id=?";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, productId);
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static class ProductFormData {
		public int categoryId;
		public int branchId;
		public int warehouseId;
		public String name;
		public String description;
		public String baseUnit;
		public int cartonsPerPallet;
		public int quantity;
		public int minQuantity;
		public int maxQuantity;
		public double costPrice;
		public double sellingPrice;
		public String imagePath;
	}
}
