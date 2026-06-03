package vn.edu.uet.daugia.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AuctionServer {

    // Khai báo cổng mạng lấy từ file cấu hình chung
    private static final int PORT = vn.edu.uet.daugia.shared.ServerConfig.SERVER_PORT;
    // Bể chứa luồng (ThreadPool) - Giới hạn tối đa 100 người dùng cùng lúc
    // Vượt quá 100 người, hệ thống sẽ cho vào hàng đợi (Queue) chứ không tạo thêm luồng
    private static final ExecutorService pool = Executors.newFixedThreadPool(100);

    public static void main(String[] args) {
        System.out.println("=== HỆ THỐNG ĐẤU GIÁ SERVER ĐANG KHỞI ĐỘNG ===");
        System.out.println("Đang mở cổng " + PORT + " và chờ Client kết nối...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // Luôn mở cửa đón khách
            while (true) {
                // Lệnh này là Blocking: Đứng đợi cho đến khi có khách gõ cửa
                Socket clientSocket = serverSocket.accept();
                System.out.println("[KẾT NỐI MỚI] Khách hàng từ IP: " + clientSocket.getInetAddress().getHostAddress());

                // Giao Client này cho ThreadPool xử lý
                // ThreadPool sẽ gắp 1 luồng nhàn rỗi ra để chạy ClientHandler
                pool.execute(new ClientHandler(clientSocket));
            }

        } catch (Exception e) {
            System.err.println("Lỗi tại Server chính: " + e.getMessage());
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }
}