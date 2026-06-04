package vn.edu.uet.daugia.server.controller;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import vn.edu.uet.daugia.server.service.AuctionManager;
import vn.edu.uet.daugia.server.service.AuctionObserver;
import vn.edu.uet.daugia.server.service.AuctionService;
import vn.edu.uet.daugia.server.admin.AdminHandler;
import vn.edu.uet.daugia.shared.model.Auction;
import vn.edu.uet.daugia.shared.model.RegisterMessage;
import vn.edu.uet.daugia.shared.model.LoginMessage;
import vn.edu.uet.daugia.shared.model.item.ItemFactoryProvider;
import vn.edu.uet.daugia.shared.model.user.Seller;
import vn.edu.uet.daugia.shared.model.user.UserFactoryProvider;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import vn.edu.uet.daugia.server.dao.DatabaseConnection;

/**
 * ClientHandler – xử lý từng kết nối TCP của một client.
 *
 * Thay đổi so với phiên bản cũ:
 *  1. Tích hợp {@link AdminHandler}: mọi lệnh ADMIN_* được uỷ quyền ra
 *     AdminHandler thay vì chen vào switch-case khổng lồ ở đây.
 *  2. Dùng {@link UserFactoryProvider} và {@link ItemFactoryProvider}
 *     thay vì gọi trực tiếp {@code new Seller(…)}, {@code new Electronics(…)}.
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private final Socket clientSocket;
    private final AuctionService auctionService;
    private final AdminHandler adminHandler;   // ← MỚI
    private PrintWriter out;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.auctionService = new AuctionService();
        this.adminHandler = new AdminHandler();  // ← MỚI
        AuctionManager.getInstance().addObserver(this);
    }

    // ===== OBSERVER: Nhận thông báo khi có bid mới =====

    @Override
    public void onNewBid(Auction auction) {
        if (out != null) {
            out.println("{\"type\":\"NEW_BID\",\"data\":" + auction.toJson() + "}");
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            AuctionManager.getInstance().addClient(out);

            String json;
            while ((json = in.readLine()) != null) {
                System.out.println("Server nhận được: " + json);

                Gson gson = new Gson();
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                String type = obj.get("type").getAsString();

                // =========================
                // ADMIN – uỷ quyền toàn bộ cho AdminHandler
                // =========================
                if (adminHandler.handle(obj, out)) {
                    continue;  // AdminHandler đã xử lý xong
                }

                // =========================
                // REGISTER
                // =========================
                if (type.equals("REGISTER")) {
                    RegisterMessage register = gson.fromJson(json, RegisterMessage.class);
                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        PreparedStatement checkStmt = connection.prepareStatement(
                                "SELECT username FROM users WHERE username = ?");
                        checkStmt.setString(1, register.getUsername());
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next()) {
                            out.println("REGISTER_FAILED:USERNAME_EXISTS");
                        } else {
                            PreparedStatement statement = connection.prepareStatement(
                                    "INSERT INTO users(username, password, role) VALUES (?, ?, ?)");
                            statement.setString(1, register.getUsername());
                            statement.setString(2, register.getPassword());
                            statement.setString(3, register.getRole());
                            statement.executeUpdate();
                            out.println("REGISTER_SUCCESS");
                        }
                    } catch (Exception e) {
                        out.println("REGISTER_FAILED:SERVER_ERROR");
                        System.err.println("Lỗi REGISTER: " + e.getMessage());
                    }
                }

                // =========================
                // LOGIN
                // =========================
                if (type.equals("LOGIN")) {
                    LoginMessage login = gson.fromJson(json, LoginMessage.class);
                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        PreparedStatement statement = connection.prepareStatement(
                                "SELECT * FROM users WHERE username=? AND password=?");
                        statement.setString(1, login.getUsername());
                        statement.setString(2, login.getPassword());
                        ResultSet resultSet = statement.executeQuery();
                        if (resultSet.next()) {
                            String role = resultSet.getString("role");
                            if (role == null || role.isEmpty()) role = "BIDDER";

                            // Kiểm tra tài khoản có bị khoá không
                            try {
                                boolean isActive = resultSet.getBoolean("is_active");
                                if (!isActive) {
                                    out.println("LOGIN_FAILED:ACCOUNT_BANNED");
                                    continue;
                                }
                            } catch (Exception ignored) { /* cột is_active chưa tồn tại */ }

                            out.println("LOGIN_SUCCESS:" + role);
                        } else {
                            out.println("LOGIN_FAILED");
                        }
                    } catch (Exception e) {
                        out.println("LOGIN_FAILED");
                        System.err.println("Lỗi LOGIN: " + e.getMessage());
                    }
                }

                // =========================
                // GET_AUCTIONS – danh sách RUNNING cho Bidder
                // =========================
                if (type.equals("GET_AUCTIONS")) {
                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        PreparedStatement statement = connection.prepareStatement(
                                "SELECT * FROM auctions WHERE status = 'RUNNING'");
                        ResultSet resultSet = statement.executeQuery();

                        StringBuilder sb = new StringBuilder("[");
                        boolean first = true;
                        while (resultSet.next()) {
                            if (!first) sb.append(",");
                            String itemId   = resultSet.getString("id");
                            String itemName = resultSet.getString("itemName");
                            String seller   = resultSet.getString("sellerName");
                            double price    = resultSet.getDouble("startPrice");
                            String endTime  = resultSet.getString("endTime");

                            String imageUrl     = safeGetString(resultSet, "image_url");
                            double maxPrice     = safeGetDouble(resultSet, "max_price");
                            double currentPrice = safeGetDouble(resultSet, "currentPrice");
                            if (currentPrice < price) currentPrice = price;

                            sb.append(String.format(
                                    "{\"itemId\":\"%s\",\"itemName\":\"%s\"," +
                                            "\"sellerName\":\"%s\",\"startPrice\":%.0f," +
                                            "\"currentPrice\":%.0f," +
                                            "\"endTime\":\"%s\"," +
                                            "\"imageUrl\":\"%s\"," +
                                            "\"maxPrice\":%.0f}",
                                    itemId, itemName, seller, price,
                                    currentPrice, endTime,
                                    escapeJson(imageUrl), maxPrice));
                            first = false;
                        }
                        sb.append("]");
                        out.println(sb.toString());

                    } catch (Exception e) {
                        out.println("[]");
                        System.err.println("Lỗi GET_AUCTIONS: " + e.getMessage());
                    }
                }

                // =========================
                // GET_MY_AUCTIONS – danh sách phiên của Seller (tất cả status)
                // =========================
                if (type.equals("GET_MY_AUCTIONS")) {
                    String sellerName = obj.get("sellerName").getAsString();
                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        PreparedStatement statement = connection.prepareStatement(
                                "SELECT * FROM auctions WHERE sellerName = ? ORDER BY endTime DESC");
                        statement.setString(1, sellerName);
                        ResultSet rs = statement.executeQuery();

                        StringBuilder sb = new StringBuilder("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) sb.append(",");
                            String itemId       = rs.getString("id");
                            String itemName     = rs.getString("itemName");
                            double startPrice   = rs.getDouble("startPrice");
                            double currentPrice = rs.getDouble("currentPrice");
                            if (currentPrice < startPrice) currentPrice = startPrice;
                            String endTime      = rs.getString("endTime");
                            String startTime    = rs.getString("startTime") != null
                                    ? rs.getString("startTime") : "";
                            String status       = rs.getString("status");
                            String winner       = safeGetString(rs, "winner");
                            String imageUrl     = safeGetString(rs, "image_url");
                            double maxPrice     = safeGetDouble(rs, "max_price");
                            String description  = safeGetString(rs, "description");

                            String leader = "";
                            if ("RUNNING".equals(status)) {
                                Auction auction = AuctionManager.getInstance().findById(itemId);
                                if (auction != null && auction.getCurrentLeader() != null) {
                                    leader = auction.getCurrentLeader().getUsername();
                                    currentPrice = auction.getCurrentPrice();
                                } else {
                                    try {
                                        PreparedStatement psBid = connection.prepareStatement(
                                                "SELECT bidderId, price FROM bids WHERE auctionId = ? ORDER BY price DESC LIMIT 1");
                                        psBid.setString(1, itemId);
                                        ResultSet rsBid = psBid.executeQuery();
                                        if (rsBid.next()) {
                                            leader = rsBid.getString("bidderId");
                                            currentPrice = Math.max(currentPrice, rsBid.getDouble("price"));
                                        }
                                    } catch (Exception ignored) {}
                                }
                            } else if ("FINISHED".equals(status)) {
                                leader = winner != null ? winner : "";
                            }

                            sb.append(String.format(
                                    "{\"itemId\":\"%s\",\"itemName\":\"%s\"," +
                                            "\"startPrice\":%.0f,\"currentPrice\":%.0f," +
                                            "\"maxPrice\":%.0f," +
                                            "\"startTime\":\"%s\",\"endTime\":\"%s\"," +
                                            "\"status\":\"%s\",\"leader\":\"%s\"," +
                                            "\"winner\":\"%s\",\"imageUrl\":\"%s\"," +
                                            "\"description\":\"%s\"}",
                                    escapeJson(itemId), escapeJson(itemName),
                                    startPrice, currentPrice, maxPrice,
                                    escapeJson(startTime), escapeJson(endTime),
                                    escapeJson(status), escapeJson(leader),
                                    escapeJson(winner), escapeJson(imageUrl),
                                    escapeJson(description)));
                            first = false;
                        }
                        sb.append("]");
                        out.println(sb.toString());

                    } catch (Exception e) {
                        out.println("[]");
                        System.err.println("Lỗi GET_MY_AUCTIONS: " + e.getMessage());
                    }
                }

                // =========================
                // GET_AUCTION_STATE
                // =========================
                if (type.equals("GET_AUCTION_STATE")) {
                    String auctionId = obj.get("auctionId").getAsString();
                    try {
                        Auction auction = auctionService.ensureAuctionLoaded(auctionId);
                        double currentPrice = auction != null
                                ? auction.getCurrentPrice()
                                : resolveCurrentPrice(auctionId);
                        String leader = "";
                        if (auction != null && auction.getCurrentLeader() != null) {
                            leader = auction.getCurrentLeader().getUsername();
                        }
                        out.println(String.format(
                                "{\"status\":\"OK\",\"itemId\":\"%s\",\"currentPrice\":%.0f,\"leader\":\"%s\"}",
                                auctionId, currentPrice, leader));
                    } catch (Exception e) {
                        out.println("{\"status\":\"ERROR\",\"message\":\"Không đọc được trạng thái phiên\"}");
                    }
                }

                // =========================
                // BID – validate + kiểm tra giá mua đứt
                // =========================
                if (type.equals("BID")) {
                    String auctionId = obj.get("auctionId").getAsString();
                    String bidderId  = obj.get("bidderId").getAsString();
                    double price     = obj.get("price").getAsDouble();

                    String responseJson = auctionService.handlePlaceBid(auctionId, bidderId, price);
                    try {
                        JsonObject resp = new Gson().fromJson(responseJson, JsonObject.class);
                        if ("OK".equals(resp.get("status").getAsString())) {
                            insertBidRecord(auctionId, bidderId, price);
                            updateWinnerInDb(auctionId, bidderId, price);

                            double maxPrice = getMaxPrice(auctionId);
                            if (maxPrice > 0 && price >= maxPrice) {
                                System.out.println("[BUYOUT] " + bidderId + " mua đứt phiên " + auctionId + " với giá " + price);
                                out.println(String.format(
                                        "{\"status\":\"BUYOUT\",\"currentPrice\":%.0f,\"leader\":\"%s\"," +
                                                "\"message\":\"Chúc mừng! Bạn đã mua đứt sản phẩm!\"}",
                                        price, escapeJson(bidderId)));
                                auctionService.finalizeExpiredAuction(auctionId);
                                continue;
                            }
                        }
                    } catch (Exception dbEx) {
                        System.err.println("Lỗi lưu bid: " + dbEx.getMessage());
                    }
                    out.println(responseJson);
                }

                // =========================
                // CREATE_AUCTION  ← dùng ItemFactoryProvider thay vì new Electronics(…)
                // =========================
                if (type.equals("CREATE_AUCTION")) {
                    String itemId       = obj.get("itemId").getAsString();
                    String itemName     = obj.get("itemName").getAsString();
                    String desc         = obj.has("description") ? obj.get("description").getAsString() : "";
                    double startPrice   = obj.get("startPrice").getAsDouble();
                    String sellerName   = obj.get("sellerName").getAsString();
                    int durationMinutes = obj.get("durationMinutes").getAsInt();
                    String imageUrl     = obj.has("imageUrl") && !obj.get("imageUrl").isJsonNull()
                            ? obj.get("imageUrl").getAsString() : "";
                    double maxPrice     = obj.has("maxPrice") && !obj.get("maxPrice").isJsonNull()
                            ? obj.get("maxPrice").getAsDouble() : 0;

                    // Loại item từ JSON (mặc định ELECTRONICS nếu không có)
                    String itemTypeStr = obj.has("itemType") ? obj.get("itemType").getAsString() : "ELECTRONICS";
                    String extra       = obj.has("extra")    ? obj.get("extra").getAsString()    : "";

                    LocalDateTime now     = LocalDateTime.now();
                    LocalDateTime endTime = now.plusMinutes(durationMinutes);

                    // ← Factory Method: tạo Item đúng loại
                    var item = ItemFactoryProvider.createFromString(
                            itemTypeStr, itemId, itemName, desc, startPrice, now, endTime, extra);

                    // ← Factory Method: tạo Seller
                    Seller seller = UserFactoryProvider.createSeller(sellerName, "", "", "");

                    Auction auction = new Auction(item, seller, startPrice, now, endTime);
                    auction.startAuction();
                    AuctionManager.getInstance().addAuction(itemId, auction);

                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        PreparedStatement statement = connection.prepareStatement(
                                "INSERT INTO auctions(id, itemName, description, sellerName, startPrice, " +
                                        "startTime, endTime, status, image_url, max_price) " +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, 'RUNNING', ?, ?)");
                        statement.setString(1, itemId);
                        statement.setString(2, itemName);
                        statement.setString(3, desc);
                        statement.setString(4, sellerName);
                        statement.setDouble(5, startPrice);
                        statement.setString(6, now.toString());
                        statement.setString(7, endTime.toString());
                        statement.setString(8, imageUrl);
                        statement.setDouble(9, maxPrice);
                        statement.executeUpdate();
                    } catch (Exception dbEx) {
                        System.err.println("[WARN] INSERT đầy đủ thất bại, thử fallback: " + dbEx.getMessage());
                        try {
                            Connection connection = DatabaseConnection.getConnection();
                            PreparedStatement stmt = connection.prepareStatement(
                                    "INSERT INTO auctions(id, itemName, sellerName, startPrice, endTime, status) " +
                                            "VALUES (?, ?, ?, ?, ?, 'RUNNING')");
                            stmt.setString(1, itemId);
                            stmt.setString(2, itemName);
                            stmt.setString(3, sellerName);
                            stmt.setDouble(4, startPrice);
                            stmt.setString(5, endTime.toString());
                            stmt.executeUpdate();
                        } catch (Exception ex2) {
                            out.println("{\"status\":\"ERROR\",\"message\":\"Lỗi tạo phiên: " + ex2.getMessage() + "\"}");
                            continue;
                        }
                    }

                    out.println("{\"status\":\"OK\",\"message\":\"Tạo phiên đấu giá thành công\"}");

                    String pushJson = String.format(
                            "{\"type\":\"NEW_AUCTION\"," +
                                    "\"itemId\":\"%s\",\"itemName\":\"%s\"," +
                                    "\"sellerName\":\"%s\",\"startPrice\":%.0f," +
                                    "\"currentPrice\":%.0f," +
                                    "\"endTime\":\"%s\"," +
                                    "\"imageUrl\":\"%s\"," +
                                    "\"maxPrice\":%.0f}",
                            escapeJson(itemId), escapeJson(itemName),
                            escapeJson(sellerName), startPrice, startPrice,
                            endTime.toString(), escapeJson(imageUrl), maxPrice);
                    AuctionManager.getInstance().notifyAllClients(pushJson);
                }

                // =========================
                // UPDATE_AUCTION
                // =========================
                if (type.equals("UPDATE_AUCTION")) {
                    String itemId   = obj.get("itemId").getAsString();
                    String itemName = obj.has("itemName") ? obj.get("itemName").getAsString() : null;
                    String desc     = obj.has("description") ? obj.get("description").getAsString() : null;
                    String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : null;
                    double maxPrice = obj.has("maxPrice") ? obj.get("maxPrice").getAsDouble() : -1;

                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        PreparedStatement psCheck = connection.prepareStatement(
                                "SELECT status FROM auctions WHERE id = ?");
                        psCheck.setString(1, itemId);
                        ResultSet rsCheck = psCheck.executeQuery();
                        if (!rsCheck.next()) {
                            out.println("{\"status\":\"ERROR\",\"message\":\"Không tìm thấy phiên\"}");
                            continue;
                        }
                        String currentStatus = rsCheck.getString("status");
                        if ("FINISHED".equals(currentStatus) || "CANCELED".equals(currentStatus)) {
                            out.println("{\"status\":\"ERROR\",\"message\":\"Không thể sửa phiên đã kết thúc\"}");
                            continue;
                        }

                        StringBuilder sqlUpdate = new StringBuilder("UPDATE auctions SET ");
                        java.util.List<Object> params = new java.util.ArrayList<>();
                        if (itemName != null) { sqlUpdate.append("itemName = ?, "); params.add(itemName); }
                        if (desc != null)     { sqlUpdate.append("description = ?, "); params.add(desc); }
                        if (imageUrl != null) { sqlUpdate.append("image_url = ?, "); params.add(imageUrl); }
                        if (maxPrice >= 0)    { sqlUpdate.append("max_price = ?, "); params.add(maxPrice); }

                        if (params.isEmpty()) {
                            out.println("{\"status\":\"OK\",\"message\":\"Không có gì thay đổi\"}");
                            continue;
                        }

                        String sql = sqlUpdate.toString();
                        sql = sql.substring(0, sql.lastIndexOf(",")) + " WHERE id = ?";
                        params.add(itemId);

                        PreparedStatement psUpdate = connection.prepareStatement(sql);
                        for (int i = 0; i < params.size(); i++) {
                            Object p = params.get(i);
                            if (p instanceof String) psUpdate.setString(i + 1, (String) p);
                            else if (p instanceof Double) psUpdate.setDouble(i + 1, (Double) p);
                        }
                        int affected = psUpdate.executeUpdate();

                        if (affected > 0) {
                            if ("RUNNING".equals(currentStatus) && itemName != null) {
                                Auction ramAuction = AuctionManager.getInstance().findById(itemId);
                                if (ramAuction != null && ramAuction.getItem() != null) {
                                    ramAuction.getItem().setName(itemName);
                                }
                            }
                            out.println("{\"status\":\"OK\",\"message\":\"Cập nhật thành công\"}");
                        } else {
                            out.println("{\"status\":\"ERROR\",\"message\":\"Không tìm thấy phiên để cập nhật\"}");
                        }

                    } catch (Exception e) {
                        out.println("{\"status\":\"ERROR\",\"message\":\"Lỗi cập nhật: " + escapeJson(e.getMessage()) + "\"}");
                        System.err.println("Lỗi UPDATE_AUCTION: " + e.getMessage());
                    }
                }

                // =========================
                // DELETE_AUCTION (Seller xóa phiên của mình)
                // =========================
                if (type.equals("DELETE_AUCTION")) {
                    String itemId = obj.get("itemId").getAsString();
                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        PreparedStatement psCheck = connection.prepareStatement(
                                "SELECT status FROM auctions WHERE id = ?");
                        psCheck.setString(1, itemId);
                        ResultSet rsCheck = psCheck.executeQuery();
                        if (!rsCheck.next()) {
                            out.println("{\"status\":\"ERROR\",\"message\":\"Không tìm thấy phiên\"}");
                            continue;
                        }
                        String currentStatus = rsCheck.getString("status");
                        if ("RUNNING".equals(currentStatus)) {
                            Auction ramAuction = AuctionManager.getInstance().findById(itemId);
                            if (ramAuction != null) {
                                ramAuction.closeAuction();
                                AuctionManager.getInstance().removeAuction(itemId);
                            }
                        }

                        PreparedStatement psDelBids = connection.prepareStatement(
                                "DELETE FROM bids WHERE auctionId = ?");
                        psDelBids.setString(1, itemId);
                        psDelBids.executeUpdate();

                        PreparedStatement psDelete = connection.prepareStatement(
                                "DELETE FROM auctions WHERE id = ?");
                        psDelete.setString(1, itemId);
                        int affected = psDelete.executeUpdate();

                        if (affected > 0) {
                            out.println("{\"status\":\"OK\",\"message\":\"Đã xóa phiên\"}");
                            String pushJson = String.format(
                                    "{\"type\":\"AUCTION_DELETED\",\"auctionId\":\"%s\"}",
                                    escapeJson(itemId));
                            AuctionManager.getInstance().notifyAllClients(pushJson);
                        } else {
                            out.println("{\"status\":\"ERROR\",\"message\":\"Xóa thất bại\"}");
                        }

                    } catch (Exception e) {
                        out.println("{\"status\":\"ERROR\",\"message\":\"Lỗi xóa: " + escapeJson(e.getMessage()) + "\"}");
                        System.err.println("Lỗi DELETE_AUCTION: " + e.getMessage());
                    }
                }

                // =========================
                // GET_BID_HISTORY
                // =========================
                if (type.equals("GET_BID_HISTORY")) {
                    String auctionId = obj.get("auctionId").getAsString();
                    out.println(buildBidHistoryJson(auctionId));
                }

                // =========================
                // GET_BIDS
                // =========================
                if (type.equals("GET_BIDS")) {
                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        PreparedStatement statement = connection.prepareStatement("SELECT * FROM bids");
                        ResultSet resultSet = statement.executeQuery();
                        StringBuilder response = new StringBuilder();
                        while (resultSet.next()) {
                            response.append(resultSet.getInt("id")).append(" | ")
                                    .append(resultSet.getString("auctionId")).append(" | ")
                                    .append(resultSet.getString("bidderId")).append(" | ")
                                    .append(resultSet.getDouble("price")).append("\n");
                        }
                        out.println(response.toString());
                    } catch (Exception e) {
                        out.println("Lỗi GET_BIDS: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Client đã ngắt kết nối!");
        } finally {
            AuctionManager.getInstance().removeClient(out);
            AuctionManager.getInstance().removeObserver(this);
        }
    }

    // ===== HELPER: Lưu bid vào DB =====

    private void insertBidRecord(String auctionId, String bidderId, double price) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            LocalDateTime now = LocalDateTime.now();
            try {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO bids(auctionId, bidderId, price, bidTime) VALUES (?, ?, ?, ?)");
                statement.setString(1, auctionId);
                statement.setString(2, bidderId);
                statement.setDouble(3, price);
                statement.setTimestamp(4, Timestamp.valueOf(now));
                statement.executeUpdate();
            } catch (Exception colEx) {
                PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO bids(auctionId, bidderId, price) VALUES (?, ?, ?)");
                stmt.setString(1, auctionId);
                stmt.setString(2, bidderId);
                stmt.setDouble(3, price);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Lỗi lưu bid: " + e.getMessage());
        }
    }

    private void updateWinnerInDb(String auctionId, String bidderId, double price) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE auctions SET currentPrice = ?, winner = ? " +
                            "WHERE id = ? AND (currentPrice IS NULL OR currentPrice < ?)");
            ps.setDouble(1, price);
            ps.setString(2, bidderId);
            ps.setString(3, auctionId);
            ps.setDouble(4, price);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Lỗi cập nhật winner: " + e.getMessage());
        }
    }

    private double getMaxPrice(String auctionId) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT max_price FROM auctions WHERE id = ?");
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("max_price");
        } catch (Exception e) {
            System.err.println("Lỗi getMaxPrice: " + e.getMessage());
        }
        return 0;
    }

    private double resolveCurrentPrice(String auctionId) throws Exception {
        Auction auction = AuctionManager.getInstance().findById(auctionId);
        if (auction != null) return auction.getCurrentPrice();

        Connection connection = DatabaseConnection.getConnection();
        double startPrice = 0;
        PreparedStatement psStart = connection.prepareStatement(
                "SELECT startPrice FROM auctions WHERE id = ?");
        psStart.setString(1, auctionId);
        ResultSet rsStart = psStart.executeQuery();
        if (rsStart.next()) startPrice = rsStart.getDouble("startPrice");

        double maxBid = startPrice;
        PreparedStatement psMax = connection.prepareStatement(
                "SELECT MAX(price) AS maxPrice FROM bids WHERE auctionId = ?");
        psMax.setString(1, auctionId);
        ResultSet rsMax = psMax.executeQuery();
        if (rsMax.next() && rsMax.getObject("maxPrice") != null) {
            maxBid = Math.max(startPrice, rsMax.getDouble("maxPrice"));
        }
        return maxBid;
    }

    private String buildBidHistoryJson(String auctionId) {
        try {
            Connection connection = DatabaseConnection.getConnection();
            double startPrice = 0;
            PreparedStatement psStart = connection.prepareStatement(
                    "SELECT startPrice FROM auctions WHERE id = ?");
            psStart.setString(1, auctionId);
            ResultSet rsStart = psStart.executeQuery();
            if (rsStart.next()) startPrice = rsStart.getDouble("startPrice");

            StringBuilder items = new StringBuilder();
            boolean first = true;
            ResultSet rs;
            boolean hasBidTime = true;
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT bidderId, price, bidTime FROM bids WHERE auctionId = ? ORDER BY bidTime ASC, id ASC");
                ps.setString(1, auctionId);
                rs = ps.executeQuery();
            } catch (Exception ex) {
                hasBidTime = false;
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT bidderId, price FROM bids WHERE auctionId = ? ORDER BY id ASC");
                ps.setString(1, auctionId);
                rs = ps.executeQuery();
            }
            while (rs.next()) {
                if (!first) items.append(",");
                first = false;
                String bidder  = rs.getString("bidderId");
                double price   = rs.getDouble("price");
                String bidTime = hasBidTime
                        ? formatBidTime(rs, "bidTime")
                        : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                items.append(String.format(
                        "{\"bidderId\":\"%s\",\"price\":%.0f,\"bidTime\":\"%s\"}",
                        escapeJson(bidder), price, escapeJson(bidTime)));
            }
            return String.format(
                    "{\"status\":\"OK\",\"auctionId\":\"%s\",\"startPrice\":%.0f,\"history\":[%s]}",
                    escapeJson(auctionId), startPrice, items);
        } catch (Exception e) {
            System.err.println("Lỗi GET_BID_HISTORY: " + e.getMessage());
            return "{\"status\":\"ERROR\",\"message\":\"Không đọc được lịch sử đặt giá\"}";
        }
    }

    private String formatBidTime(ResultSet rs, String column) throws Exception {
        try {
            Timestamp ts = rs.getTimestamp(column);
            if (ts != null) return ts.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String safeGetString(ResultSet rs, String column) {
        try { Object val = rs.getObject(column); return val != null ? val.toString() : ""; }
        catch (Exception e) { return ""; }
    }

    private double safeGetDouble(ResultSet rs, String column) {
        try { Object val = rs.getObject(column); return val != null ? Double.parseDouble(val.toString()) : 0; }
        catch (Exception e) { return 0; }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
