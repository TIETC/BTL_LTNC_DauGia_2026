package vn.edu.uet.daugia.shared.model;

import vn.edu.uet.daugia.shared.exception.AuctionClosedException;
import vn.edu.uet.daugia.shared.exception.InvalidBidException;
import vn.edu.uet.daugia.shared.model.entity.Entity;
import vn.edu.uet.daugia.shared.model.item.Item;
import vn.edu.uet.daugia.shared.model.user.Bidder;
import vn.edu.uet.daugia.shared.model.user.Seller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private Item item;              // Sản phẩm đang được đấu giá
    private Seller seller;          // Người đăng bán
    private double currentPrice;    // Giá cao nhất hiện tại
    private Bidder currentLeader;   // Người đang dẫn đầu
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<BidTransaction> bidHistory; // Lịch sử tất cả lần đặt giá
    private LocalDateTime lastBidTime;       // Thời điểm bid mới nhất (push realtime)
    private String lastBidderName;           // Người đặt bid mới nhất

    // Lock để xử lý đồng thời — chống Race Condition
    private final ReentrantLock lock = new ReentrantLock();

    public Auction(Item item, Seller seller,
                   double startingPrice,
                   LocalDateTime startTime,
                   LocalDateTime endTime) {
        super(); // sinh id, createdAt từ Entity
        this.item = item;
        this.seller = seller;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN; // mới tạo mặc định = OPEN
        this.bidHistory = new ArrayList<>();
    }
    /**
     * Người dùng đặt giá. Phải thread-safe vì nhiều người gọi đồng thời.
     */
    public void placeBid(Bidder bidder, double amount) {
        lock.lock(); // Khóa lại — chỉ 1 thread vào được tại 1 thời điểm
        try {
            // Kiểm tra 1: phiên có đang chạy không?
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá không còn nhận bid. Trạng thái: " + status);
            }

            // Kiểm tra 2: giá đặt có cao hơn giá hiện tại không?
            if (amount <= currentPrice) {
                throw new InvalidBidException("Giá phải cao hơn " + currentPrice + ". Bạn đặt: " + amount);
            }
            // Hợp lệ — cập nhật thông tin
            this.currentPrice = amount;
            this.currentLeader = bidder;
            this.lastBidTime = LocalDateTime.now();
            this.lastBidderName = bidder.getUsername();

            // Tạo bản ghi giao dịch
            BidTransaction tx = new BidTransaction(bidder, this, amount);
            bidHistory.add(tx);

            System.out.println("[BID] " + bidder.getUsername() + " đặt " + amount + " | Phiên: " + item.getName());
        } finally {
            lock.unlock(); // LUÔN LUÔN mở khóa dù có lỗi hay không
        }
    }

    public void startAuction() {
        if (status != AuctionStatus.OPEN) return;
        status = AuctionStatus.RUNNING;
        System.out.println("[START] Phiên đấu giá bắt đầu: " + item.getName());
    }

    /** Khôi phục giá / người dẫn sau khi nạp phiên từ database. */
    public void applyRestoredState(double price, Bidder leader) {
        lock.lock();
        try {
            if (price > currentPrice) {
                currentPrice = price;
            }
            if (leader != null) {
                currentLeader = leader;
            }
            if (status == AuctionStatus.OPEN) {
                status = AuctionStatus.RUNNING;
            }
        } finally {
            lock.unlock();
        }
    }

    public void closeAuction() {
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING) return;
            if (currentLeader != null) {
                status = AuctionStatus.FINISHED;
                System.out.println("[END] Người thắng: " + currentLeader.getUsername() + " | Giá: " + currentPrice);
            } else {
                status = AuctionStatus.CANCELED; // không ai đặt giá
            }
        } finally {
            lock.unlock();
        }
    }

    public Bidder getWinner() {
        return (status == AuctionStatus.FINISHED) ? currentLeader : null;
    }

    // Tự động đóng phiên bằng ScheduledExecutorService
    public void scheduleAutoClose() {
        long delay = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
        if (delay <= 0) {
            closeAuction(); //  đóng ngay
            return;
        }
        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            closeAuction();
            scheduler.shutdown();
        }, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    public double getCurrentPrice()          { return currentPrice; }
    public Bidder getCurrentLeader()         { return currentLeader; }
    public AuctionStatus getStatus()         { return status; }
    public Item getItem()                    { return item; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }

    @Override
    public String getInfo() {
        return String.format("[Auction] %s | Giá: %.0f | Trạng thái: %s", item.getName(), currentPrice, status);
    }
    // Thêm vào dưới cùng class Auction.java
    public String toJson() {
        String leaderName = (currentLeader != null) ? currentLeader.getUsername() : "";
        String bidTimeStr = (lastBidTime != null) ? lastBidTime.toString() : "";
        String lastBidder = (lastBidderName != null) ? lastBidderName : leaderName;
        return String.format(
                "{\"id\":\"%s\",\"itemId\":\"%s\",\"itemName\":\"%s\",\"currentPrice\":%.0f,"
                        + "\"currentLeader\":\"%s\",\"leader\":\"%s\",\"lastBidder\":\"%s\","
                        + "\"bidTime\":\"%s\",\"status\":\"%s\"}",
                getId(), item.getId(), item.getName(), currentPrice,
                leaderName, leaderName, lastBidder, bidTimeStr, status.name()
        );
    }
}
