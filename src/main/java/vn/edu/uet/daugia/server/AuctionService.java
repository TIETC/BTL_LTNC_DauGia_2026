package vn.edu.uet.daugia.server;

import vn.edu.uet.daugia.shared.exception.AuctionClosedException;
import vn.edu.uet.daugia.shared.exception.InvalidBidException;
import vn.edu.uet.daugia.shared.model.Auction;
import vn.edu.uet.daugia.shared.model.user.Bidder;
import vn.edu.uet.daugia.database.DatabaseConnection; // Import lớp kết nối CSDL

public class AuctionService {
    private AuctionManager manager = AuctionManager.getInstance();

    /**
     * Xử lý yêu cầu đặt giá từ Client, lưu vào Database và phát Broadcast
     */
    public String handlePlaceBid(String auctionId, String bidderId, double amount) {
        try {
            Auction auction = manager.findById(auctionId);
            if (auction == null) {
                return "{\"status\":\"ERROR\",\"message\":\"Không tìm thấy mã sản phẩm này\"}";
            }

            Bidder bidder = UserManager.findBidder(bidderId);
            if (bidder == null) {
                return "{\"status\":\"ERROR\",\"message\":\"Tài khoản người dùng không tồn tại\"}";
            }
            auction.placeBid(bidder, amount);
            boolean isSaved = DatabaseConnection.saveBidTransaction(auctionId, bidderId, amount);
            if (!isSaved) {
                System.err.println("[CẢNH BÁO] Đặt giá thành công trên RAM nhưng chưa lưu được xuống ổ cứng!");
            }

            manager.notifyObservers(auction);

            return String.format(
                    "{\"status\":\"OK\",\"currentPrice\":%.0f,\"leader\":\"%s\"}",
                    auction.getCurrentPrice(),
                    auction.getCurrentLeader().getUsername()
            );

        } catch (InvalidBidException e) {
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}";
        } catch (AuctionClosedException e) {
            return "{\"status\":\"ERROR\",\"message\":\"Phiên đấu giá đã đóng\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\":\"ERROR\",\"message\":\"Lỗi hệ thống Server\"}";
        }
    }

    /**
     * Lấy danh sách các phiên đang chạy dưới định dạng JSON cho màn hình chính
     */
    public String handleGetAuctions() {
        StringBuilder sb = new StringBuilder("[");
        manager.getActiveAuctions().forEach(a ->
                sb.append(a.toJson()).append(",")
        );

        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.append("]").toString();
    }
}