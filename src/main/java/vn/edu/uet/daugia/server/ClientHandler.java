package vn.edu.uet.daugia.server;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import vn.edu.uet.daugia.shared.model.BidMessage;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    // Gọi AuctionService
    private AuctionService auctionService;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.auctionService = new AuctionService(); // Khởi tạo Cầu nối
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String json = in.readLine();
            System.out.println("Server nhận được: " + json);

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            String type = jsonObject.get("type").getAsString();
            // Đạt giá bid
            if (type.equals("BID")) {
                // Giả sử Client gửi JSON có chứa auctionId, bidderId và price
                String auctionId = jsonObject.has("auctionId") ? jsonObject.get("auctionId").getAsString() : "SP01";
                String bidderId = jsonObject.has("bidderId") ? jsonObject.get("bidderId").getAsString() : "user_123";
                double price = jsonObject.get("price").getAsDouble();

                // GỌI XUỐNG CORE LOGIC CỦA (Đã bao gồm ReentrantLock)
                String responseJson = auctionService.handlePlaceBid(auctionId, bidderId, price);

                // Gửi JSON kết quả ("OK" hoặc "ERROR") về cho Client
                out.println(responseJson);
            }

            // LẤY DANH SÁCH PHIÊN (GET_BIDS)
            if (type.equals("GET_BIDS")) {
                // Gọi hàm lấy danh sách JSON
                String responseJson = auctionService.handleGetAuctions();

                // Gửi trả Client mảng JSON chuẩn
                out.println(responseJson);
            }

        } catch (Exception e) {
            System.out.println("Lỗi ClientHandler!");
            e.printStackTrace();
        }
    }
}