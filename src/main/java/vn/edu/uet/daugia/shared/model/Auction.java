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
    private Item item;
    private Seller seller;
    private double currentPrice;
    private Bidder currentLeader;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<BidTransaction> bidHistory;
    private LocalDateTime lastBidTime;
    private String lastBidderName;

    // Anti-sniping: bid trong 60s cuối → gia hạn thêm 60s
    private static final int ANTI_SNIPE_THRESHOLD_SECONDS = 60;
    private static final int ANTI_SNIPE_EXTENSION_SECONDS = 60;

    private final ReentrantLock lock = new ReentrantLock();

    public Auction(Item item, Seller seller,
                   double startingPrice,
                   LocalDateTime startTime,
                   LocalDateTime endTime) {
        super();
        this.item = item;
        this.seller = seller;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
    }

    /**
     * Đặt giá — thread-safe.
     * Trả về true nếu phiên được gia hạn (anti-sniping), false nếu không.
     */
    public boolean placeBid(Bidder bidder, double amount) {
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá không còn nhận bid. Trạng thái: " + status);
            }
            if (amount <= currentPrice) {
                throw new InvalidBidException(String.format(
                        "Giá phải cao hơn %,.0f VNĐ. Bạn đặt: %,.0f VNĐ", currentPrice, amount));
            }

            this.currentPrice   = amount;
            this.currentLeader  = bidder;
            this.lastBidTime    = LocalDateTime.now();
            this.lastBidderName = bidder.getUsername();

            BidTransaction tx = new BidTransaction(bidder, this, amount);
            bidHistory.add(tx);

            System.out.println("[BID] " + bidder.getUsername() + " đặt " + amount + " | Phiên: " + item.getName());

            // ===== ANTI-SNIPING =====
            long secondsLeft = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
            if (secondsLeft > 0 && secondsLeft <= ANTI_SNIPE_THRESHOLD_SECONDS) {
                endTime = endTime.plusSeconds(ANTI_SNIPE_EXTENSION_SECONDS);
                System.out.printf("[ANTI-SNIPE] Phiên %s gia hạn thêm %ds → kết thúc lúc %s%n",
                        item.getId(), ANTI_SNIPE_EXTENSION_SECONDS, endTime);
                return true;
            }
            return false;

        } finally {
            lock.unlock();
        }
    }

    public void startAuction() {
        if (status != AuctionStatus.OPEN) return;
        status = AuctionStatus.RUNNING;
        System.out.println("[START] Phiên đấu giá bắt đầu: " + item.getName());
    }

    public void applyRestoredState(double price, Bidder leader) {
        lock.lock();
        try {
            if (price > currentPrice) currentPrice = price;
            if (leader != null) currentLeader = leader;
            if (status == AuctionStatus.OPEN) status = AuctionStatus.RUNNING;
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
                status = AuctionStatus.CANCELED;
            }
        } finally {
            lock.unlock();
        }
    }

    public Bidder getWinner() {
        return (status == AuctionStatus.FINISHED) ? currentLeader : null;
    }

    public void scheduleAutoClose() {
        long delay = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
        if (delay <= 0) { closeAuction(); return; }
        java.util.concurrent.ScheduledExecutorService scheduler =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> { closeAuction(); scheduler.shutdown(); },
                delay, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public double getCurrentPrice()             { return currentPrice; }
    public Bidder getCurrentLeader()            { return currentLeader; }
    public AuctionStatus getStatus()            { return status; }
    public Item getItem()                       { return item; }
    public LocalDateTime getEndTime()           { return endTime; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }

    @Override
    public String getInfo() {
        return String.format("[Auction] %s | Giá: %.0f | Trạng thái: %s",
                item.getName(), currentPrice, status);
    }

    public String toJson() {
        String leaderName = (currentLeader != null) ? currentLeader.getUsername() : "";
        String bidTimeStr = (lastBidTime != null) ? lastBidTime.toString() : "";
        String lastBidder = (lastBidderName != null) ? lastBidderName : leaderName;
        return String.format(
                "{\"id\":\"%s\",\"itemId\":\"%s\",\"itemName\":\"%s\",\"currentPrice\":%.0f,"
                        + "\"currentLeader\":\"%s\",\"leader\":\"%s\",\"lastBidder\":\"%s\","
                        + "\"bidTime\":\"%s\",\"status\":\"%s\",\"endTime\":\"%s\"}",
                getId(), item.getId(), item.getName(), currentPrice,
                leaderName, leaderName, lastBidder, bidTimeStr, status.name(),
                endTime.toString());
    }
}