package application;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class SalesRepDAO {

	public static List<SalesRepRow> getAllSalesReps() {
		List<SalesRepRow> list = new ArrayList<>();

		String sql = """
		    SELECT e.employee_id, e.full_name, e.phone, e.branch_id, b.name AS branch_name,
		           e.hire_date, e.image_path,
		           COALESCE(e.sales_daily_rate, 0) AS sales_daily_rate,
		           COALESCE(e.commission_rate, 0) AS commission_rate
		    FROM employee e
		    JOIN branch b ON b.branch_id = e.branch_id
		    WHERE e.employee_type='SALES_REP'
		      AND e.is_active=1
		    ORDER BY e.full_name
		""";

		try (Connection c = DBAuth.get();
		     PreparedStatement ps = c.prepareStatement(sql);
		     ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new SalesRepRow(
						rs.getInt("employee_id"),
						rs.getString("full_name"),
						rs.getString("phone"),
						rs.getInt("branch_id"),
						rs.getString("branch_name"),
						rs.getDate("hire_date").toLocalDate(),
						rs.getString("image_path"),
						rs.getDouble("sales_daily_rate"),
						rs.getDouble("commission_rate")
				));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static boolean deleteSalesRep(int employeeId) {
		String sql = """
		    UPDATE employee
		    SET is_active=0
		    WHERE employee_id=?
		      AND employee_type='SALES_REP'
		""";

		try (Connection c = DBAuth.get();
		     PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, employeeId);
			return ps.executeUpdate() > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean updateSalesRep(int employeeId, String fullName, String phone, int branchId,
			LocalDate hireDate, String imagePath, double dailyRate, double commissionRate) {

		String sql = """
		    UPDATE employee
		    SET full_name=?,
		        phone=?,
		        branch_id=?,
		        hire_date=?,
		        image_path=?,

		        employee_type='SALES_REP',

		        worker_mode=NULL,
		        daily_rate=NULL,
		        hourly_rate=NULL,
		        driver_daily_rate=NULL,
		        admin_daily_rate=NULL,
		        managed_branch_id=NULL,

		        sales_daily_rate=?,
		        commission_rate=?
		    WHERE employee_id=?
		      AND employee_type='SALES_REP'
		      AND is_active=1
		""";

		try (Connection c = DBAuth.get();
		     PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, fullName);
			ps.setString(2, phone);
			ps.setInt(3, branchId);
			ps.setDate(4, Date.valueOf(hireDate));

			if (imagePath == null || imagePath.isBlank())
				ps.setNull(5, Types.VARCHAR);
			else
				ps.setString(5, imagePath);

			ps.setDouble(6, dailyRate);
			ps.setDouble(7, commissionRate);
			ps.setInt(8, employeeId);

			return ps.executeUpdate() > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
