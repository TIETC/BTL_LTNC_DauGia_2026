package vn.edu.uet.daugia.server;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import vn.edu.uet.daugia.shared.model.Auction;

// 1. Gắn thêm tai nghe "AuctionObserver"
public class ClientHandler implements Runnable, AuctionObserver {

    private Socket clientSocket;
    private AuctionService auctionService;
    private PrintWriter out; // 2. Đưa loa phát thanh lên làm tài sản chung của class

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.auctionService = new AuctionService();

        // 3. Đăng ký thông tin với Trạm phát sóng (AuctionManager)
        AuctionManager.getInstance().addObserver(this);
    }
// Hàm tự động chạy khi có lênh mới
    @Override
    public void onNewBid(Auction auction) {
        if (out != null) {
            // Đóng gói thông báo thành JSON và bắn thẳng về màn hình Client ( Không cần f5 )
            out.println("{\"type\":\"NEW_BID\",\"data\":" + auction.toJson() + "}");
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // Khởi tạo loa phát
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String json;
            // Vòng lặp liên tục lắng nghe yêu cầu từ Client
            while ((json = in.readLine()) != null) {
                System.out.println("Server nhận được: " + json);

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                String type = jsonObject.get("type").getAsString();

                // XỬ LÝ LỆNH ĐẶT GIÁ
                if (type.equals("BID")) {
                    String auctionId = jsonObject.has("auctionId") ? jsonObject.get("auctionId").getAsString() : "SP01";
                    String bidderId = jsonObject.has("bidderId") ? jsonObject.get("bidderId").getAsString() : "user_123";
                    double price = jsonObject.get("price").getAsDouble();

                    String responseJson = auctionService.handlePlaceBid(auctionId, bidderId, price);
                    out.println(responseJson);
                }

                // XỬ LÝ LỆNH LẤY DANH SÁCH
                if (type.equals("GET_BIDS")) {
                    String responseJson = auctionService.handleGetAuctions();
                    out.println(responseJson);
                }
            }
        } catch (Exception e) {
            System.out.println("Client đã ngắt kết nối!");
        }
    }
}