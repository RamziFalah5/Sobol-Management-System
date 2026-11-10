package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

	public static List<Category> getAllCategories() {
		List<Category> list = new ArrayList<>();
		String sql = "SELECT category_id, name, description FROM category";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new Category(rs.getInt("category_id"), rs.getString("name"), rs.getString("description")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public static boolean insertCategory(String name, String description) {
		String sql = "INSERT INTO category (name, description) VALUES (?, ?)";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setString(2, description);
			ps.executeUpdate();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean updateCategory(int id, String name, String description) {
		String sql = "UPDATE category SET name=?, description=? WHERE category_id=?";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setString(2, description);
			ps.setInt(3, id);
			ps.executeUpdate();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean deleteCategory(int id) {
		String sql = "DELETE FROM category WHERE category_id=?";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, id);
			return ps.executeUpdate() > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean categoryNameExists(String name) {
		String sql = "SELECT 1 FROM category WHERE name = ? LIMIT 1";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			return rs.next();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static int getNextCategoryId() {
		String sql = "SELECT AUTO_INCREMENT " + "FROM information_schema.TABLES " + "WHERE TABLE_SCHEMA = DATABASE() "
				+ "AND TABLE_NAME = 'category'";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			if (rs.next())
				return rs.getInt(1);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1;
	}

	public static boolean categoryNameExistsForEdit(String name, int currentId) {
		String sql = "SELECT 1 FROM category WHERE name = ? AND category_id <> ? LIMIT 1";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setInt(2, currentId);
			ResultSet rs = ps.executeQuery();
			return rs.next();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
