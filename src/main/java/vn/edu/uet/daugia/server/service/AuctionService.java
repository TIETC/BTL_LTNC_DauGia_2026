package vn.edu.uet.daugia.server.service;

import vn.edu.uet.daugia.server.dao.DatabaseConnection;
import vn.edu.uet.daugia.shared.exception.AuctionClosedException;
import vn.edu.uet.daugia.shared.exception.InvalidBidException;
import vn.edu.uet.daugia.shared.model.*;
import vn.edu.uet.daugia.shared.model.item.Electronics;
import vn.edu.uet.daugia.shared.model.user.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionService {
    private AuctionManager manager = AuctionManager.getInstance();

    public String handlePlaceBid(String auctionId, String bidderId, double amount) {
        try {
            Auction auction = ensureAuctionLoaded(auctionId);
            if (auction == null) {
                return "{\"status\":\"ERROR\",\"message\":\"Không tìm thấy phiên đấu giá (id: "
                        + auctionId + "). Hãy tạo phiên mới hoặc kiểm tra DB.\"}";
            }

            Bidder bidder = UserManager.findBidder(bidderId);
            if (bidder == null) {
                return "{\"status\":\"ERROR\",\"message\":\"Bidder not found\"}";
            }

            // placeBid trả về true nếu phiên được gia hạn (anti-sniping)
            boolean extended = auction.placeBid(bidder, amount);
            manager.notifyObservers(auction);
            updateCurrentWinner(auctionId, bidderId, amount);

            // Nếu gia hạn → cập nhật DB + broadcast cho tất cả client
            if (extended) {
                updateEndTimeInDb(auctionId, auction.getEndTime());
                String extendPush = String.format(
                        "{\"type\":\"AUCTION_EXTENDED\",\"auctionId\":\"%s\",\"newEndTime\":\"%s\"}",
                        auctionId, auction.getEndTime().toString());
                manager.notifyAllClients(extendPush);
                System.out.printf("[ANTI-SNIPE] Broadcast gia hạn phiên %s → %s%n",
                        auctionId, auction.getEndTime());
            }

            return String.format(
                    "{\"status\":\"OK\",\"currentPrice\":%.0f,\"leader\":\"%s\"}",
                    auction.getCurrentPrice(),
                    auction.getCurrentLeader().getUsername());

        } catch (InvalidBidException e) {
            return "{\"status\":\"ERROR\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}";
        } catch (AuctionClosedException e) {
            return "{\"status\":\"ERROR\",\"message\":\"Phiên đã đóng\"}";
        } catch (Exception e) {
            System.out.println("LỖI HANDLE BID:");
            e.printStackTrace();
            return "{\"status\":\"ERROR\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private void updateCurrentWinner(String auctionId, String bidderId, double amount) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            if (connection == null) return;
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE auctions SET currentPrice = ?, winner = ? WHERE id = ?");
            ps.setDouble(1, amount);
            ps.setString(2, bidderId);
            ps.setString(3, auctionId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[BID] Lỗi cập nhật winner: " + e.getMessage());
        }
    }

    /** Cập nhật endTime mới vào DB sau khi gia hạn anti-sniping */
    private void updateEndTimeInDb(String auctionId, LocalDateTime newEndTime) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            if (connection == null) return;
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE auctions SET endTime = ? WHERE id = ?");
            ps.setString(1, newEndTime.toString());
            ps.setString(2, auctionId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[ANTI-SNIPE] Lỗi cập nhật endTime: " + e.getMessage());
        }
    }

    public Auction ensureAuctionLoaded(String auctionId) {
        Auction auction = manager.findById(auctionId);
        if (auction != null) return auction;
        return loadAuctionFromDatabase(auctionId);
    }

    public void loadAllRunningAuctionsFromDatabase() {
        try {
            Connection connection = DatabaseConnection.getConnection();
            if (connection == null) {
                System.err.println("[LOAD] Không kết nối được DB — bỏ qua nạp phiên.");
                return;
            }
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT id FROM auctions WHERE status = 'RUNNING'");
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                if (loadAuctionFromDatabase(rs.getString("id")) != null) count++;
            }
            System.out.println("[LOAD] Đã nạp " + count + " phiên RUNNING từ database.");
        } catch (Exception e) {
            System.err.println("[LOAD] Lỗi nạp phiên: " + e.getMessage());
        }
    }

    private Auction loadAuctionFromDatabase(String auctionId) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            if (connection == null) return null;

            PreparedStatement ps = connection.prepareStatement(
                    "SELECT itemName, sellerName, startPrice, currentPrice, endTime, status FROM auctions WHERE id = ?");
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("[LOAD] Không có auction id=" + auctionId + " trong DB");
                return null;
            }

            String statusStr = rs.getString("status");
            if (!"RUNNING".equalsIgnoreCase(statusStr)) {
                System.out.println("[LOAD] Phiên " + auctionId + " không RUNNING: " + statusStr);
                return null;
            }

            String itemName       = rs.getString("itemName");
            String sellerName     = rs.getString("sellerName");
            double startPrice     = rs.getDouble("startPrice");
            double currentPriceDb = rs.getDouble("currentPrice");
            LocalDateTime endTime = parseDateTime(rs.getString("endTime"));

            if (LocalDateTime.now().isAfter(endTime)) {
                System.out.println("[LOAD] Phiên " + auctionId + " đã hết hạn — đóng phiên ngay");
                finalizeExpiredAuction(auctionId);
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            Seller seller = new Seller(sellerName, "", "", "");
            Electronics item = new Electronics(
                    auctionId, itemName, "", startPrice, now, endTime, "Unknown", 0);

            Auction auction = new Auction(item, seller, startPrice, now, endTime);
            auction.startAuction();

            double bestPrice = (currentPriceDb > startPrice) ? currentPriceDb : startPrice;
            Bidder leader = null;

            PreparedStatement psBid = connection.prepareStatement(
                    "SELECT bidderId, price FROM bids WHERE auctionId = ? ORDER BY price DESC LIMIT 1");
            psBid.setString(1, auctionId);
            ResultSet rsBid = psBid.executeQuery();
            if (rsBid.next()) {
                bestPrice = Math.max(bestPrice, rsBid.getDouble("price"));
                String leaderName = rsBid.getString("bidderId");
                leader = UserManager.findBidder(leaderName);
            }

            if (bestPrice > startPrice || leader != null) {
                auction.applyRestoredState(bestPrice, leader);
            }

            manager.addAuction(auctionId, auction);
            System.out.println("[LOAD] Nạp phiên " + auctionId + " | giá hiện tại: " + auction.getCurrentPrice());
            return auction;

        } catch (Exception e) {
            System.err.println("[LOAD] Lỗi nạp phiên " + auctionId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void startAuctionCloserScheduler() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionCloser");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::closeExpiredAuctions, 5, 10, TimeUnit.SECONDS);
        System.out.println("[SCHEDULER] Đã bật bộ tự động đóng phiên (quét mỗi 10 giây).");
    }

    private void closeExpiredAuctions() {
        try {
            Connection connection = DatabaseConnection.getConnection();
            if (connection == null) return;

            PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, endTime FROM auctions WHERE status = 'RUNNING'");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String auctionId = rs.getString("id");
                LocalDateTime dbEndTime = parseDateTime(rs.getString("endTime"));

                // Ưu tiên kiểm tra endTime trong RAM (có thể đã gia hạn anti-sniping)
                Auction ramAuction = manager.findById(auctionId);
                LocalDateTime effectiveEndTime = (ramAuction != null)
                        ? ramAuction.getEndTime() : dbEndTime;

                if (LocalDateTime.now().isAfter(effectiveEndTime)) {
                    finalizeExpiredAuction(auctionId);
                }
            }
        } catch (Exception e) {
            System.err.println("[SCHEDULER] Lỗi quét phiên hết hạn: " + e.getMessage());
        }
    }

    public void finalizeExpiredAuction(String auctionId) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            if (connection == null) return;

            String winner     = null;
            double finalPrice = 0;

            PreparedStatement psBid = connection.prepareStatement(
                    "SELECT bidderId, price FROM bids WHERE auctionId = ? ORDER BY price DESC LIMIT 1");
            psBid.setString(1, auctionId);
            ResultSet rsBid = psBid.executeQuery();
            if (rsBid.next()) {
                winner     = rsBid.getString("bidderId");
                finalPrice = rsBid.getDouble("price");
            }

            Auction ramAuction = manager.findById(auctionId);
            if (ramAuction != null) {
                ramAuction.closeAuction();
                manager.removeAuction(auctionId);
            }

            String newStatus = (winner != null) ? "FINISHED" : "CANCELED";
            PreparedStatement psUpdate = connection.prepareStatement(
                    "UPDATE auctions SET status = ?, winner = ?, currentPrice = " +
                            "CASE WHEN ? > currentPrice THEN ? ELSE currentPrice END WHERE id = ?");
            psUpdate.setString(1, newStatus);
            psUpdate.setString(2, winner);
            psUpdate.setDouble(3, finalPrice);
            psUpdate.setDouble(4, finalPrice);
            psUpdate.setString(5, auctionId);

            int updated = psUpdate.executeUpdate();
            if (updated > 0) {
                System.out.printf("[CLOSE] Phiên %s → %s%s%n", auctionId, newStatus,
                        winner != null ? " | Người thắng: " + winner + " (" + finalPrice + ")" : " | Không có người thắng");

                String pushJson = String.format(
                        "{\"type\":\"AUCTION_CLOSED\",\"auctionId\":\"%s\",\"status\":\"%s\"," +
                                "\"winner\":\"%s\",\"finalPrice\":%.0f}",
                        auctionId, newStatus,
                        (winner != null ? winner : ""), finalPrice);
                manager.notifyAllClients(pushJson);
            }

        } catch (Exception e) {
            System.err.println("[CLOSE] Thử lại không có cột winner: " + e.getMessage());
            try {
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement psBid = connection.prepareStatement(
                        "SELECT bidderId FROM bids WHERE auctionId = ? LIMIT 1");
                psBid.setString(1, auctionId);
                boolean hasBid = psBid.executeQuery().next();

                String newStatus = hasBid ? "FINISHED" : "CANCELED";
                PreparedStatement psUpdate = connection.prepareStatement(
                        "UPDATE auctions SET status = ? WHERE id = ?");
                psUpdate.setString(1, newStatus);
                psUpdate.setString(2, auctionId);
                psUpdate.executeUpdate();

                Auction ramAuction = manager.findById(auctionId);
                if (ramAuction != null) {
                    ramAuction.closeAuction();
                    manager.removeAuction(auctionId);
                }
                System.out.println("[CLOSE] Phiên " + auctionId + " → " + newStatus + " (fallback)");
            } catch (Exception ex) {
                System.err.println("[CLOSE] Lỗi đóng phiên " + auctionId + ": " + ex.getMessage());
            }
        }
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null) throw new IllegalArgumentException("endTime null");
        try { return LocalDateTime.parse(raw); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(raw.replace(' ', 'T')); } catch (Exception ignored) {}
        return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String handleGetAuctions() {
        StringBuilder sb = new StringBuilder("[");
        manager.getActiveAuctions().forEach(a -> sb.append(a.toJson()).append(","));
        if (sb.length() > 1) sb.deleteCharAt(sb.length() - 1);
        return sb.append("]").toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}