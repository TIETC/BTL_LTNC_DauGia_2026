package vn.edu.uet.daugia.model;

import vn.edu.uet.daugia.model.entity.Entity;
import vn.edu.uet.daugia.model.item.Item;
import vn.edu.uet.daugia.model.user.Bidder;
import vn.edu.uet.daugia.model.user.Seller;

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
        lock.lock(); // Khóa lại — chỉ 1 thread vào được tại 1 thời điểm [cite: 222]
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

            // Tạo bản ghi giao dịch (không bao giờ xóa)
            BidTransaction tx = new BidTransaction(bidder, this, amount);
            bidHistory.add(tx);

            System.out.println("[BID] " + bidder.getUsername() + " đặt " + amount + " | Phiên: " + item.getName());
        } finally {
            lock.unlock(); // LUÔN LUÔN mở khóa dù có lỗi hay không [cite: 245]
        }
    }

    public void startAuction() {
        if (status != AuctionStatus.OPEN) return;
        status = AuctionStatus.RUNNING;
        System.out.println("[START] Phiên đấu giá bắt đầu: " + item.getName());
    }

    public void closeAuction() {
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING) return;
            if (currentLeader != null) {
                status = AuctionStatus.FINISHED;
                System.out.println("[END] Người thắng: " + currentLeader.getUsername() + " | Giá: " + currentPrice);
            } else {
                status = AuctionStatus.CANCELED; // không ai đặt giá [cite: 269]
            }
        } finally {
            lock.unlock();
        }
    }

    public Bidder getWinner() {
        return (status == AuctionStatus.FINISHED) ? currentLeader : null;
    }

    // Tự động đóng phiên bằng ScheduledExecutorService [cite: 291]
    public void scheduleAutoClose() {
        long delay = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis(); [cite: 300, 301, 302]
        if (delay <= 0) {
            closeAuction(); //  đóng ngay [cite: 304]
            return;
        }
        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(); [cite: 307, 308]
        scheduler.schedule(() -> { [cite: 309]
            closeAuction(); [cite: 310]
            scheduler.shutdown(); [cite: 311]
        }, delay, java.util.concurrent.TimeUnit.MILLISECONDS); [cite: 312]
    }

    // --- Getters ---
    public double getCurrentPrice()          { return currentPrice; }
    public Bidder getCurrentLeader()         { return currentLeader; }
    public AuctionStatus getStatus()         { return status; }
    public Item getItem()                    { return item; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }

    @Override
    public String getInfo() {
        return String.format("[Auction] %s | Giá: %.0f | Trạng thái: %s", item.getName(), currentPrice, status); [cite: 287, 288]
    }
}