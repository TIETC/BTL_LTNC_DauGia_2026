package vn.edu.uet.daugia.server.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import vn.edu.uet.daugia.shared.exception.AuctionClosedException;
import vn.edu.uet.daugia.shared.exception.InvalidBidException;

import java.sql.*;
import java.time.LocalDateTime;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/he_thong_dau_gia";
    private static final String USER = "root";
    private static final String PASSWORD = "16122007"; //ĐỔi theo pass

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

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

    public static boolean insertItem(String itemId, String name, double startingPrice, String startTime, String endTime, String imagePath, String sellerName, String description) {
        String sql = "INSERT INTO items (item_id, name, starting_price, start_time, end_time, image_path, seller_name, status, description) VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN', ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId); pstmt.setString(2, name); pstmt.setDouble(3, startingPrice);
            pstmt.setString(4, startTime); pstmt.setString(5, endTime); pstmt.setString(6, imagePath);
            pstmt.setString(7, sellerName); pstmt.setString(8, description);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

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

    // [CORE LOGIC]: Đấu giá an toàn (Concurrency) & Chống bắn tỉa (Anti-Sniping)
    public static String placeBid(String itemId, String bidderId, double newPrice) throws InvalidBidException, AuctionClosedException {
        final int SNIPING_THRESHOLD_SECONDS = 30; // Ngưỡng 30s cuối
        final int EXTENSION_SECONDS = 60;         // Tự động gia hạn 60s

        // Dùng FOR UPDATE để khóa dòng sản phẩm này lại, các Request khác phải xếp hàng chờ
        String selectForUpdateSql = "SELECT starting_price, end_time, status FROM items WHERE item_id = ? FOR UPDATE";
        String updateItemSql = "UPDATE items SET starting_price = ?, end_time = ? WHERE item_id = ?";
        String insertBidSql = "INSERT INTO bids (auction_id, bidder_id, price, bid_time) VALUES (?, ?, ?, NOW())";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Bật Giao dịch an toàn (Transaction)

            try (PreparedStatement selectStmt = conn.prepareStatement(selectForUpdateSql)) {
                selectStmt.setString(1, itemId);
                ResultSet rs = selectStmt.executeQuery(); // Dòng DB bị KHÓA tại đây!

                if (rs.next()) {
                    double currentPrice = rs.getDouble("starting_price");
                    String status = rs.getString("status");
                    Timestamp endTimeTs = rs.getTimestamp("end_time");
                    LocalDateTime endTime = endTimeTs.toLocalDateTime();

                    // 1. Kiểm tra nghiệp vụ bằng OOP Exception
                    if (!"OPEN".equals(status) && !"RUNNING".equals(status)) {
                        conn.rollback();
                        throw new AuctionClosedException("Phiên đấu giá đã kết thúc, không thể đặt giá!");
                    }
                    if (newPrice <= currentPrice) {
                        conn.rollback();
                        throw new InvalidBidException("Giá đặt (" + newPrice + ") phải cao hơn giá hiện tại (" + currentPrice + ")!");
                    }
                    if (LocalDateTime.now().isAfter(endTime)) {
                        conn.rollback();
                        throw new AuctionClosedException("Đã hết hạn đặt giá!");
                    }

                    // 2. Thuật toán Anti-sniping
                    LocalDateTime now = LocalDateTime.now();
                    java.time.Duration timeRemaining = java.time.Duration.between(now, endTime);

                    if (timeRemaining.getSeconds() <= SNIPING_THRESHOLD_SECONDS && timeRemaining.getSeconds() > 0) {
                        endTime = endTime.plusSeconds(EXTENSION_SECONDS);
                        System.out.println("[Anti-Sniping] Kích hoạt! Gia hạn thêm " + EXTENSION_SECONDS + "s cho sản phẩm " + itemId);
                    }

                    // 3. Thực thi cập nhật xuống CSDL
                    try (PreparedStatement upd = conn.prepareStatement(updateItemSql);
                         PreparedStatement ins = conn.prepareStatement(insertBidSql)) {

                        upd.setDouble(1, newPrice);
                        upd.setTimestamp(2, Timestamp.valueOf(endTime));
                        upd.setString(3, itemId);
                        upd.executeUpdate();

                        ins.setString(1, itemId);
                        ins.setString(2, bidderId);
                        ins.setDouble(3, newPrice);
                        ins.executeUpdate();

                        conn.commit(); // Thành công -> Lưu dữ liệu và Mở khóa
                        return "{\"status\":\"SUCCESS\",\"message\":\"Đặt giá thành công!\"}";
                    }
                } else {
                    conn.rollback();
                    return "{\"status\":\"ERROR\", \"message\":\"Không tìm thấy sản phẩm\"}";
                }
            } catch (Exception e) {
                conn.rollback();
                throw e; // Ném lỗi vỡ lại cho ClientHandler xử lý
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"status\":\"ERROR\", \"message\":\"Lỗi hệ thống CSDL\"}";
        }
    }
}