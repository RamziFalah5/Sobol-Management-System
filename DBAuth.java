package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBAuth {
    private static final String URL  = "jdbc:mysql://127.0.0.1:3306/accounts?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "70320139rmysql";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ MySQL Driver not found.", e);
        }
    }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void main(String[] args) {
        try (Connection conn = get()) {
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
