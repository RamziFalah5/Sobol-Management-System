package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class WarehouseViewDAO {

    // ======= Small DTOs (no extra files needed) =======
    public static class WarehouseInfo {
        public final String warehouseName;
        public final String branchName;
        public final int capacity;

        public WarehouseInfo(String warehouseName, String branchName, int capacity) {
            this.warehouseName = warehouseName;
            this.branchName = branchName;
            this.capacity = capacity;
        }
    }

    public static class Summary {
        public final int capacity;
        public final int usedPallets;
        public final int totalProducts;
        public final int totalQuantity;
        public final int outOfStock;
        public final int lowStock;
        public final int avgCartonsPerPallet;
        public final int maxPalletsSingleProduct;

        public Summary(int capacity, int usedPallets, int totalProducts, int totalQuantity,
                       int outOfStock, int lowStock, int avgCartonsPerPallet, int maxPalletsSingleProduct) {
            this.capacity = capacity;
            this.usedPallets = usedPallets;
            this.totalProducts = totalProducts;
            this.totalQuantity = totalQuantity;
            this.outOfStock = outOfStock;
            this.lowStock = lowStock;
            this.avgCartonsPerPallet = avgCartonsPerPallet;
            this.maxPalletsSingleProduct = maxPalletsSingleProduct;
        }
    }

    public static class CategorySlice {
        public final String categoryName;
        public final int palletsUsed;

        public CategorySlice(String categoryName, int palletsUsed) {
            this.categoryName = categoryName;
            this.palletsUsed = palletsUsed;
        }
    }

    public static class BarPoint {
        public final String name;
        public final int value;

        public BarPoint(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class TrendPoint {
        public final String label;
        public final int value;

        public TrendPoint(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }

    public static class StockStatus {
        public final int out;
        public final int low;
        public final int ok;
        public final int high;

        public StockStatus(int out, int low, int ok, int high) {
            this.out = out;
            this.low = low;
            this.ok = ok;
            this.high = high;
        }
    }

    // ===================== Warehouse Names (NO IDs shown) =====================
    public static WarehouseInfo getWarehouseInfo(int warehouseId) {

        String sql = """
                SELECT w.name AS warehouse_name,
                       b.name AS branch_name,
                       w.pallet_capacity AS capacity
                FROM warehouse w
                JOIN branch b ON b.branch_id = w.branch_id
                WHERE w.warehouse_id = ?
                """;

        try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new WarehouseInfo(
                        rs.getString("warehouse_name"),
                        rs.getString("branch_name"),
                        rs.getInt("capacity")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // ===================== Products linked to warehouse =====================
    public static List<WarehouseProductRow> getProductsForWarehouse(int warehouseId) {

        List<WarehouseProductRow> list = new ArrayList<>();

        String sql = """
                SELECT product_id, name, quantity, cartons_per_pallet, image_path,
                       CASE
                         WHEN cartons_per_pallet IS NULL OR cartons_per_pallet <= 0 THEN 0
                         ELSE CEIL(quantity / cartons_per_pallet)
                       END AS pallets_used
                FROM product
                WHERE warehouse_id = ?
                ORDER BY product_id DESC
                """;

        try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, warehouseId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new WarehouseProductRow(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getInt("cartons_per_pallet"),
                        rs.getInt("pallets_used"),
                        rs.getString("image_path")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ===================== Sum of pallets used =====================
    public static int getUsedPalletsForWarehouse(int warehouseId) {

        String sql = """
                SELECT COALESCE(SUM(
                    CASE
                      WHEN cartons_per_pallet IS NULL OR cartons_per_pallet <= 0 THEN 0
                      ELSE CEIL(quantity / cartons_per_pallet)
                    END
                ), 0) AS used_pallets
                FROM product
                WHERE warehouse_id = ?
                """;

        try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, warehouseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("used_pallets");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // ===================== Summary Stats (Cards + Circle) =====================
    public static Summary getWarehouseSummary(int warehouseId) {

        String sql = """
                SELECT
                    w.pallet_capacity AS capacity,

                    COALESCE(SUM(
                        CASE
                          WHEN p.cartons_per_pallet IS NULL OR p.cartons_per_pallet <= 0 THEN 0
                          ELSE CEIL(p.quantity / p.cartons_per_pallet)
                        END
                    ), 0) AS used_pallets,

                    COUNT(*) AS total_products,
                    COALESCE(SUM(p.quantity), 0) AS total_quantity,

                    COALESCE(SUM(CASE WHEN p.quantity <= 0 THEN 1 ELSE 0 END), 0) AS out_of_stock,
                    COALESCE(SUM(CASE WHEN p.quantity > 0 AND p.quantity < p.min_quantity THEN 1 ELSE 0 END), 0) AS low_stock,

                    COALESCE(ROUND(AVG(CASE WHEN p.cartons_per_pallet IS NULL THEN 0 ELSE p.cartons_per_pallet END)), 0) AS avg_cartons,

                    COALESCE(MAX(
                        CASE
                          WHEN p.cartons_per_pallet IS NULL OR p.cartons_per_pallet <= 0 THEN 0
                          ELSE CEIL(p.quantity / p.cartons_per_pallet)
                        END
                    ), 0) AS max_pallets_single

                FROM warehouse w
                LEFT JOIN product p ON p.warehouse_id = w.warehouse_id
                WHERE w.warehouse_id = ?
                GROUP BY w.pallet_capacity
                """;

        try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, warehouseId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Summary(
                        rs.getInt("capacity"),
                        rs.getInt("used_pallets"),
                        rs.getInt("total_products"),
                        rs.getInt("total_quantity"),
                        rs.getInt("out_of_stock"),
                        rs.getInt("low_stock"),
                        rs.getInt("avg_cartons"),
                        rs.getInt("max_pallets_single")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // ===================== Pie: Pallets by Category (Top N + Others) =====================
    public static List<CategorySlice> getCategoryPalletDistribution(int warehouseId, int topN) {

        List<CategorySlice> result = new ArrayList<>();

        String sqlTop = """
                SELECT c.name AS category_name,
                       COALESCE(SUM(
                           CASE
                             WHEN p.cartons_per_pallet IS NULL OR p.cartons_per_pallet <= 0 THEN 0
                             ELSE CEIL(p.quantity / p.cartons_per_pallet)
                           END
                       ), 0) AS pallets_used
                FROM product p
                JOIN category c ON c.category_id = p.category_id
                WHERE p.warehouse_id = ?
                GROUP BY c.name
                ORDER BY pallets_used DESC
                LIMIT ?
                """;

        String sqlAll = """
                SELECT COALESCE(SUM(
                           CASE
                             WHEN p.cartons_per_pallet IS NULL OR p.cartons_per_pallet <= 0 THEN 0
                             ELSE CEIL(p.quantity / p.cartons_per_pallet)
                           END
                       ), 0) AS total_pallets
                FROM product p
                WHERE p.warehouse_id = ?
                """;

        try (Connection c = DBAuth.get()) {

            int topSum = 0;

            try (PreparedStatement ps = c.prepareStatement(sqlTop)) {
                ps.setInt(1, warehouseId);
                ps.setInt(2, Math.max(1, topN));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int v = rs.getInt("pallets_used");
                    topSum += v;
                    result.add(new CategorySlice(rs.getString("category_name"), v));
                }
            }

            int total = 0;
            try (PreparedStatement ps2 = c.prepareStatement(sqlAll)) {
                ps2.setInt(1, warehouseId);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) total = rs2.getInt("total_pallets");
            }

            int others = Math.max(0, total - topSum);
            if (others > 0) result.add(new CategorySlice("Others", others));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // ===================== Bar: Top Products by Pallets Used =====================
    public static List<BarPoint> getTopProductsByPallets(int warehouseId, int limit) {

        List<BarPoint> list = new ArrayList<>();

        String sql = """
                SELECT p.name,
                       CASE
                         WHEN p.cartons_per_pallet IS NULL OR p.cartons_per_pallet <= 0 THEN 0
                         ELSE CEIL(p.quantity / p.cartons_per_pallet)
                       END AS pallets_used
                FROM product p
                WHERE p.warehouse_id = ?
                ORDER BY pallets_used DESC, p.product_id DESC
                LIMIT ?
                """;

        try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            ps.setInt(2, Math.max(1, limit));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                int v = rs.getInt("pallets_used");
                list.add(new BarPoint(shortName(name, 14), v));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ===================== Line: Products added last X days =====================
    public static List<TrendPoint> getProductsAddedTrend(int warehouseId, int days) {

        List<TrendPoint> list = new ArrayList<>();

        String sql = """
                SELECT DATE(p.created_at) AS d, COUNT(*) AS cnt
                FROM product p
                WHERE p.warehouse_id = ?
                  AND p.created_at >= (CURRENT_DATE - INTERVAL ? DAY)
                GROUP BY DATE(p.created_at)
                ORDER BY d ASC
                """;

        try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, warehouseId);
            ps.setInt(2, Math.max(7, days));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String label = String.valueOf(rs.getDate("d")); // yyyy-mm-dd
                int v = rs.getInt("cnt");
                list.add(new TrendPoint(label, v));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ===================== Area: Stock Status Counts =====================
    public static StockStatus getStockStatusCounts(int warehouseId) {

        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN quantity <= 0 THEN 1 ELSE 0 END), 0) AS out_cnt,
                    COALESCE(SUM(CASE WHEN quantity > 0 AND quantity < min_quantity THEN 1 ELSE 0 END), 0) AS low_cnt,
                    COALESCE(SUM(CASE WHEN quantity >= min_quantity AND quantity <= max_quantity THEN 1 ELSE 0 END), 0) AS ok_cnt,
                    COALESCE(SUM(CASE WHEN quantity > max_quantity AND max_quantity > 0 THEN 1 ELSE 0 END), 0) AS high_cnt
                FROM product
                WHERE warehouse_id = ?
                """;

        try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, warehouseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new StockStatus(
                        rs.getInt("out_cnt"),
                        rs.getInt("low_cnt"),
                        rs.getInt("ok_cnt"),
                        rs.getInt("high_cnt")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new StockStatus(0, 0, 0, 0);
    }

    private static String shortName(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(1, max - 1)) + "…";
    }
}
