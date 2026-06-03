package vn.edu.uet.daugia.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import vn.edu.uet.daugia.database.DatabaseConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private AuctionService auctionService;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.gson = new Gson();
        this.auctionService = new AuctionService();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String jsonRequest;

            while ((jsonRequest = in.readLine()) != null) {
                System.out.println("[Yêu cầu từ Client]: " + jsonRequest);

                JsonObject requestObj = gson.fromJson(jsonRequest, JsonObject.class);

                String type = requestObj.has("type") ? requestObj.get("type").getAsString() : "";
                String response = "";

                switch (type) {
                    case "LOGIN":
                        String user = requestObj.get("username").getAsString();
                        String pass = requestObj.get("password").getAsString();
                        String role = DatabaseConnection.checkLogin(user, pass);
                        if (role != null) {
                            response = "{\"status\":\"SUCCESS\",\"role\":\"" + role + "\"}";
                        } else {
                            response = "{\"status\":\"FAIL\",\"message\":\"Sai tài khoản hoặc mật khẩu\"}";
                        }
                        break;

                    case "ADD_ITEM":
                        String itemId = requestObj.get("itemId").getAsString();
                        String name = requestObj.get("name").getAsString();
                        double price = requestObj.get("startingPrice").getAsDouble();

                        boolean isAdded = DatabaseConnection.insertItem(itemId, name, price);
                        if (isAdded) {
                            response = "{\"status\":\"OK\",\"message\":\"Đã thêm sản phẩm thành công\"}";
                        } else {
                            response = "{\"status\":\"ERROR\",\"message\":\"Lỗi không thể thêm sản phẩm\"}";
                        }
                        break;

                    case "BID":
                        String auctionId = requestObj.get("auctionId").getAsString();
                        String bidderId = requestObj.get("bidderId").getAsString();
                        double bidAmount = requestObj.get("price").getAsDouble();

                        response = auctionService.handlePlaceBid(auctionId, bidderId, bidAmount);
                        break;

                    case "GET_AUCTIONS":
                        response = auctionService.handleGetAuctions();
                        break;

                    default:
                        response = "{\"status\":\"ERROR\",\"message\":\"Cú pháp JSON không hợp lệ\"}";
                        break;
                }

                out.println(response);
            }
        } catch (Exception e) {
            System.out.println("Một Client đã ngắt kết nối: " + socket.getInetAddress().getHostAddress());
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}