package vn.edu.uet.daugia.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {

    private static final int PORT = 5000;
    private static final ExecutorService pool = Executors.newFixedThreadPool(100);

    public static void main(String[] args) {
        new Thread(new ServerDiscovery()).start();

        System.out.println("=== HỆ THỐNG ĐẤU GIÁ SERVER ĐANG KHỞI ĐỘNG ===");

        // Nạp các phiên RUNNING từ MySQL vào RAM (tránh "Auction not found" sau khi restart)
        AuctionService auctionService = new AuctionService();
        auctionService.loadAllRunningAuctionsFromDatabase();

        // ⭐ MỚI: Bật bộ tự động đóng phiên hết giờ (RUNNING → FINISHED/CANCELED)
        auctionService.startAuctionCloserScheduler();

        System.out.println("Đang mở cổng " + PORT + " và chờ Client kết nối...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[KẾT NỐI MỚI] Khách hàng từ IP: " + clientSocket.getInetAddress().getHostAddress());
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