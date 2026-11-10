package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkerDAO {

	public static List<WorkerRow> getDailyWorkers() {
		String sql = """
				    SELECT e.employee_id, e.full_name, e.phone, e.branch_id,
				           b.name AS branch_name,
				           e.daily_rate AS rate
				    FROM employee e
				    LEFT JOIN branch b ON b.branch_id = e.branch_id
				    WHERE e.employee_type='GENERAL_WORKER' AND e.worker_mode='DAILY'
				    ORDER BY e.employee_id DESC
				""";

		List<WorkerRow> list = new ArrayList<>();
		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new WorkerRow(rs.getInt("employee_id"), rs.getString("full_name"), rs.getString("phone"),
						rs.getInt("branch_id"), rs.getString("branch_name"), rs.getDouble("rate")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public static List<WorkerRow> getHourlyWorkers() {
		String sql = """
				    SELECT e.employee_id, e.full_name, e.phone, e.branch_id,
				           b.name AS branch_name,
				           e.hourly_rate AS rate
				    FROM employee e
				    LEFT JOIN branch b ON b.branch_id = e.branch_id
				    WHERE e.employee_type='GENERAL_WORKER' AND e.worker_mode='HOURLY'
				    ORDER BY e.employee_id DESC
				""";

		List<WorkerRow> list = new ArrayList<>();
		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new WorkerRow(rs.getInt("employee_id"), rs.getString("full_name"), rs.getString("phone"),
						rs.getInt("branch_id"), rs.getString("branch_name"), rs.getDouble("rate")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public static boolean deleteEmployee(int employeeId) {
		String sql = "DELETE FROM employee WHERE employee_id=?";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, employeeId);
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
