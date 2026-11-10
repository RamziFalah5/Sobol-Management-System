package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

	public static class VehicleFormData {
		public String name, plateNumber, vin;
		public String vehicleType, manufacturer, model, status;
		public int modelYear, capacityPallets, maxLoadKg;
		public String fuelType, transmission;
		public String purchaseDate, lastServiceDate, insuranceExpiry;
		public String imagePath, notes;
	}

	public static List<Vehicle> getAllVehicles() {
		List<Vehicle> list = new ArrayList<>();
		String sql = """
				    SELECT vehicle_id, name, plate_number, vin,
				           vehicle_type, manufacturer, model, model_year,
				           status, capacity_pallets, max_load_kg,
				           fuel_type, transmission,
				           purchase_date, last_service_date, insurance_expiry,
				           image_path, notes
				    FROM vehicle
				    ORDER BY vehicle_id DESC
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new Vehicle(rs.getInt("vehicle_id"), rs.getString("name"), rs.getString("plate_number"),
						rs.getString("vin"), rs.getString("vehicle_type"), rs.getString("manufacturer"),
						rs.getString("model"), rs.getInt("model_year"), rs.getString("status"),
						rs.getInt("capacity_pallets"), rs.getInt("max_load_kg"), rs.getString("fuel_type"),
						rs.getString("transmission"), String.valueOf(rs.getDate("purchase_date")),
						String.valueOf(rs.getDate("last_service_date")), String.valueOf(rs.getDate("insurance_expiry")),
						rs.getString("image_path"), rs.getString("notes")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	// ===== Duplicate checks (global) =====
	public static boolean nameExists(String name) {
		return exists("SELECT 1 FROM vehicle WHERE name=? LIMIT 1", name);
	}

	public static boolean nameExistsForEdit(String name, int vehicleId) {
		return exists("SELECT 1 FROM vehicle WHERE name=? AND vehicle_id<>? LIMIT 1", name, vehicleId);
	}

	public static boolean plateExists(String plate) {
		return exists("SELECT 1 FROM vehicle WHERE plate_number=? LIMIT 1", plate);
	}

	public static boolean plateExistsForEdit(String plate, int vehicleId) {
		return exists("SELECT 1 FROM vehicle WHERE plate_number=? AND vehicle_id<>? LIMIT 1", plate, vehicleId);
	}

	public static boolean vinExists(String vin) {
		return exists("SELECT 1 FROM vehicle WHERE vin=? LIMIT 1", vin);
	}

	public static boolean vinExistsForEdit(String vin, int vehicleId) {
		return exists("SELECT 1 FROM vehicle WHERE vin=? AND vehicle_id<>? LIMIT 1", vin, vehicleId);
	}

	private static boolean exists(String sql, Object... params) {
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++)
				ps.setObject(i + 1, params[i]);
			return ps.executeQuery().next();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean insertVehicle(VehicleFormData d) {
		String sql = """
				    INSERT INTO vehicle(
				        name, plate_number, vin,
				        vehicle_type, manufacturer, model, model_year,
				        status, capacity_pallets, max_load_kg,
				        fuel_type, transmission,
				        purchase_date, last_service_date, insurance_expiry,
				        image_path, notes
				    )
				    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			int i = 1;
			ps.setString(i++, d.name);
			ps.setString(i++, d.plateNumber);
			ps.setString(i++, d.vin);

			ps.setString(i++, d.vehicleType);
			ps.setString(i++, d.manufacturer);
			ps.setString(i++, d.model);
			ps.setInt(i++, d.modelYear);

			ps.setString(i++, d.status);
			ps.setInt(i++, d.capacityPallets);
			ps.setInt(i++, d.maxLoadKg);

			ps.setString(i++, d.fuelType);
			ps.setString(i++, d.transmission);

			ps.setDate(i++, Date.valueOf(d.purchaseDate));
			ps.setDate(i++, Date.valueOf(d.lastServiceDate));
			ps.setDate(i++, Date.valueOf(d.insuranceExpiry));

			ps.setString(i++, d.imagePath);
			ps.setString(i++, d.notes);

			return ps.executeUpdate() > 0;

		} catch (SQLException e) {
			if (e.getErrorCode() == 1062)
				return false; // duplicate unique key
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean updateVehicle(int vehicleId, VehicleFormData d) {
		String sql = """
				    UPDATE vehicle
				    SET name=?, plate_number=?, vin=?,
				        vehicle_type=?, manufacturer=?, model=?, model_year=?,
				        status=?, capacity_pallets=?, max_load_kg=?,
				        fuel_type=?, transmission=?,
				        purchase_date=?, last_service_date=?, insurance_expiry=?,
				        image_path=?, notes=?
				    WHERE vehicle_id=?
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			int i = 1;
			ps.setString(i++, d.name);
			ps.setString(i++, d.plateNumber);
			ps.setString(i++, d.vin);

			ps.setString(i++, d.vehicleType);
			ps.setString(i++, d.manufacturer);
			ps.setString(i++, d.model);
			ps.setInt(i++, d.modelYear);

			ps.setString(i++, d.status);
			ps.setInt(i++, d.capacityPallets);
			ps.setInt(i++, d.maxLoadKg);

			ps.setString(i++, d.fuelType);
			ps.setString(i++, d.transmission);

			ps.setDate(i++, Date.valueOf(d.purchaseDate));
			ps.setDate(i++, Date.valueOf(d.lastServiceDate));
			ps.setDate(i++, Date.valueOf(d.insuranceExpiry));

			ps.setString(i++, d.imagePath);
			ps.setString(i++, d.notes);

			ps.setInt(i++, vehicleId);

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

	public static boolean deleteVehicle(int id) {
		String sql = "DELETE FROM vehicle WHERE vehicle_id=?";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, id);
			return ps.executeUpdate() > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static List<Vehicle> getAvailableVehiclesForDrivers() {
		List<Vehicle> list = new ArrayList<>();

		String sql = """
				    SELECT v.*
				    FROM vehicle v
				    WHERE NOT EXISTS (
				        SELECT 1
				        FROM employee e
				        WHERE e.employee_type='TRUCK_DRIVER'
				          AND e.vehicle_id = v.vehicle_id
				    )
				    ORDER BY v.name
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(mapVehicle(rs));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static List<Vehicle> getVehiclesForDriverEdit(int currentVehicleId) {
		List<Vehicle> list = new ArrayList<>();

		String sql = """
				    SELECT v.*
				    FROM vehicle v
				    WHERE v.vehicle_id = ?
				       OR NOT EXISTS (
				            SELECT 1
				            FROM employee e
				            WHERE e.employee_type='TRUCK_DRIVER'
				              AND e.vehicle_id = v.vehicle_id
				       )
				    ORDER BY v.name
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, currentVehicleId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapVehicle(rs));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	private static Vehicle mapVehicle(ResultSet rs) throws Exception {
		return new Vehicle(rs.getInt("vehicle_id"), rs.getString("name"), rs.getString("plate_number"),
				rs.getString("vin"), rs.getString("vehicle_type"), rs.getString("manufacturer"), rs.getString("model"),
				rs.getInt("model_year"), rs.getString("status"), rs.getInt("capacity_pallets"),
				rs.getInt("max_load_kg"), rs.getString("fuel_type"), rs.getString("transmission"),
				String.valueOf(rs.getDate("purchase_date")), String.valueOf(rs.getDate("last_service_date")),
				String.valueOf(rs.getDate("insurance_expiry")), rs.getString("image_path"), rs.getString("notes"));
	}
}
