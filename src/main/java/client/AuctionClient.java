package client;

import java.net.Socket;

public class AuctionClient {
    public static void main(String[] args) {
        try {
            // Địa chỉ của Server. "127.0.0.1" (localhost) nghĩa là Server đang nằm ngay trên chính máy tính này
            String serverAddress = "127.0.0.1";
            int port = 5000;

            System.out.println("Đang tìm kiếm Máy chủ ở cổng " + port + "...");

            // Dùng Socket để "gõ cửa" Server
            Socket socket = new Socket(serverAddress, port);

            System.out.println("Đã kết nối thành công tới Máy chủ Đấu giá!");

        } catch (Exception e) {
            System.out.println("Không thể kết nối! Cửa Server đang đóng hoặc Server chưa được bật.");
        }
    }
}