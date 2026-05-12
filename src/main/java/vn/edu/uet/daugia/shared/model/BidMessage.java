package vn.edu.uet.daugia.shared.model;

public class BidMessage {

    private String type;
    private String auctionId; // Bổ sung: Bắt buộc phải biết đang đấu giá món hàng nào
    private String bidderId;  // Đổi username thành bidderId cho chuẩn với UserManager
    private double price;     // SỬA QUAN TRỌNG: Phải là double để khớp với Core Logic của Trung

    public BidMessage() {
    }

    public BidMessage(String type, String auctionId, String bidderId, double price) {
        this.type = type;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getPrice() {
        return price;
    }
}