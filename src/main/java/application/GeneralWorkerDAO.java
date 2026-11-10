package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GeneralWorkerDAO {

	public static List<GeneralWorkerRow> getDailyWorkers() {
		String sql = """
				    SELECT e.employee_id, e.full_name, e.phone, e.branch_id, b.name AS branch_name,
				           e.daily_rate, e.hire_date, e.image_path
				    FROM employee e
				    JOIN branch b ON b.branch_id = e.branch_id
				    WHERE e.employee_type='GENERAL_WORKER' AND e.worker_mode='DAILY'
				    ORDER BY e.employee_id DESC
				""";

		List<GeneralWorkerRow> list = new ArrayList<>();
		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(
						new GeneralWorkerRow(rs.getInt("employee_id"), rs.getString("full_name"), rs.getString("phone"),
								rs.getInt("branch_id"), rs.getString("branch_name"), rs.getDouble("daily_rate"),
								String.valueOf(rs.getDate("hire_date")), rs.getString("image_path")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public static List<GeneralWorkerRow> getHourlyWorkers() {
		String sql = """
				    SELECT e.employee_id, e.full_name, e.phone, e.branch_id, b.name AS branch_name,
				           e.hourly_rate, e.hire_date, e.image_path
				    FROM employee e
				    JOIN branch b ON b.branch_id = e.branch_id
				    WHERE e.employee_type='GENERAL_WORKER' AND e.worker_mode='HOURLY'
				    ORDER BY e.employee_id DESC
				""";

		List<GeneralWorkerRow> list = new ArrayList<>();
		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(
						new GeneralWorkerRow(rs.getInt("employee_id"), rs.getString("full_name"), rs.getString("phone"),
								rs.getInt("branch_id"), rs.getString("branch_name"), rs.getDouble("hourly_rate"),
								String.valueOf(rs.getDate("hire_date")), rs.getString("image_path")));
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

	public static boolean nameExistsForEdit(String fullName, int employeeId) {
		String sql = "SELECT 1 FROM employee WHERE full_name=? AND employee_id<>? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			ps.setInt(2, employeeId);
			return ps.executeQuery().next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean updateDailyWorker(int employeeId, String fullName, String phone, int branchId, Date hireDate,
			String imagePath, double dailyRate) {

		String sql = """
				    UPDATE employee
				    SET full_name=?, phone=?, branch_id=?, hire_date=?, image_path=?,
				        worker_mode='DAILY', daily_rate=?, hourly_rate=NULL
				    WHERE employee_id=? AND employee_type='GENERAL_WORKER'
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDate(4, hireDate);
			ps.setString(5, imagePath);
			ps.setDouble(6, dailyRate);
			ps.setInt(7, employeeId);
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

	public static boolean updateHourlyWorker(int employeeId, String fullName, String phone, int branchId, Date hireDate,
			String imagePath, double hourlyRate) {

		String sql = """
				    UPDATE employee
				    SET full_name=?, phone=?, branch_id=?, hire_date=?, image_path=?,
				        worker_mode='HOURLY', hourly_rate=?, daily_rate=NULL
				    WHERE employee_id=? AND employee_type='GENERAL_WORKER'
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDate(4, hireDate);
			ps.setString(5, imagePath);
			ps.setDouble(6, hourlyRate);
			ps.setInt(7, employeeId);
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
}
