package vn.edu.uet.daugia.server;

import java.net.Socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
// ĐÃ XÓA: import java.sql.ResultSet trùng lặp ở đây

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import vn.edu.uet.daugia.shared.model.Auction;
import vn.edu.uet.daugia.shared.model.RegisterMessage;
import vn.edu.uet.daugia.shared.model.LoginMessage;

// THÊM MỚI: cần để tạo Auction trong CREATE_AUCTION
import vn.edu.uet.daugia.shared.model.item.Electronics;
import vn.edu.uet.daugia.shared.model.user.Seller;
import java.time.LocalDateTime;

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

    @Override
    public void onNewBid(Auction auction) {
        if (out != null) {
            out.println(
                    "{\"type\":\"NEW_BID\",\"data\":" + auction.toJson() + "}"
            );
        }
    }

    @Override
    public void run() {

        try {

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream())
                    );

            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String json;

            while ((json = in.readLine()) != null) {

                System.out.println("Server nhận được: " + json);

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                String type = jsonObject.get("type").getAsString();

                // =========================
                // REGISTER
                // =========================

                if (type.equals("REGISTER")) {
                    RegisterMessage register = gson.fromJson(json, RegisterMessage.class);
                    System.out.println("Đang xử lý REGISTER...");

                    try {
                        Connection connection = DatabaseConnection.getConnection();

                        // Kiểm tra username đã tồn tại chưa
                        String checkSql = "SELECT username FROM users WHERE username = ?";
                        PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                        checkStmt.setString(1, register.getUsername());
                        ResultSet rs = checkStmt.executeQuery();

                        if (rs.next()) {
                            // Username đã tồn tại
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
                // LOGIN
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
                // BID
                // =========================

                if (type.equals("BID")) {

                    String auctionId = jsonObject.get("auctionId").getAsString();
                    String bidderId  = jsonObject.get("bidderId").getAsString();
                    double price     = jsonObject.get("price").getAsDouble();

                    // Lưu database
                    Connection connection = DatabaseConnection.getConnection();
                    String sql = "INSERT INTO bids(auctionId, bidderId, price) VALUES (?, ?, ?)";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, auctionId);
                    statement.setString(2, bidderId);
                    statement.setDouble(3, price);
                    statement.executeUpdate();
                    System.out.println("Đã lưu bid vào database!");

                    // Xử lý logic realtime
                    String responseJson = auctionService.handlePlaceBid(auctionId, bidderId, price);
                    out.println(responseJson);
                }

                // =========================
                // CREATE_AUCTION (THÊM MỚI)
                // Seller bấm "Thêm +" → Client gửi lên đây
                // =========================

                if (type.equals("CREATE_AUCTION")) {

                    // 1. Đọc toàn bộ thông tin từ JSON
                    String itemId    = jsonObject.get("itemId").getAsString();
                    String itemName  = jsonObject.get("itemName").getAsString();
                    String desc      = jsonObject.get("description").getAsString();
                    double startPrice = jsonObject.get("startPrice").getAsDouble();
                    String sellerName = jsonObject.get("sellerName").getAsString();
                    int durationMinutes = jsonObject.get("durationMinutes").getAsInt();

                    System.out.println("Đang tạo phiên đấu giá: " + itemId + " - " + itemName);

                    // 2. Tạo object
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime endTime = now.plusMinutes(durationMinutes);

                    Seller seller = new Seller(sellerName, "", "", "");
                    Electronics item = new Electronics(
                            itemId, itemName, desc, startPrice,
                            now, endTime, "Unknown", 0
                    );

                    // 3. Tạo phiên và bắt đầu ngay
                    Auction auction = new Auction(item, seller, startPrice, now, endTime);
                    auction.startAuction(); // RUNNING → mới nhận được BID

                    // 4. Lưu vào AuctionManager với key = itemId
                    //    (để khi Client gửi BID với auctionId="SP01" thì tìm được)
                    AuctionManager.getInstance().addAuction(itemId, auction);

                    // 5. Lưu vào database
                    Connection connection = DatabaseConnection.getConnection();
                    String sql = "INSERT INTO auctions(id, itemName, sellerName, startPrice, endTime) " +
                            "VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setString(1, itemId);
                    statement.setString(2, itemName);
                    statement.setString(3, sellerName);
                    statement.setDouble(4, startPrice);
                    statement.setString(5, endTime.toString());
                    statement.executeUpdate();

                    System.out.println("✅ Đã tạo phiên: " + itemId + " | Kết thúc sau " + durationMinutes + " phút");
                    out.println("{\"status\":\"OK\",\"message\":\"Tạo phiên đấu giá thành công\"}");
                }

                // =========================
                // GET_BIDS
                // =========================

                if (type.equals("GET_BIDS")) {

                    Connection connection = DatabaseConnection.getConnection();
                    String sql = "SELECT * FROM bids";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery();

                    StringBuilder response = new StringBuilder();
                    while (resultSet.next()) {
                        int id         = resultSet.getInt("id");
                        String auctionId = resultSet.getString("auctionId");
                        String bidderId  = resultSet.getString("bidderId");
                        double price     = resultSet.getDouble("price");
                        response.append(id + " | " + auctionId + " | " + bidderId + " | " + price + "\n");
                    }

                    out.println(response.toString());
                }
            }

        } catch (Exception e) {
            System.out.println("Client đã ngắt kết nối!");
            e.printStackTrace();
        }
    }
}