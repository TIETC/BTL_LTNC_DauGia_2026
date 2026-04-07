package server;

import java.net.ServerSocket;
import java.net.Socket; // Nhớ thêm dòng import này

public class AuctionServer {
    public static void main(String[] args) {
        try {
            int port = 5000;
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Máy chủ Đấu giá đang chạy trên cổng " + port + " và chờ khách hàng kết nối...");

            // Lệnh accept() này cực kỳ quan trọng: Nó bắt Server phải "đóng băng" đứng đợi ở đây
            // cho đến khi nào có một Client kết nối vào thì nó mới chạy tiếp.
            Socket clientSocket = serverSocket.accept();
            System.out.println("Đã có một Khách hàng vừa kết nối vào Server!");

        } catch (Exception e) {
            System.out.println("Có lỗi xảy ra khi khởi động Server!");
            e.printStackTrace();
        }
    }
}