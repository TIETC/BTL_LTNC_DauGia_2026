package vn.edu.uet.daugia.model;

import vn.edu.uet.daugia.model.entity.Entity;
import vn.edu.uet.daugia.model.user.Bidder;
import java.time.LocalDateTime;
/**
 * Ghi lại MỘT lần đặt giá.
 * Immutable (bất biến) sau khi tạo — không có setter.
 */
public class BidTransaction extends Entity {
    private final Bidder bidder;       // Ai đặt
    private final Auction auction;     // Thuộc phiên nào
    private final double amount;       // Đặt bao nhiêu
    private final LocalDateTime bidTime; // Lúc mấy giờ

    public BidTransaction(Bidder bidder, Auction auction, double amount) {
        super();
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.bidTime = LocalDateTime.now(); // ghi thời gian ngay lúc tạo
    }
    public Bidder getBidder()      { return bidder; }
    public Auction getAuction()    { return auction; }
    public double getAmount()      { return amount; }
    public LocalDateTime getBidTime() { return bidTime; }

    @Override
    public String getInfo() {
        return String.format("[Bid] %s đặt %.0f lúc %s",
                bidder.getUsername(), amount,
                bidTime.format(java.time.format.DateTimeFormatter
                        .ofPattern("HH:mm:ss")));
    }
}