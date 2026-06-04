package vn.edu.uet.daugia.server.service;

import vn.edu.uet.daugia.shared.model.Auction;
import vn.edu.uet.daugia.shared.model.item.Electronics;
import vn.edu.uet.daugia.shared.model.user.Seller;
import java.time.LocalDateTime;

public class TestAuctionService {
    public static void main(String[] args) {
        System.out.println("=== TEST AUCTION SERVICE (GIAO TIẾP JSON) ===");

        // 1. Chuẩn bị dữ liệu và nạp vào Manager
        AuctionManager manager = AuctionManager.getInstance();
        LocalDateTime now = LocalDateTime.now();
        Seller seller = new Seller("tuan", "tu@gmail.com", "123", "Shop A");
        Electronics laptop = new Electronics(
                "SP01", "Laptop Gaming", "Mô tả", 500000,
                now, now.plusMinutes(10), "Asus", 24
        );

        Auction auction = new Auction(laptop, seller, 500000, now, now.plusMinutes(10));
        auction.startAuction();
        manager.addAuction(auction); // Nạp phiên đấu giá vào hệ thống quản lý chung

        String auctionId = auction.getId(); // Lấy ID tự sinh ra để test

        // 2. Khởi tạo Service (Cầu nối)
        AuctionService service = new AuctionService();

        // --- KỊCH BẢN 1: Đặt giá hợp lệ ---
        System.out.println("\n[Kịch bản 1] Gửi lệnh đặt 600.000đ:");
        String response1 = service.handlePlaceBid(auctionId, "user_123", 600000);
        System.out.println("--> JSON gửi về Client: " + response1);

        // --- KỊCH BẢN 2: Đặt giá lỗi (Thấp hơn giá hiện tại) ---
        System.out.println("\n[Kịch bản 2] Gửi lệnh đặt 550.000đ (Bị lỗi):");
        String response2 = service.handlePlaceBid(auctionId, "user_123", 550000);
        System.out.println("--> JSON gửi về Client: " + response2);

        // --- KỊCH BẢN 3: Lấy danh sách phiên đang chạy ---
        System.out.println("\n[Kịch bản 3] Tuấn yêu cầu lấy danh sách để hiển thị lên màn hình:");
        String listResponse = service.handleGetAuctions();
        System.out.println("--> JSON gửi về Client: " + listResponse);
    }
}