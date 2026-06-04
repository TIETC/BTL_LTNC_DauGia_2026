package vn.edu.uet.daugia.server.admin;

import vn.edu.uet.daugia.database.DatabaseConnection;
import vn.edu.uet.daugia.server.AuctionManager;
import vn.edu.uet.daugia.server.AuctionService;
import vn.edu.uet.daugia.shared.model.Auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminService – xử lý toàn bộ nghiệp vụ dành cho ADMIN.
 *
 * Các tính năng:
 *  1. Xem tất cả phiên đấu giá (mọi trạng thái)
 *  2. Xóa sản phẩm / phiên đấu giá của Seller
 *  3. Quản lý Bidder: xem danh sách, khoá / mở tài khoản
 *  4. Quản lý Seller: xem danh sách, khoá / mở tài khoản
 *  5. Thống kê tổng quan (tổng phiên, tổng user…)
 */
public class AdminService {

    private final AuctionService auctionService = new AuctionService();
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    // =========================================================
    // 1. QUẢN LÝ PHIÊN ĐẤU GIÁ
    // =========================================================

    /**
     * Lấy tất cả phiên đấu giá (OPEN, RUNNING, FINISHED, CANCELED, PAID).
     * Trả JSON Array.
     */
    public String handleGetAllAuctions() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return "{\"status\":\"ERROR\",\"message\":\"Không kết nối được DB\"}";

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, itemName, sellerName, startPrice, currentPrice, " +
                            "startTime, endTime, status, winner, image_url " +
                            "FROM auctions ORDER BY endTime DESC");
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(buildAuctionJson(rs));
            }
            sb.append("]");
            return sb.toString();

        } catch (Exception e) {
            System.err.println("[AdminService] Lỗi GET_ALL_AUCTIONS: " + e.getMessage());
            return "[]";
        }
    }

    /**
     * Admin xóa một phiên đấu giá bất kỳ (kể cả RUNNING).
     * – Dừng phiên trong RAM nếu đang chạy.
     * – Xóa bids trước rồi mới xóa auction (tránh vi phạm FK).
     * – Broadcast AUCTION_DELETED tới tất cả client.
     */
    public String handleAdminDeleteAuction(String auctionId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return "{\"status\":\"ERROR\",\"message\":\"Không kết nối được DB\"}";

            // Kiểm tra tồn tại
            PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT status FROM auctions WHERE id = ?");
            psCheck.setString(1, auctionId);
            ResultSet rsCheck = psCheck.executeQuery();
            if (!rsCheck.next()) {
                return "{\"status\":\"ERROR\",\"message\":\"Không tìm thấy phiên\"}";
            }

            // Dừng phiên RAM nếu đang RUNNING
            Auction ramAuction = auctionManager.findById(auctionId);
            if (ramAuction != null) {
                ramAuction.closeAuction();
                auctionManager.removeAuction(auctionId);
            }

            // Xóa bids (foreign key)
            PreparedStatement psDelBids = conn.prepareStatement(
                    "DELETE FROM bids WHERE auctionId = ?");
            psDelBids.setString(1, auctionId);
            psDelBids.executeUpdate();

            // Xóa auction
            PreparedStatement psDelete = conn.prepareStatement(
                    "DELETE FROM auctions WHERE id = ?");
            psDelete.setString(1, auctionId);
            int affected = psDelete.executeUpdate();

            if (affected > 0) {
                String push = String.format(
                        "{\"type\":\"AUCTION_DELETED\",\"auctionId\":\"%s\"}", escape(auctionId));
                auctionManager.notifyAllClients(push);
                System.out.println("[ADMIN] Đã xóa phiên: " + auctionId);
                return "{\"status\":\"OK\",\"message\":\"Đã xóa phiên đấu giá\"}";
            }
            return "{\"status\":\"ERROR\",\"message\":\"Xóa thất bại\"}";

        } catch (Exception e) {
            System.err.println("[AdminService] Lỗi xóa phiên: " + e.getMessage());
            return "{\"status\":\"ERROR\",\"message\":\"" + escape(e.getMessage()) + "\"}";
        }
    }

    /**
     * Admin hủy (CANCELED) một phiên đang RUNNING mà không xóa lịch sử.
     */
    public String handleAdminCancelAuction(String auctionId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return "{\"status\":\"ERROR\",\"message\":\"Không kết nối được DB\"}";

            Auction ramAuction = auctionManager.findById(auctionId);
            if (ramAuction != null) {
                ramAuction.closeAuction();
                auctionManager.removeAuction(auctionId);
            }

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE auctions SET status = 'CANCELED' WHERE id = ?");
            ps.setString(1, auctionId);
            int affected = ps.executeUpdate();

            if (affected > 0) {
                String push = String.format(
                        "{\"type\":\"AUCTION_CLOSED\",\"auctionId\":\"%s\"," +
                                "\"status\":\"CANCELED\",\"winner\":\"\",\"finalPrice\":0}",
                        escape(auctionId));
                auctionManager.notifyAllClients(push);
                return "{\"status\":\"OK\",\"message\":\"Đã hủy phiên đấu giá\"}";
            }
            return "{\"status\":\"ERROR\",\"message\":\"Không tìm thấy phiên\"}";

        } catch (Exception e) {
            return "{\"status\":\"ERROR\",\"message\":\"" + escape(e.getMessage()) + "\"}";
        }
    }

    // =========================================================
    // 2. QUẢN LÝ BIDDER
    // =========================================================

    /**
     * Lấy danh sách tất cả Bidder.
     * Trả JSON Array gồm: username, email, balance, isActive.
     */
    public String handleGetAllBidders() {
        return getUsersByRole("BIDDER", true);
    }

    /**
     * Khoá tài khoản Bidder (isActive = false).
     * Bidder bị khoá không thể đăng nhập.
     */
    public String handleBanBidder(String username) {
        return setUserActive(username, "BIDDER", false);
    }

    /**
     * Mở khoá tài khoản Bidder.
     */
    public String handleUnbanBidder(String username) {
        return setUserActive(username, "BIDDER", true);
    }

    /**
     * Xóa Bidder khỏi hệ thống.
     * Lưu ý: sẽ không xóa nếu Bidder còn bid đang RUNNING (tránh mất dữ liệu).
     */
    public String handleDeleteBidder(String username) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return "{\"status\":\"ERROR\",\"message\":\"Không kết nối được DB\"}";

            // Kiểm tra còn bid trong phiên đang chạy không
            PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT COUNT(*) FROM bids b " +
                            "JOIN auctions a ON b.auctionId = a.id " +
                            "WHERE b.bidderId = ? AND a.status = 'RUNNING'");
            psCheck.setString(1, username);
            ResultSet rs = psCheck.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "{\"status\":\"ERROR\",\"message\":\"Bidder đang có bid trong phiên RUNNING\"}";
            }

            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM users WHERE username = ? AND role = 'BIDDER'");
            ps.setString(1, username);
            int affected = ps.executeUpdate();

            return affected > 0
                    ? "{\"status\":\"OK\",\"message\":\"Đã xóa bidder\"}"
                    : "{\"status\":\"ERROR\",\"message\":\"Không tìm thấy bidder\"}";

        } catch (Exception e) {
            return "{\"status\":\"ERROR\",\"message\":\"" + escape(e.getMessage()) + "\"}";
        }
    }

    // =========================================================
    // 3. QUẢN LÝ SELLER
    // =========================================================

    /**
     * Lấy danh sách tất cả Seller.
     * Trả JSON Array gồm: username, email, shopName, isActive, totalAuctions.
     */
    public String handleGetAllSellers() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return "[]";

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.username, u.email, u.shop_name, u.is_active, " +
                            "       COUNT(a.id) AS totalAuctions " +
                            "FROM users u " +
                            "LEFT JOIN auctions a ON a.sellerName = u.username " +
                            "WHERE u.role = 'SELLER' " +
                            "GROUP BY u.username, u.email, u.shop_name, u.is_active " +
                            "ORDER BY u.username");
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(String.format(
                        "{\"username\":\"%s\",\"email\":\"%s\"," +
                                "\"shopName\":\"%s\",\"isActive\":%b,\"totalAuctions\":%d}",
                        escape(rs.getString("username")),
                        escape(safeStr(rs, "email")),
                        escape(safeStr(rs, "shop_name")),
                        rs.getBoolean("is_active"),
                        rs.getInt("totalAuctions")));
            }
            sb.append("]");
            return sb.toString();

        } catch (Exception e) {
            System.err.println("[AdminService] Lỗi GET_ALL_SELLERS: " + e.getMessage());
            return "[]";
        }
    }

    /**
     * Khoá tài khoản Seller.
     */
    public String handleBanSeller(String username) {
        return setUserActive(username, "SELLER", false);
    }

    /**
     * Mở khoá tài khoản Seller.
     */
    public String handleUnbanSeller(String username) {
        return setUserActive(username, "SELLER", true);
    }

    /**
     * Admin xóa tất cả sản phẩm (phiên đấu giá) của một Seller cụ thể.
     * Chỉ xóa phiên FINISHED / CANCELED – phiên RUNNING sẽ bị hủy trước.
     */
    public String handleDeleteAllAuctionsBySeller(String sellerName) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return "{\"status\":\"ERROR\",\"message\":\"Không kết nối được DB\"}";

            // Lấy danh sách phiên của seller
            PreparedStatement psGet = conn.prepareStatement(
                    "SELECT id, status FROM auctions WHERE sellerName = ?");
            psGet.setString(1, sellerName);
            ResultSet rs = psGet.executeQuery();

            List<String> auctionIds = new ArrayList<>();
            while (rs.next()) {
                String aid = rs.getString("id");
                auctionIds.add(aid);
                // Dừng phiên RAM nếu RUNNING
                if ("RUNNING".equals(rs.getString("status"))) {
                    Auction ram = auctionManager.findById(aid);
                    if (ram != null) {
                        ram.closeAuction();
                        auctionManager.removeAuction(aid);
                    }
                }
            }

            if (auctionIds.isEmpty()) {
                return "{\"status\":\"OK\",\"message\":\"Seller không có phiên nào\"}";
            }

            // Xóa bids rồi xóa auctions theo batch
            for (String aid : auctionIds) {
                PreparedStatement psBids = conn.prepareStatement(
                        "DELETE FROM bids WHERE auctionId = ?");
                psBids.setString(1, aid);
                psBids.executeUpdate();
            }

            PreparedStatement psDelAuc = conn.prepareStatement(
                    "DELETE FROM auctions WHERE sellerName = ?");
            psDelAuc.setString(1, sellerName);
            int deleted = psDelAuc.executeUpdate();

            // Broadcast xóa cho từng phiên
            for (String aid : auctionIds) {
                String push = String.format(
                        "{\"type\":\"AUCTION_DELETED\",\"auctionId\":\"%s\"}", escape(aid));
                auctionManager.notifyAllClients(push);
            }

            System.out.printf("[ADMIN] Đã xóa %d phiên của seller: %s%n", deleted, sellerName);
            return String.format("{\"status\":\"OK\",\"deletedCount\":%d}", deleted);

        } catch (Exception e) {
            return "{\"status\":\"ERROR\",\"message\":\"" + escape(e.getMessage()) + "\"}";
        }
    }

    // =========================================================
    // 4. THỐNG KÊ TỔNG QUAN
    // =========================================================

    /**
     * Trả JSON thống kê hệ thống:
     * totalUsers, totalBidders, totalSellers, totalAuctions,
     * runningAuctions, finishedAuctions, canceledAuctions.
     */
    public String handleGetSystemStats() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return "{\"status\":\"ERROR\"}";

            int totalUsers    = queryCount(conn, "SELECT COUNT(*) FROM users");
            int totalBidders  = queryCount(conn, "SELECT COUNT(*) FROM users WHERE role='BIDDER'");
            int totalSellers  = queryCount(conn, "SELECT COUNT(*) FROM users WHERE role='SELLER'");
            int totalAuctions = queryCount(conn, "SELECT COUNT(*) FROM auctions");
            int running       = queryCount(conn, "SELECT COUNT(*) FROM auctions WHERE status='RUNNING'");
            int finished      = queryCount(conn, "SELECT COUNT(*) FROM auctions WHERE status='FINISHED'");
            int canceled      = queryCount(conn, "SELECT COUNT(*) FROM auctions WHERE status='CANCELED'");

            return String.format(
                    "{\"totalUsers\":%d,\"totalBidders\":%d,\"totalSellers\":%d," +
                            "\"totalAuctions\":%d,\"runningAuctions\":%d," +
                            "\"finishedAuctions\":%d,\"canceledAuctions\":%d}",
                    totalUsers, totalBidders, totalSellers,
                    totalAuctions, running, finished, canceled);

        } catch (Exception e) {
            System.err.println("[AdminService] Lỗi stats: " + e.getMessage());
            return "{\"status\":\"ERROR\"}";
        }
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private String getUsersByRole(String role, boolean includeBalance) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return "[]";

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT username, email, is_active" +
                            (includeBalance ? ", balance" : "") +
                            " FROM users WHERE role = ? ORDER BY username");
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(String.format(
                        "{\"username\":\"%s\",\"email\":\"%s\",\"isActive\":%b",
                        escape(rs.getString("username")),
                        escape(safeStr(rs, "email")),
                        rs.getBoolean("is_active")));
                if (includeBalance) {
                    try {
                        sb.append(String.format(",\"balance\":%.0f", rs.getDouble("balance")));
                    } catch (Exception ignored) { }
                }
                sb.append("}");
            }
            sb.append("]");
            return sb.toString();

        } catch (Exception e) {
            System.err.println("[AdminService] Lỗi getUsers: " + e.getMessage());
            return "[]";
        }
    }

    private String setUserActive(String username, String role, boolean active) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return "{\"status\":\"ERROR\",\"message\":\"Không kết nối được DB\"}";

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET is_active = ? WHERE username = ? AND role = ?");
            ps.setBoolean(1, active);
            ps.setString(2, username);
            ps.setString(3, role);
            int affected = ps.executeUpdate();

            String action = active ? "mở khoá" : "khoá";
            return affected > 0
                    ? String.format("{\"status\":\"OK\",\"message\":\"Đã %s tài khoản %s\"}", action, username)
                    : "{\"status\":\"ERROR\",\"message\":\"Không tìm thấy người dùng\"}";

        } catch (Exception e) {
            return "{\"status\":\"ERROR\",\"message\":\"" + escape(e.getMessage()) + "\"}";
        }
    }

    private int queryCount(Connection conn, String sql) {
        try {
            ResultSet rs = conn.prepareStatement(sql).executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }

    private String buildAuctionJson(ResultSet rs) throws Exception {
        return String.format(
                "{\"id\":\"%s\",\"itemName\":\"%s\",\"sellerName\":\"%s\"," +
                        "\"startPrice\":%.0f,\"currentPrice\":%.0f," +
                        "\"startTime\":\"%s\",\"endTime\":\"%s\"," +
                        "\"status\":\"%s\",\"winner\":\"%s\",\"imageUrl\":\"%s\"}",
                escape(rs.getString("id")),
                escape(rs.getString("itemName")),
                escape(rs.getString("sellerName")),
                rs.getDouble("startPrice"),
                rs.getDouble("currentPrice"),
                escape(safeStr(rs, "startTime")),
                escape(safeStr(rs, "endTime")),
                escape(rs.getString("status")),
                escape(safeStr(rs, "winner")),
                escape(safeStr(rs, "image_url")));
    }

    private String safeStr(ResultSet rs, String col) {
        try { Object v = rs.getObject(col); return v != null ? v.toString() : ""; }
        catch (Exception e) { return ""; }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
