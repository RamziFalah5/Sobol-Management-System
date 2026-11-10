package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WarehouseDAO {

	// ================= GET =================
	public static List<Warehouse> getWarehousesByBranch(int branchId) {

		List<Warehouse> list = new ArrayList<>();

		String sql = """
				    SELECT warehouse_id, branch_id, name, pallet_capacity
				    FROM warehouse
				    WHERE branch_id = ?
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, branchId);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				list.add(new Warehouse(rs.getInt("warehouse_id"), rs.getInt("branch_id"), rs.getString("name"),
						rs.getInt("pallet_capacity")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// ================= INSERT =================
	public static boolean insertWarehouse(int branchId, String name, int palletCapacity) {

		String insertWarehouse = """
				    INSERT INTO warehouse (branch_id, name, pallet_capacity)
				    VALUES (?, ?, ?)
				""";

		String updateBranch = """
				    UPDATE branch
				    SET warehouses_count = warehouses_count + 1,
				        total_pallet_capacity = total_pallet_capacity + ?
				    WHERE branch_id = ?
				""";

		try (Connection c = DBAuth.get()) {
			c.setAutoCommit(false);

			try (PreparedStatement ps1 = c.prepareStatement(insertWarehouse);
					PreparedStatement ps2 = c.prepareStatement(updateBranch)) {

				ps1.setInt(1, branchId);
				ps1.setString(2, name);
				ps1.setInt(3, palletCapacity);
				ps1.executeUpdate();

				ps2.setInt(1, palletCapacity);
				ps2.setInt(2, branchId);
				ps2.executeUpdate();

				c.commit();
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	// ================= DELETE =================
	public static boolean deleteWarehouse(int warehouseId, int branchId, int palletCapacity) {

		String deleteWarehouse = "DELETE FROM warehouse WHERE warehouse_id = ?";

		String updateBranch = """
				    UPDATE branch
				    SET warehouses_count = warehouses_count - 1,
				        total_pallet_capacity = total_pallet_capacity - ?
				    WHERE branch_id = ?
				""";

		try (Connection c = DBAuth.get()) {
			c.setAutoCommit(false);

			try (PreparedStatement ps1 = c.prepareStatement(deleteWarehouse);
					PreparedStatement ps2 = c.prepareStatement(updateBranch)) {

				ps1.setInt(1, warehouseId);
				ps1.executeUpdate();

				ps2.setInt(1, palletCapacity);
				ps2.setInt(2, branchId);
				ps2.executeUpdate();

				c.commit();
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	// ================= UPDATE =================
	public static boolean updateWarehouse(int warehouseId, int branchId, String name, int newCapacity,
			int capacityDiff) {

		String updateWarehouse = """
				    UPDATE warehouse
				    SET name = ?, pallet_capacity = ?
				    WHERE warehouse_id = ?
				""";

		String updateBranch = """
				    UPDATE branch
				    SET total_pallet_capacity = total_pallet_capacity + ?
				    WHERE branch_id = ?
				""";

		try (Connection c = DBAuth.get()) {
			c.setAutoCommit(false);

			try (PreparedStatement ps1 = c.prepareStatement(updateWarehouse);
					PreparedStatement ps2 = c.prepareStatement(updateBranch)) {

				ps1.setString(1, name);
				ps1.setInt(2, newCapacity);
				ps1.setInt(3, warehouseId);
				ps1.executeUpdate();

				ps2.setInt(1, capacityDiff);
				ps2.setInt(2, branchId);
				ps2.executeUpdate();

				c.commit();
				return true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	// ================= DUPLICATE CHECK (ADD) =================
	public static boolean warehouseNameExists(int branchId, String name) {

		String sql = """
				    SELECT COUNT(*)
				    FROM warehouse
				    WHERE branch_id = ? AND name = ?
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, branchId);
			ps.setString(2, name);

			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1) > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// ================= DUPLICATE CHECK (EDIT) =================
	public static boolean warehouseNameExistsForEdit(int branchId, String name, int warehouseId) {

		String sql = """
				    SELECT COUNT(*)
				    FROM warehouse
				    WHERE branch_id = ?
				      AND name = ?
				      AND warehouse_id <> ?
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, branchId);
			ps.setString(2, name);
			ps.setInt(3, warehouseId);

			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1) > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
