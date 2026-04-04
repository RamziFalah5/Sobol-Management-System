package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BranchDAO {

	public static List<Branch> getAllBranches() {

		List<Branch> list = new ArrayList<>();

		String sql = """
				    SELECT branch_id,
				           name,
				           city,
				           phone,
				           warehouses_count,
				           total_pallet_capacity
				    FROM branch
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				list.add(new Branch(rs.getInt("branch_id"), rs.getString("name"), rs.getString("city"),
						rs.getString("phone"), rs.getInt("warehouses_count"), rs.getInt("total_pallet_capacity") // ✅
				));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	//  add 
	public static boolean branchNameExists(String name) {

		String sql = """
				    SELECT 1
				    FROM branch
				    WHERE name = ?
				    LIMIT 1
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			return ps.executeQuery().next();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	// edit
	public static boolean branchNameExistsForEdit(String name, int branchId) {

		String sql = """
				    SELECT 1
				    FROM branch
				    WHERE name = ?
				      AND branch_id <> ?
				    LIMIT 1
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setInt(2, branchId);
			return ps.executeQuery().next();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public static boolean insertBranch(String name, String city, String phone) {

		String sql = """
				    INSERT INTO branch (name, city, phone)
				    VALUES (?, ?, ?)
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setString(2, city);
			ps.setString(3, phone);

			return ps.executeUpdate() > 0;

		} catch (SQLException e) {
			if (e.getErrorCode() == 1062) { 
				return false;
			}
			e.printStackTrace();
		}

		return false;
	}

	public static boolean updateBranch(int id, String name, String city, String phone) {

		String sql = """
				    UPDATE branch
				    SET name = ?, city = ?, phone = ?
				    WHERE branch_id = ?
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setString(2, city);
			ps.setString(3, phone);
			ps.setInt(4, id);

			return ps.executeUpdate() > 0;

		} catch (SQLException e) {
			if (e.getErrorCode() == 1062) {
				return false;
			}
			e.printStackTrace();
		}

		return false;
	}

	public static boolean deleteBranch(int id) {

		String sql = "DELETE FROM branch WHERE branch_id = ?";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, id);
			return ps.executeUpdate() > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
}
