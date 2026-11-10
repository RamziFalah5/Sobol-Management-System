package application;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class SalesDAO {

	public static List<SalesOrder> getAllOrders() {

		List<SalesOrder> list = new ArrayList<>();

		String sql = """
				select o.order_id,
				       o.customer_id,
				       c.full_name as customer_name,
				       o.employee_id,
				       e.full_name as employee_name,
				       o.order_date,
				       o.total_amount,
				       (select count(*) from sales_order_item i where i.order_id = o.order_id) as items_count
				from sales_order o
				join customer c on c.customer_id = o.customer_id
				join employee e on e.employee_id = o.employee_id
				order by o.order_id desc
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {

				LocalDate d = rs.getDate("order_date").toLocalDate();

				list.add(new SalesOrder(rs.getInt("order_id"), rs.getInt("customer_id"), rs.getInt("employee_id"),
						rs.getString("customer_name"), rs.getString("employee_name"), d, rs.getDouble("total_amount"),
						rs.getInt("items_count")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static List<SalesOrder> searchOrdersByCustomerName(String key) {

		if (key == null)
			key = "";
		key = key.trim();

		List<SalesOrder> list = new ArrayList<>();

		String sql = """
				select o.order_id,
				       o.customer_id,
				       c.full_name as customer_name,
				       o.employee_id,
				       e.full_name as employee_name,
				       o.order_date,
				       o.total_amount,
				       (select count(*) from sales_order_item i where i.order_id = o.order_id) as items_count
				from sales_order o
				join customer c on c.customer_id = o.customer_id
				join employee e on e.employee_id = o.employee_id
				where lower(c.full_name) like ?
				order by o.order_id desc
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, "%" + key.toLowerCase() + "%");

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				LocalDate d = rs.getDate("order_date").toLocalDate();

				list.add(new SalesOrder(rs.getInt("order_id"), rs.getInt("customer_id"), rs.getInt("employee_id"),
						rs.getString("customer_name"), rs.getString("employee_name"), d, rs.getDouble("total_amount"),
						rs.getInt("items_count")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static List<SalesItem> getOrderItems(int orderId) {

		List<SalesItem> list = new ArrayList<>();

		String sql = """
				select i.product_id,
				       p.name as product_name,
				       i.quantity,
				       i.unit_price,
				       i.line_total
				from sales_order_item i
				join product p on p.product_id = i.product_id
				where i.order_id = ?
				order by i.item_id asc
				""";

		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, orderId);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				list.add(new SalesItem(rs.getInt("product_id"), rs.getString("product_name"), rs.getInt("quantity"),
						rs.getDouble("unit_price"), rs.getDouble("line_total")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static List<ProductSalePickRow> getAllProductsForSale() {

		List<ProductSalePickRow> list = new ArrayList<>();

		String sql = """
				select product_id, name, quantity, selling_price
				from product
				order by product_id desc
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new ProductSalePickRow(rs.getInt("product_id"), rs.getString("name"), rs.getInt("quantity"),
						rs.getDouble("selling_price")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static List<EmployeeMini> getSalesEmployees() {

		List<EmployeeMini> list = new ArrayList<>();

		String sql = """
				select employee_id, full_name, commission_rate
				from employee
				where commission_rate is not null
				  and is_active = 1
				order by full_name
				""";

		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(new EmployeeMini(rs.getInt("employee_id"), rs.getString("full_name"),
						rs.getDouble("commission_rate")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static boolean createOrder(int customerId, int employeeId, List<OrderItemForm> items) {

		if (items == null || items.isEmpty())
			return false;

		String insOrder = """
				insert into sales_order (customer_id, employee_id, order_date, total_amount)
				values (?,?,?,0)
				""";

		String lockProduct = """
				select quantity, selling_price
				from product
				where product_id = ?
				""";

		String updProduct = """
				update product
				set quantity = ?
				where product_id = ?
				""";

		String insItem = """
				insert into sales_order_item (order_id, product_id, quantity, unit_price, line_total)
				values (?,?,?,?,?)
				""";

		String updOrderTotal = """
				update sales_order
				set total_amount = ?
				where order_id = ?
				""";

		try (Connection c = DBAuth.get()) {
			c.setAutoCommit(false);

			int orderId;

			try (PreparedStatement ps = c.prepareStatement(insOrder, Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, customerId);
				ps.setInt(2, employeeId);
				ps.setDate(3, Date.valueOf(LocalDate.now()));
				ps.executeUpdate();

				ResultSet keys = ps.getGeneratedKeys();
				if (!keys.next()) {
					c.rollback();
					return false;
				}
				orderId = keys.getInt(1);
			}

			double total = 0.0;

			for (OrderItemForm it : items) {

				int available;
				double unitPrice;

				try (PreparedStatement ps = c.prepareStatement(lockProduct)) {
					ps.setInt(1, it.productId);
					ResultSet rs = ps.executeQuery();
					if (!rs.next()) {
						c.rollback();
						return false;
					}
					available = rs.getInt("quantity");
					unitPrice = rs.getDouble("selling_price");
				}

				if (it.quantity <= 0) {
					c.rollback();
					return false;
				}

				if (available < it.quantity) {
					c.rollback();
					return false;
				}

				int newQty = available - it.quantity;
				double lineTotal = unitPrice * it.quantity;

				try (PreparedStatement ps = c.prepareStatement(updProduct)) {
					ps.setInt(1, newQty);
					ps.setInt(2, it.productId);
					ps.executeUpdate();
				}

				try (PreparedStatement ps = c.prepareStatement(insItem)) {
					ps.setInt(1, orderId);
					ps.setInt(2, it.productId);
					ps.setInt(3, it.quantity);
					ps.setDouble(4, unitPrice);
					ps.setDouble(5, lineTotal);
					ps.executeUpdate();
				}

				total += lineTotal;
			}

			try (PreparedStatement ps = c.prepareStatement(updOrderTotal)) {
				ps.setDouble(1, total);
				ps.setInt(2, orderId);
				ps.executeUpdate();
			}

			c.commit();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public static class EmployeeMini {
		public int id;
		public String name;
		public double commission;

		public EmployeeMini(int id, String name, double commission) {
			this.id = id;
			this.name = name;
			this.commission = commission;
		}

		@Override
		public String toString() {
			return name + " (comm: " + commission + "%)";
		}
	}

	public static class OrderItemForm {
		public int productId;
		public int quantity;

		public OrderItemForm(int productId, int quantity) {
			this.productId = productId;
			this.quantity = quantity;
		}
	}
}
