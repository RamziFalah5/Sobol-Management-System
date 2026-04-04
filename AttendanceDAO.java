package application;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class AttendanceDAO {

	public static Boolean getPresent(int employeeId, LocalDate date) {
		String sql = "SELECT present FROM attendance WHERE employee_id=? AND work_date=? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, employeeId);
			ps.setDate(2, Date.valueOf(date));
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getInt("present") == 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Integer getHours(int employeeId, LocalDate date) {
		String sql = "SELECT hours_worked FROM attendance WHERE employee_id=? AND work_date=? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, employeeId);
			ps.setDate(2, Date.valueOf(date));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				int v = rs.getInt("hours_worked");
				return rs.wasNull() ? null : v;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// ✅ Daily: present, hours = NULL
	public static boolean saveDaily(LocalDate date, List<DailyAttendanceRow> rows) {
		String sql = """
				    INSERT INTO attendance(employee_id, work_date, present, hours_worked)
				    VALUES(?, ?, ?, NULL)
				    ON DUPLICATE KEY UPDATE present=VALUES(present), hours_worked=NULL
				""";

		try (Connection c = DBAuth.get()) {
			c.setAutoCommit(false);
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				for (DailyAttendanceRow r : rows) {
					ps.setInt(1, r.getEmployeeId());
					ps.setDate(2, Date.valueOf(date));
					ps.setInt(3, r.isPresent() ? 1 : 0);
					ps.addBatch();
				}
				ps.executeBatch();
				c.commit();
				return true;
			} catch (Exception e) {
				c.rollback();
				throw e;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// ✅ Hourly: hours, present = (hours>0)
	public static boolean saveHourly(LocalDate date, List<HourlyAttendanceRow> rows) {
		String sql = """
				    INSERT INTO attendance(employee_id, work_date, present, hours_worked)
				    VALUES(?, ?, ?, ?)
				    ON DUPLICATE KEY UPDATE present=VALUES(present), hours_worked=VALUES(hours_worked)
				""";

		try (Connection c = DBAuth.get()) {
			c.setAutoCommit(false);
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				for (HourlyAttendanceRow r : rows) {
					int h = r.getHours();
					ps.setInt(1, r.getEmployeeId());
					ps.setDate(2, Date.valueOf(date));
					ps.setInt(3, (h > 0) ? 1 : 0);
					ps.setInt(4, h);
					ps.addBatch();
				}
				ps.executeBatch();
				c.commit();
				return true;
			} catch (Exception e) {
				c.rollback();
				throw e;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean upsertAttendance(int employeeId, LocalDate date, boolean present, Integer hoursWorked) {

		String sql = """
				    INSERT INTO attendance(employee_id, work_date, present, hours_worked)
				    VALUES(?,?,?,?)
				    ON DUPLICATE KEY UPDATE
				        present=VALUES(present),
				        hours_worked=VALUES(hours_worked)
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, employeeId);
			ps.setDate(2, Date.valueOf(date));
			ps.setInt(3, present ? 1 : 0);

			if (hoursWorked == null)
				ps.setNull(4, Types.INTEGER);
			else
				ps.setInt(4, hoursWorked);

			return ps.executeUpdate() > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
