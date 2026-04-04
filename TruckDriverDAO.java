package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TruckDriverDAO {

	public static List<TruckDriverRow> getAllDrivers() {

		List<TruckDriverRow> list = new ArrayList<>();

		String sql = """
				    SELECT e.employee_id, e.full_name, e.phone, e.branch_id,
				           b.name AS branch_name,
				           e.hire_date,
				           e.driver_daily_rate,
				           e.vehicle_id,
				           v.name AS vehicle_name,
				           e.image_path
				    FROM employee e
				    JOIN branch b ON b.branch_id = e.branch_id
				    LEFT JOIN vehicle v ON v.vehicle_id = e.vehicle_id
				    WHERE e.employee_type='TRUCK_DRIVER'
				    ORDER BY e.full_name
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new TruckDriverRow(rs.getInt("employee_id"), rs.getString("full_name"), rs.getString("phone"),
						rs.getInt("branch_id"), rs.getString("branch_name"), String.valueOf(rs.getDate("hire_date")),
						rs.getDouble("driver_daily_rate"), rs.getInt("vehicle_id"), rs.getString("vehicle_name"),
						rs.getString("image_path")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static boolean deleteDriver(int employeeId) {
		String sql = "DELETE FROM employee WHERE employee_id=? AND employee_type='TRUCK_DRIVER'";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, employeeId);
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
