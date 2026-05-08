package vn.edu.uet.daugia;

import vn.edu.uet.daugia.model.*;
import vn.edu.uet.daugia.model.user.*;
import vn.edu.uet.daugia.model.item.*;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== TEST LOGIC ĐẤU GIÁ ===");


        Bidder bidder1 = new Bidder("trung", "t@gmail.com", "123", 1000000);
        Bidder bidder2 = new Bidder("quan", "q@gmail.com", "123", 1000000);
        Seller seller  = new Seller("tuan", "tu@gmail.com", "123", "Shop A");

        LocalDateTime now = LocalDateTime.now();
        Electronics laptop = new Electronics(
                "SP01",                  // id
                "Laptop Gaming",         // name
                "Laptop đồ họa cao cấp", // description
                500000,                  // startingPrice
                now,                     // startTime
                now.plusSeconds(10),     // endTime (đóng sau 10s)
                "Asus",                  // brand
                24                       // warrantyMonths (Bảo hành 24 tháng)
        );

        Auction auction = new Auction(
                laptop,
                seller,
                500000,
                now,
                now.plusSeconds(10)
        );

        auction.startAuction();
        auction.scheduleAutoClose();

        System.out.println("\n--- TIẾN HÀNH ĐẶT GIÁ ---");
        try {
            auction.placeBid(bidder1, 600000);
            auction.placeBid(bidder2, 700000);
            auction.placeBid(bidder1, 650000); //Thất bại vì thấp hơn 700k
        } catch (Exception e) {
            System.out.println("CẢNH BÁO LỖI: " + e.getMessage());
        }

        // 5. Kiểm tra kết quả sau khi hết giờ
        System.out.println("\nĐang chờ 11 giây để hệ thống tự đóng phiên...");
        Thread.sleep(11000);

        System.out.println("\n--- KẾT QUẢ CHUNG CUỘC ---");
        if (auction.getWinner() != null) {
            System.out.println("Người thắng: " + auction.getWinner().getUsername() + " | Giá: " + auction.getCurrentPrice());
        }

        System.out.println("Lịch sử giao dịch:");
        auction.getBidHistory().forEach(tx -> System.out.println(tx.getInfo()));
    }
}