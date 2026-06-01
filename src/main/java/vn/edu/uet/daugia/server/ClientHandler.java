package vn.edu.uet.daugia.server;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import vn.edu.uet.daugia.shared.model.Auction;
import vn.edu.uet.daugia.shared.model.RegisterMessage;
import vn.edu.uet.daugia.shared.model.LoginMessage;
import vn.edu.uet.daugia.shared.model.item.Electronics;
import vn.edu.uet.daugia.shared.model.user.Seller;
import java.time.LocalDateTime;
import java.util.List;

import vn.edu.uet.daugia.database.DatabaseConnection;

public class ClientHandler implements Runnable, AuctionObserver {

    private Socket clientSocket;
    private AuctionService auctionService;
    private PrintWriter out;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.auctionService = new AuctionService();
        AuctionManager.getInstance().addObserver(this);
    }

    // ===== OBSERVER: Nhận thông báo khi có bid mới (GIỮ NGUYÊN) =====

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

            // Đăng ký client này vào AuctionManager (GIỮ NGUYÊN)
            AuctionManager.getInstance().addClient(out);

            String json;
            while ((json = in.readLine()) != null) {
                System.out.println("Server nhận được: " + json);

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                String type = jsonObject.get("type").getAsString();

                // =========================
                // REGISTER (GIỮ NGUYÊN)
                // =========================
                if (type.equals("REGISTER")) {
                    RegisterMessage register = gson.fromJson(json, RegisterMessage.class);
                    System.out.println("Đang xử lý REGISTER...");

                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        String checkSql = "SELECT username FROM users WHERE username = ?";
                        PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                        checkStmt.setString(1, register.getUsername());
                        ResultSet rs = checkStmt.executeQuery();

                        if (rs.next()) {
                            out.println("REGISTER_FAILED:USERNAME_EXISTS");
                            System.out.println("Username đã tồn tại: " + register.getUsername());
                        } else {
                            String sql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";
                            PreparedStatement statement = connection.prepareStatement(sql);
                            statement.setString(1, register.getUsername());
                            statement.setString(2, register.getPassword());
                            statement.setString(3, register.getRole());
                            statement.executeUpdate();
                            out.println("REGISTER_SUCCESS");
                            System.out.println("Đã lưu user: " + register.getUsername());
                        }
                    } catch (Exception e) {
                        out.println("REGISTER_FAILED:SERVER_ERROR");
                        System.out.println("Lỗi REGISTER: " + e.getMessage());
                    }
                }

                // =========================
                // LOGIN (GIỮ NGUYÊN)
                // =========================
                if (type.equals("LOGIN")) {
                    LoginMessage login = gson.fromJson(json, LoginMessage.class);
                    Connection connection = DatabaseConnection.getConnection();
                    String sql = "SELECT * FROM users WHERE username=? AND password=?";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, login.getUsername());
                    statement.setString(2, login.getPassword());
                    ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        String role = resultSet.getString("role");
                        if (role == null || role.isEmpty()) role = "BIDDER";
                        out.println("LOGIN_SUCCESS:" + role);
                        System.out.println("Đăng nhập thành công! Role: " + role);
                    } else {
                        out.println("LOGIN_FAILED");
                        System.out.println("Sai tài khoản hoặc mật khẩu!");
                    }
                }

                // =========================
                // GET_AUCTIONS — SỬA: thêm imageUrl và maxPrice vào JSON trả về
                // =========================
                if (type.equals("GET_AUCTIONS")) {
                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        // SỬA: SELECT thêm cột image_url và max_price
                        String sql = "SELECT * FROM auctions WHERE status = 'RUNNING'";
                        PreparedStatement statement = connection.prepareStatement(sql);
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

                            // MỚI: Lấy imageUrl và maxPrice từ DB
                            // Dùng getObject để tránh crash nếu cột chưa có
                            String imageUrl = "";
                            try {
                                Object imgObj = resultSet.getObject("image_url");
                                if (imgObj != null) imageUrl = imgObj.toString();
                            } catch (Exception ignored) {}

                            double maxPrice = 0;
                            try {
                                Object maxObj = resultSet.getObject("max_price");
                                if (maxObj != null) maxPrice = Double.parseDouble(maxObj.toString());
                            } catch (Exception ignored) {}

                            // MỚI: thêm imageUrl và maxPrice vào JSON
                            sb.append(String.format(
                                    "{\"itemId\":\"%s\",\"itemName\":\"%s\"," +
                                            "\"sellerName\":\"%s\",\"startPrice\":%.0f," +
                                            "\"endTime\":\"%s\"," +
                                            "\"imageUrl\":\"%s\"," +
                                            "\"maxPrice\":%.0f}",
                                    itemId, itemName, seller, price, endTime,
                                    imageUrl, maxPrice
                            ));
                            first = false;
                        }
                        sb.append("]");

                        out.println(sb.toString());
                        System.out.println("Đã gửi danh sách auction: " + sb);

                    } catch (Exception e) {
                        out.println("[]");
                        System.out.println("Lỗi GET_AUCTIONS: " + e.getMessage());
                    }
                }

                // =========================
                // BID (GIỮ NGUYÊN)
                // =========================
                if (type.equals("BID")) {
                    String auctionId = jsonObject.get("auctionId").getAsString();
                    String bidderId  = jsonObject.get("bidderId").getAsString();
                    double price     = jsonObject.get("price").getAsDouble();

                    Connection connection = DatabaseConnection.getConnection();
                    String sql = "INSERT INTO bids(auctionId, bidderId, price) VALUES (?, ?, ?)";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, auctionId);
                    statement.setString(2, bidderId);
                    statement.setDouble(3, price);
                    statement.executeUpdate();
                    System.out.println("Đã lưu bid vào database!");

                    String responseJson = auctionService.handlePlaceBid(auctionId, bidderId, price);
                    out.println(responseJson);
                }

                // =========================
                // CREATE_AUCTION — SỬA: đọc và lưu imageUrl, maxPrice
                // =========================
                if (type.equals("CREATE_AUCTION")) {
                    String itemId       = jsonObject.get("itemId").getAsString();
                    String itemName     = jsonObject.get("itemName").getAsString();
                    String desc         = jsonObject.get("description").getAsString();
                    double startPrice   = jsonObject.get("startPrice").getAsDouble();
                    String sellerName   = jsonObject.get("sellerName").getAsString();
                    int durationMinutes = jsonObject.get("durationMinutes").getAsInt();

                    // MỚI: Đọc imageUrl (tùy chọn, mặc định "")
                    String imageUrl = "";
                    if (jsonObject.has("imageUrl")
                            && !jsonObject.get("imageUrl").isJsonNull()) {
                        imageUrl = jsonObject.get("imageUrl").getAsString();
                    }

                    // MỚI: Đọc maxPrice (tùy chọn, mặc định 0)
                    double maxPrice = 0;
                    if (jsonObject.has("maxPrice")
                            && !jsonObject.get("maxPrice").isJsonNull()) {
                        maxPrice = jsonObject.get("maxPrice").getAsDouble();
                    }

                    System.out.println("Đang tạo phiên đấu giá: " + itemId + " - " + itemName);

                    LocalDateTime now     = LocalDateTime.now();
                    LocalDateTime endTime = now.plusMinutes(durationMinutes);

                    Seller seller = new Seller(sellerName, "", "", "");
                    Electronics item = new Electronics(
                            itemId, itemName, desc, startPrice,
                            now, endTime, "Unknown", 0);

                    Auction auction = new Auction(item, seller, startPrice, now, endTime);
                    auction.startAuction();
                    AuctionManager.getInstance().addAuction(itemId, auction);

                    // SỬA: Lưu DB thêm cột image_url và max_price
                    // (Cần chạy câu ALTER TABLE bên dưới trước nếu cột chưa tồn tại)
                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        String sql =
                                "INSERT INTO auctions(id, itemName, sellerName, startPrice, " +
                                        "endTime, status, image_url, max_price) " +
                                        "VALUES (?, ?, ?, ?, ?, 'RUNNING', ?, ?)";
                        PreparedStatement statement = connection.prepareStatement(sql);
                        statement.setString(1, itemId);
                        statement.setString(2, itemName);
                        statement.setString(3, sellerName);
                        statement.setDouble(4, startPrice);
                        statement.setString(5, endTime.toString());
                        statement.setString(6, imageUrl);   // MỚI
                        statement.setDouble(7, maxPrice);   // MỚI
                        statement.executeUpdate();
                    } catch (Exception dbEx) {
                        // Nếu cột chưa có (chưa ALTER TABLE) → fallback INSERT cũ
                        System.err.println("[WARN] Chưa có cột image_url/max_price, dùng INSERT cũ: "
                                + dbEx.getMessage());
                        Connection connection = DatabaseConnection.getConnection();
                        String sqlFallback =
                                "INSERT INTO auctions(id, itemName, sellerName, startPrice, endTime, status) " +
                                        "VALUES (?, ?, ?, ?, ?, 'RUNNING')";
                        PreparedStatement stmtFallback = connection.prepareStatement(sqlFallback);
                        stmtFallback.setString(1, itemId);
                        stmtFallback.setString(2, itemName);
                        stmtFallback.setString(3, sellerName);
                        stmtFallback.setDouble(4, startPrice);
                        stmtFallback.setString(5, endTime.toString());
                        stmtFallback.executeUpdate();
                    }

                    System.out.println("✅ Đã tạo phiên: " + itemId);

                    // Phản hồi cho Seller (GIỮ NGUYÊN)
                    out.println("{\"status\":\"OK\",\"message\":\"Tạo phiên đấu giá thành công\"}");

                    // Push NEW_AUCTION tới TẤT CẢ client — SỬA: thêm imageUrl, maxPrice
                    String pushJson = String.format(
                            "{\"type\":\"NEW_AUCTION\"," +
                                    "\"itemId\":\"%s\",\"itemName\":\"%s\"," +
                                    "\"sellerName\":\"%s\",\"startPrice\":%.0f," +
                                    "\"endTime\":\"%s\"," +
                                    "\"imageUrl\":\"%s\"," +
                                    "\"maxPrice\":%.0f}",
                            itemId, itemName, sellerName, startPrice,
                            endTime.toString(), imageUrl, maxPrice
                    );
                    AuctionManager.getInstance().notifyAllClients(pushJson);
                }

                // =========================
                // GET_BIDS (GIỮ NGUYÊN)
                // =========================
                if (type.equals("GET_BIDS")) {
                    Connection connection = DatabaseConnection.getConnection();
                    String sql = "SELECT * FROM bids";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery();

                    StringBuilder response = new StringBuilder();
                    while (resultSet.next()) {
                        int id           = resultSet.getInt("id");
                        String auctionId = resultSet.getString("auctionId");
                        String bidderId  = resultSet.getString("bidderId");
                        double price     = resultSet.getDouble("price");
                        response.append(id).append(" | ").append(auctionId)
                                .append(" | ").append(bidderId)
                                .append(" | ").append(price).append("\n");
                    }
                    out.println(response.toString());
                }
            }

        } catch (Exception e) {
            System.out.println("Client đã ngắt kết nối!");
        } finally {
            // Xóa khỏi danh sách push khi ngắt kết nối (GIỮ NGUYÊN)
            AuctionManager.getInstance().removeClient(out);
            AuctionManager.getInstance().removeObserver(this);
        }
    }
}