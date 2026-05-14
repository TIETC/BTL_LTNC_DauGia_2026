package vn.edu.uet.daugia.client;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.Gson;
import vn.edu.uet.daugia.shared.model.BidMessage;

public class AuctionClient {

    public static void main(String[] args) {

        try {
            String serverAddress = "10.11.71.231";
            int port = 5000;

            System.out.println("Đang tìm kiếm Máy chủ ở cổng " + port + "...");

            // Kết nối server
            Socket socket = new Socket(serverAddress, port);
            System.out.println("Đã kết nối thành công tới Máy chủ Đấu giá!");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Khởi tạo theo cấu trúc MỚI: (type, auctionId, bidderId, price)
            BidMessage bid = new BidMessage(
                    "BID",
                    "SP01",        // Mã phiên đấu giá giả định
                    "Trung khim",  // Bidder ID
                    1200.0         // Giá tiền (Bắt buộc phải có .0 để thành kiểu double)
            );

            Gson gson = new Gson();
            String json = gson.toJson(bid);

            // Gửi JSON
            out.println(json);
            System.out.println("Đã gửi JSON:\n" + json);

            // Đọc phản hồi server
            String response = in.readLine();
            System.out.println("Server phản hồi:\n" + response);

            socket.close();

        } catch (Exception e) {
            System.out.println("Không thể kết nối tới server!");
            e.printStackTrace();
        }
    }
}