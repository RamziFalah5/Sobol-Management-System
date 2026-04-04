package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

	public static List<Customer> getAllCustomers() {
		List<Customer> list = new ArrayList<>();
		String sql = """
				SELECT customer_id, full_name, phone, email, city, address, notes
				FROM customer
				ORDER BY customer_id DESC
				""";
		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new Customer(rs.getInt("customer_id"), rs.getString("full_name"), rs.getString("phone"),
						rs.getString("email"), rs.getString("city"), rs.getString("address"), rs.getString("notes")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public static boolean nameExists(String fullName) {
		String sql = "SELECT 1 FROM customer WHERE full_name=? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			return ps.executeQuery().next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean nameExistsForEdit(String fullName, int customerId) {
		String sql = "SELECT 1 FROM customer WHERE full_name=? AND customer_id<>? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			ps.setInt(2, customerId);
			return ps.executeQuery().next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean emailExists(String email) {
		if (email == null || email.isBlank())
			return false;
		String sql = "SELECT 1 FROM customer WHERE email=? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, email);
			return ps.executeQuery().next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean emailExistsForEdit(String email, int customerId) {
		if (email == null || email.isBlank())
			return false;
		String sql = "SELECT 1 FROM customer WHERE email=? AND customer_id<>? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, email);
			ps.setInt(2, customerId);
			return ps.executeQuery().next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean insertCustomer(CustomerFormData d) {
		String sql = """
				INSERT INTO customer(full_name, phone, email, city, address, notes)
				VALUES(?,?,?,?,?,?)
				""";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, d.fullName);
			ps.setString(2, d.phone);

			if (d.email == null || d.email.isBlank())
				ps.setNull(3, Types.VARCHAR);
			else
				ps.setString(3, d.email);

			ps.setString(4, d.city);
			ps.setString(5, d.address);
			ps.setString(6, d.notes);

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

	public static boolean updateCustomer(int customerId, CustomerFormData d) {
		String sql = """
				UPDATE customer
				SET full_name=?, phone=?, email=?, city=?, address=?, notes=?
				WHERE customer_id=?
				""";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, d.fullName);
			ps.setString(2, d.phone);

			if (d.email == null || d.email.isBlank())
				ps.setNull(3, Types.VARCHAR);
			else
				ps.setString(3, d.email);

			ps.setString(4, d.city);
			ps.setString(5, d.address);
			ps.setString(6, d.notes);
			ps.setInt(7, customerId);

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

	public static class CustomerFormData {
		public String fullName;
		public String phone;
		public String email;
		public String city;
		public String address;
		public String notes;
	}
}
