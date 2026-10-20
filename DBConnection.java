
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBConnection {

    // SQLite database file
    private static final String DB_URL =
            "jdbc:sqlite:railway_reservation.db";

    // Private constructor
    private DBConnection() {
    }

    // Get database connection
    public static Connection getConnection()
            throws SQLException {

        try {
            Class.forName("org.sqlite.JDBC");

        } catch (ClassNotFoundException e) {

            throw new SQLException(
                "SQLite JDBC Driver not found. Check your lib folder.",
                e
            );
        }

        Connection conn = DriverManager.getConnection(DB_URL);
        try (PreparedStatement ps = conn.prepareStatement(
        "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
     ResultSet rs = ps.executeQuery()) {

    System.out.println("===== TABLES FOUND BY JAVA =====");

    while (rs.next()) {
        System.out.println(rs.getString("name"));
    }
}

System.out.println(
    "Database location: " +
    new java.io.File("railway_reservation.db").getAbsolutePath()
);

return conn;
    }

    // Test database connection
    public static void testConnection()
            throws SQLException {

        try (Connection conn = getConnection()) {

            System.out.println(
                "SQLite Database Connected Successfully!"
            );

            String sql = "SELECT name FROM sqlite_master "
                       + "WHERE type='table'";

            try (PreparedStatement ps =
                    conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                System.out.println("Database Tables:");

                while (rs.next()) {
                    System.out.println(rs.getString("name"));
                }
            }
        }
    }
}