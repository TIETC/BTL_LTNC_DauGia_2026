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
            out.println("{\"type\":\"NEW_BID\",\"data\":" + auction.toJson() + "}");
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            AuctionManager.getInstance().addClient(out);

            String json;
            while ((json = in.readLine()) != null) {
                System.out.println("Server nhận được: " + json);

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                String type = jsonObject.get("type").getAsString();

                if (type.equals("REGISTER")) {
                    RegisterMessage register = gson.fromJson(json, RegisterMessage.class);
                    try {
                        Connection connection = DatabaseConnection.getConnection();
                        String checkSql = "SELECT username FROM users WHERE username = ?";
                        PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                        checkStmt.setString(1, register.getUsername());
                        ResultSet rs = checkStmt.executeQuery();

                        if (rs.next()) {
                            out.println("REGISTER_FAILED:USERNAME_EXISTS");
                        } else {
                            String sql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";
                            PreparedStatement statement = connection.prepareStatement(sql);
                            statement.setString(1, register.getUsername());
                            statement.setString(2, register.getPassword());
                            statement.setString(3, register.getRole());
                            statement.executeUpdate();
                            out.println("REGISTER_SUCCESS");
                        }
                    } catch (Exception e) {
                        out.println("REGISTER_FAILED:SERVER_ERROR");
                    }
                }

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
                    } else {
                        out.println("LOGIN_FAILED");
                    }
                }

                // ========================================================
                // === BỔ SUNG HOÀN THIỆN: GET_AUCTIONS TỪ CODE BỊ CỤT ===
                // ========================================================
                if (type.equals("GET_AUCTIONS")) {
                    try {
                        Connection connection = DatabaseConnection.getConnection();
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
                            double startPrice = resultSet.getDouble("startPrice");
                            double currentPrice = resultSet.getDouble("currentPrice");
                            String endTime  = resultSet.getString("endTime");

                            String imageUrl = "";
                            try {
                                Object imgObj = resultSet.getObject("image_url");
                                if (imgObj != null) imageUrl = imgObj.toString();
                            } catch (Exception ignored) {}

                            double maxPrice = 0;
                            try {
                                maxPrice = resultSet.getDouble("max_price");
                            } catch (Exception ignored) {}

                            sb.append(String.format(
                                    "{\"id\":\"%s\",\"name\":\"%s\",\"session\":\"%s\",\"startPrice\":%f,\"currentPrice\":%f,\"maxPrice\":%f,\"imageUrl\":\"%s\",\"endTime\":\"%s\"}",
                                    itemId, itemName, seller, startPrice, currentPrice, maxPrice, imageUrl, endTime
                            ));
                            first = false;
                        }
                        sb.append("]");
                        out.println(sb.toString());
                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý lệnh GET_AUCTIONS: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Mất kết nối ClientHandler: " + e.getMessage());
        }
    }
}