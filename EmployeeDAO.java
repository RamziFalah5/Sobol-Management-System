package application;

import java.sql.*;
import java.time.LocalDate;

public class EmployeeDAO {

	public static boolean nameExists(String fullName) {
		String sql = "SELECT 1 FROM employee WHERE full_name = ? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			return ps.executeQuery().next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// Insert General Worker
	public static boolean insertGeneralWorker(String fullName, String phone, int branchId, LocalDate hireDate,
			String imagePath, String workerMode, Double dailyRate, Double hourlyRate) {

		String sql = """
				    INSERT INTO employee(
				      full_name, phone, branch_id, hire_date, employee_type, image_path,
				      worker_mode, daily_rate, hourly_rate
				    )
				    VALUES(?,?,?,?,?,?,?,?,?)
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDate(4, Date.valueOf(hireDate));
			ps.setString(5, "GENERAL_WORKER");
			ps.setString(6, imagePath);

			ps.setString(7, workerMode);
			if (dailyRate == null)
				ps.setNull(8, Types.DECIMAL);
			else
				ps.setDouble(8, dailyRate);

			if (hourlyRate == null)
				ps.setNull(9, Types.DECIMAL);
			else
				ps.setDouble(9, hourlyRate);

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

	public static boolean insertTruckDriver(String fullName, String phone, int branchId, LocalDate hireDate,
			String imagePath, double dailyRate, int vehicleId) {

		String sql = """
				    INSERT INTO employee(
				      full_name, phone, branch_id, hire_date, employee_type, image_path,
				      driver_daily_rate, vehicle_id
				    )
				    VALUES(?,?,?,?,?,?,?,?)
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDate(4, Date.valueOf(hireDate));
			ps.setString(5, "TRUCK_DRIVER");
			ps.setString(6, imagePath);
			ps.setDouble(7, dailyRate);
			ps.setInt(8, vehicleId);

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

	// Insert Field Admin
	public static boolean insertFieldAdmin(String fullName, String phone, int branchId, LocalDate hireDate,
			String imagePath, double dailyRate, int managedBranchId) {

		String sql = """
				    INSERT INTO employee(
				      full_name, phone, branch_id, hire_date, employee_type, image_path,
				      admin_daily_rate, managed_branch_id
				    )
				    VALUES(?,?,?,?,?,?,?,?)
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDate(4, Date.valueOf(hireDate));
			ps.setString(5, "FIELD_ADMIN");
			ps.setString(6, imagePath);
			ps.setDouble(7, dailyRate);
			ps.setInt(8, managedBranchId);
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

	// Insert Sales Rep
	public static boolean insertSalesRep(String fullName, String phone, int branchId, LocalDate hireDate,
			String imagePath, double dailyRate, double commissionRate) {

		String sql = """
				    INSERT INTO employee(
				      full_name, phone, branch_id, hire_date, employee_type, image_path,
				      sales_daily_rate, commission_rate
				    )
				    VALUES(?,?,?,?,?,?,?,?)
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDate(4, Date.valueOf(hireDate));
			ps.setString(5, "SALES_REP");
			ps.setString(6, imagePath);
			ps.setDouble(7, dailyRate);
			ps.setDouble(8, commissionRate);
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

	public static boolean updateDailyWorker(int employeeId, String fullName, String phone, int branchId,
			double dailyRate) {
		String sql = """
				    UPDATE employee
				    SET full_name=?, phone=?, branch_id=?,
				        worker_mode='DAILY', daily_rate=?, hourly_rate=NULL
				    WHERE employee_id=? AND employee_type='GENERAL_WORKER'
				""";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDouble(4, dailyRate);
			ps.setInt(5, employeeId);
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

	public static boolean updateHourlyWorker(int employeeId, String fullName, String phone, int branchId,
			double hourlyRate) {
		String sql = """
				    UPDATE employee
				    SET full_name=?, phone=?, branch_id=?,
				        worker_mode='HOURLY', hourly_rate=?, daily_rate=NULL
				    WHERE employee_id=? AND employee_type='GENERAL_WORKER'
				""";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDouble(4, hourlyRate);
			ps.setInt(5, employeeId);
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

	public static boolean updateTruckDriver(int employeeId, String fullName, String phone, int branchId,
			java.time.LocalDate hireDate, String imagePath, double dailyRate, int vehicleId) {

		String sql = """
				    UPDATE employee
				    SET full_name=?, phone=?, branch_id=?, hire_date=?,
				        image_path=?, driver_daily_rate=?, vehicle_id=?
				    WHERE employee_id=? AND employee_type='TRUCK_DRIVER'
				""";

		try (java.sql.Connection c = DBAuth.get(); java.sql.PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDate(4, java.sql.Date.valueOf(hireDate));
			ps.setString(5, imagePath);
			ps.setDouble(6, dailyRate);
			ps.setInt(7, vehicleId);
			ps.setInt(8, employeeId);

			return ps.executeUpdate() > 0;

		} catch (java.sql.SQLException e) {
			if (e.getErrorCode() == 1062)
				return false;
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
