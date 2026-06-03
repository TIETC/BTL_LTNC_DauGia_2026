package vn.edu.uet.daugia.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/he_thong_dau_gia";
    private static final String USER = "root";
    private static final String PASSWORD = "16122007"; // Nhập pass của em vào đây

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    public static boolean saveBidTransaction(String auctionId, String bidderId, double amount) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, price, bid_time) VALUES (?, ?, ?, NOW())";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            pstmt.setString(2, bidderId);
            pstmt.setDouble(3, amount);

            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi ghi Database: " + e.getMessage());
            return false;
        }
    }
    public static String checkLogin(String username, String password) {
        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra đăng nhập: " + e.getMessage());
        }
        return null;
    }
    public static boolean insertItem(String itemId, String name, double startingPrice) {
        String sql = "INSERT INTO items (item_id, name, starting_price) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, itemId);
            pstmt.setString(2, name);
            pstmt.setDouble(3, startingPrice);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi thêm sản phẩm: " + e.getMessage());
            return false;
        }
    }
}