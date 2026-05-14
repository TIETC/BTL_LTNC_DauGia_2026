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

import vn.edu.uet.daugia.database.DatabaseConnection;

// 1. Gắn thêm tai nghe "AuctionObserver"
public class ClientHandler implements Runnable, AuctionObserver {

    private Socket clientSocket;

    private AuctionService auctionService;

    // 2. Đưa loa phát thanh lên làm tài sản chung của class
    private PrintWriter out;

    public ClientHandler(Socket clientSocket) {

        this.clientSocket = clientSocket;

        this.auctionService =
                new AuctionService();

        // 3. Đăng ký với AuctionManager
        AuctionManager.getInstance()
                .addObserver(this);
    }

    // Hàm realtime khi có bid mới
    @Override
    public void onNewBid(Auction auction) {

        if (out != null) {

            out.println(
                    "{\"type\":\"NEW_BID\",\"data\":"
                            + auction.toJson()
                            + "}"
            );
        }
    }

    @Override
    public void run() {

        try {

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    clientSocket.getInputStream()
                            )
                    );

            // Loa phát cho client
            out =
                    new PrintWriter(
                            clientSocket.getOutputStream(),
                            true
                    );

            String json;

            // Lắng nghe liên tục
            while ((json = in.readLine()) != null) {

                System.out.println(
                        "Server nhận được: "
                                + json
                );

                Gson gson =
                        new Gson();

                JsonObject jsonObject =
                        gson.fromJson(
                                json,
                                JsonObject.class
                        );

                String type =
                        jsonObject.get("type")
                                .getAsString();

                // =========================
                // REGISTER
                // =========================

                if (type.equals("REGISTER")) {

                    RegisterMessage register =
                            gson.fromJson(
                                    json,
                                    RegisterMessage.class
                            );

                    System.out.println(
                            "Đang xử lý REGISTER..."
                    );

                    Connection connection =
                            DatabaseConnection.getConnection();

                    String sql =
                            "INSERT INTO users(username, password) VALUES (?, ?)";

                    PreparedStatement statement =
                            connection.prepareStatement(sql);

                    statement.setString(
                            1,
                            register.getUsername()
                    );

                    statement.setString(
                            2,
                            register.getPassword()
                    );

                    statement.executeUpdate();

                    System.out.println(
                            "Đã lưu user vào database!"
                    );

                    out.println(
                            "REGISTER_SUCCESS"
                    );
                }

                // =========================
                // BID
                // =========================

                if (type.equals("BID")) {

                    String auctionId =
                            jsonObject.get("auctionId")
                                    .getAsString();

                    String bidderId =
                            jsonObject.get("bidderId")
                                    .getAsString();

                    double price =
                            jsonObject.get("price")
                                    .getAsDouble();

                    // =========================
                    // LƯU DATABASE
                    // =========================

                    Connection connection =
                            DatabaseConnection.getConnection();

                    String sql =
                            "INSERT INTO bids(auctionId, bidderId, price) VALUES (?, ?, ?)";

                    PreparedStatement statement =
                            connection.prepareStatement(sql);

                    statement.setString(
                            1,
                            auctionId
                    );

                    statement.setString(
                            2,
                            bidderId
                    );

                    statement.setDouble(
                            3,
                            price
                    );

                    statement.executeUpdate();

                    System.out.println(
                            "Đã lưu bid vào database!"
                    );

                    // =========================
                    // REALTIME LOGIC
                    // =========================

                    String responseJson =
                            auctionService.handlePlaceBid(
                                    auctionId,
                                    bidderId,
                                    price
                            );

                    out.println(responseJson);
                }

                // =========================
                // GET_BIDS
                // =========================

                if (type.equals("GET_BIDS")) {

                    Connection connection =
                            DatabaseConnection.getConnection();

                    String sql =
                            "SELECT * FROM bids";

                    PreparedStatement statement =
                            connection.prepareStatement(sql);

                    ResultSet resultSet =
                            statement.executeQuery();

                    StringBuilder response =
                            new StringBuilder();

                    while (resultSet.next()) {

                        int id =
                                resultSet.getInt("id");

                        String auctionId =
                                resultSet.getString("auctionId");

                        String bidderId =
                                resultSet.getString("bidderId");

                        double price =
                                resultSet.getDouble("price");

                        response.append(
                                id + " | "
                                        + auctionId + " | "
                                        + bidderId + " | "
                                        + price + "\n"
                        );
                    }

                    out.println(
                            response.toString()
                    );
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Client đã ngắt kết nối!"
            );

            e.printStackTrace();
        }
    }
}