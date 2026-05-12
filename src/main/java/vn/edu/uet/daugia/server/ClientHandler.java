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

import vn.edu.uet.daugia.shared.model.BidMessage;

import vn.edu.uet.daugia.database.DatabaseConnection;

public class ClientHandler implements Runnable {

    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

        try {

            // Đọc dữ liệu từ client
            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    clientSocket.getInputStream()
                            )
                    );

            // Gửi dữ liệu về client
            PrintWriter out =
                    new PrintWriter(
                            clientSocket.getOutputStream(),
                            true
                    );

            // Nhận JSON client gửi
            String json =
                    in.readLine();

            System.out.println(
                    "Server nhận được:"
            );

            System.out.println(json);

            // Parse JSON
            Gson gson = new Gson();

            JsonObject jsonObject =
                    gson.fromJson(
                            json,
                            JsonObject.class
                    );

            // Lấy type
            String type =
                    jsonObject.get("type")
                            .getAsString();

            // =========================
            // BID
            // =========================

            if(type.equals("BID")) {

                BidMessage bid =
                        gson.fromJson(
                                json,
                                BidMessage.class
                        );

                System.out.println(
                        "Username: "
                                + bid.getUsername()
                );

                System.out.println(
                        "Price: "
                                + bid.getPrice()
                );

                // Connect database
                Connection connection =
                        DatabaseConnection.getConnection();

                // SQL INSERT
                String sql =
                        "INSERT INTO bids(username, price) VALUES (?, ?)";

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                statement.setString(
                        1,
                        bid.getUsername()
                );

                statement.setInt(
                        2,
                        bid.getPrice()
                );

                // Thực thi INSERT
                statement.executeUpdate();

                System.out.println(
                        "Đã lưu bid vào database!"
                );

                // Gửi phản hồi về client
                out.println(
                        "BID_SUCCESS"
                );
            }

            // =========================
            // GET_BIDS
            // =========================

            if(type.equals("GET_BIDS")) {

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

                while(resultSet.next()) {

                    int id =
                            resultSet.getInt("id");

                    String username =
                            resultSet.getString("username");

                    int price =
                            resultSet.getInt("price");

                    response.append(
                            id + " | " +
                                    username + " | " +
                                    price + "\n"
                    );
                }

                // Gửi toàn bộ bids về client
                out.println(
                        response.toString()
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Lỗi ClientHandler!"
            );

            e.printStackTrace();
        }
    }
}