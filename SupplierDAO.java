package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

	public static List<Supplier> getAllSuppliers() {

		List<Supplier> list = new ArrayList<>();

		String sql = """
				SELECT supplier_id, name, phone, email, city, address, contact_person, notes
				FROM supplier
				ORDER BY supplier_id DESC
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new Supplier(rs.getInt("supplier_id"), rs.getString("name"), rs.getString("phone"),
						rs.getString("email"), rs.getString("city"), rs.getString("address"),
						rs.getString("contact_person"), rs.getString("notes")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static boolean nameExists(String name) {

		String sql = "SELECT 1 FROM supplier WHERE name=? LIMIT 1";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			return ps.executeQuery().next();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean nameExistsForEdit(String name, int supplierId) {

		String sql = "SELECT 1 FROM supplier WHERE name=? AND supplier_id<>? LIMIT 1";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setInt(2, supplierId);
			return ps.executeQuery().next();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean insertSupplier(SupplierFormData d) {

		String sql = """
				INSERT INTO supplier (name, phone, email, city, address, contact_person, notes)
				VALUES (?,?,?,?,?,?,?)
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, d.name);
			ps.setString(2, d.phone);

			if (d.email == null || d.email.isBlank())
				ps.setNull(3, Types.VARCHAR);
			else
				ps.setString(3, d.email);

			if (d.city == null || d.city.isBlank())
				ps.setNull(4, Types.VARCHAR);
			else
				ps.setString(4, d.city);

			if (d.address == null || d.address.isBlank())
				ps.setNull(5, Types.VARCHAR);
			else
				ps.setString(5, d.address);

			if (d.contactPerson == null || d.contactPerson.isBlank())
				ps.setNull(6, Types.VARCHAR);
			else
				ps.setString(6, d.contactPerson);

			if (d.notes == null || d.notes.isBlank())
				ps.setNull(7, Types.VARCHAR);
			else
				ps.setString(7, d.notes);

			return ps.executeUpdate() > 0;

		} catch (SQLException e) {
			if (e.getErrorCode() == 1062)
				return false; // duplicate
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean updateSupplier(int supplierId, SupplierFormData d) {

		String sql = """
				UPDATE supplier
				SET name=?, phone=?, email=?, city=?, address=?, contact_person=?, notes=?
				WHERE supplier_id=?
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, d.name);
			ps.setString(2, d.phone);

			if (d.email == null || d.email.isBlank())
				ps.setNull(3, Types.VARCHAR);
			else
				ps.setString(3, d.email);

			if (d.city == null || d.city.isBlank())
				ps.setNull(4, Types.VARCHAR);
			else
				ps.setString(4, d.city);

			if (d.address == null || d.address.isBlank())
				ps.setNull(5, Types.VARCHAR);
			else
				ps.setString(5, d.address);

			if (d.contactPerson == null || d.contactPerson.isBlank())
				ps.setNull(6, Types.VARCHAR);
			else
				ps.setString(6, d.contactPerson);

			if (d.notes == null || d.notes.isBlank())
				ps.setNull(7, Types.VARCHAR);
			else
				ps.setString(7, d.notes);

			ps.setInt(8, supplierId);

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

	public static class SupplierFormData {
		public String name;
		public String phone;
		public String email;
		public String city;
		public String address;
		public String contactPerson;
		public String notes;
	}
}
