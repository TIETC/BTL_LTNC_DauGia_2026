package vn.edu.uet.daugia.server;

import vn.edu.uet.daugia.shared.exception.AuctionClosedException;
import vn.edu.uet.daugia.shared.exception.InvalidBidException;
import vn.edu.uet.daugia.shared.model.*;
import vn.edu.uet.daugia.shared.model.user.*;

public class AuctionService {
    private AuctionManager manager = AuctionManager.getInstance();

    /** * Hàm này nhận yêu cầu đặt giá từ ClientHandler của Quân
     * và trả về kết quả dưới dạng chuỗi JSON
     */
    public String handlePlaceBid(String auctionId, String bidderId, double amount) {
        try {
            // 1. Tìm phiên đấu giá và người dùng
            Auction auction = manager.findById(auctionId);
//
//            System.out.println("auctionId = " + auctionId);
            System.out.println("auction = " + auction);

            if (auction == null) {
                return "{\"status\":\"ERROR\",\"message\":\"Auction not found\"}";
            }

            Bidder bidder = UserManager.findBidder(bidderId);

            if (bidder == null) {
                return "{\"status\":\"ERROR\",\"message\":\"Bidder not found\"}";
            }


            // 2. Kích hoạt logic Đa luồng (ReentrantLock)
            auction.placeBid(bidder, amount);
            // Thông báo cho tất cả các Client khác là giá đã thay đổi!
            manager.notifyObservers(auction);

            // 3. Nếu thành công trả về JSON OK
            return String.format(
                    "{\"status\":\"OK\",\"currentPrice\":%.0f,\"leader\":\"%s\"}",
            auction.getCurrentPrice(),
            auction.getCurrentLeader().getUsername()
            );

        } catch (InvalidBidException e) {
            // Dịch lỗi logic thành JSON để gửi về màn hình Client
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}";
        } catch (AuctionClosedException e) {
            return "{\"status\":\"ERROR\",\"message\":\"Phiên đã đóng\"}";
        } catch (Exception e) {

            System.out.println("LỖI HANDLE BID:");

            e.printStackTrace();

            return "{\"status\":\"ERROR\",\"message\":\""
                    + e.getMessage()
                    + "\"}";
        }
    }

    /** Trả về danh sách phiên đang chạy dạng JSON cho màn hình chính của Tuấn */
    public String handleGetAuctions() {
        StringBuilder sb = new StringBuilder("[");
        manager.getActiveAuctions().forEach(a ->
        sb.append(a.toJson()).append(",") // Gọi hàm toJson
        );

        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1); // Xóa dấu phẩy thừa ở cuối
        }
        return sb.append("]").toString();
    }
}