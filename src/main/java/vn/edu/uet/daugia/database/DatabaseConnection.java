package vn.edu.uet.daugia.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.*;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/he_thong_dau_gia";
    private static final String USER = "root";
    private static final String PASSWORD = "16122007"; // Giữ nguyên pass của em

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // ... (Các hàm checkLogin, registerUser giữ nguyên như cũ, em có thể tự giữ lại hoặc copy từ bản trước) ...
    public static String checkLogin(String username, String password) {
        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username); pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getString("role"); }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static boolean registerUser(String username, String password, String role) {
        String checkSql = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, username);
            if (checkStmt.executeQuery().next()) return false;
        } catch (SQLException e) { return false; }
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username); pstmt.setString(2, password); pstmt.setString(3, role);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // [ĐÃ SỬA]: Thêm cột description vào lệnh INSERT
    public static boolean insertItem(String itemId, String name, double startingPrice, String startTime, String endTime, String imagePath, String sellerName, String description) {
        String sql = "INSERT INTO items (item_id, name, starting_price, start_time, end_time, image_path, seller_name, status, description) VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN', ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId); pstmt.setString(2, name); pstmt.setDouble(3, startingPrice);
            pstmt.setString(4, startTime); pstmt.setString(5, endTime); pstmt.setString(6, imagePath);
            pstmt.setString(7, sellerName); pstmt.setString(8, description);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // [ĐÃ SỬA]: Lấy thêm cột description ra
    public static String getAllItemsAsJson() {
        String sql = "SELECT * FROM items";
        JsonArray jsonArray = new JsonArray();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                JsonObject item = new JsonObject();
                item.addProperty("item_id", rs.getString("item_id"));
                item.addProperty("name", rs.getString("name"));
                item.addProperty("starting_price", rs.getDouble("starting_price"));
                item.addProperty("start_time", rs.getString("start_time"));
                item.addProperty("end_time", rs.getString("end_time"));
                item.addProperty("image_path", rs.getString("image_path"));
                item.addProperty("seller_name", rs.getString("seller_name"));
                item.addProperty("description", rs.getString("description"));
                item.addProperty("status", rs.getString("status") != null ? rs.getString("status") : "OPEN");
                jsonArray.add(item);
            }
            JsonObject responseJSON = new JsonObject();
            responseJSON.addProperty("status", "SUCCESS");
            responseJSON.add("data", jsonArray);
            return responseJSON.toString();
        } catch (SQLException e) { return "{\"status\":\"ERROR\"}"; }
    }

    // [TÍNH NĂNG MỚI]: Hàm dành riêng cho việc cập nhật giá liên tục
    public static String getItemAsJson(String itemId) {
        String sql = "SELECT starting_price FROM items WHERE item_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("starting_price", rs.getDouble("starting_price"));
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "SUCCESS");
                    response.add("data", item);
                    return response.toString();
                }
            }
        } catch (SQLException e) { return "{\"status\":\"ERROR\"}"; }
        return "{\"status\":\"ERROR\"}";
    }

    // [TÍNH NĂNG MỚI]: Ghi nhận giá đấu mới vào CSDL
    public static String placeBid(String itemId, String bidderId, double newPrice) {
        String updateSql = "UPDATE items SET starting_price = ? WHERE item_id = ?";
        String insertSql = "INSERT INTO bids (auction_id, bidder_id, price, bid_time) VALUES (?, ?, ?, NOW())";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Bật chế độ giao dịch an toàn
            try (PreparedStatement upd = conn.prepareStatement(updateSql);
                 PreparedStatement ins = conn.prepareStatement(insertSql)) {
                upd.setDouble(1, newPrice); upd.setString(2, itemId); upd.executeUpdate();
                ins.setString(1, itemId); ins.setString(2, bidderId); ins.setDouble(3, newPrice); ins.executeUpdate();
                conn.commit();
                return "{\"status\":\"SUCCESS\"}";
            } catch (SQLException e) {
                conn.rollback();
                return "{\"status\":\"ERROR\", \"message\":\"Lỗi CSDL khi đặt giá\"}";
            }
        } catch (SQLException e) { return "{\"status\":\"ERROR\"}"; }
    }
}