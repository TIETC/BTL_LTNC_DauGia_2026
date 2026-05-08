package server;

import vn.edu.uet.daugia.model.item.Item; // Import class Item đã tạo sẵn trong model.item

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {


    private static volatile AuctionManager instance;
    public static class Auction {
        private String auctionId;
        private Item item;
        private double currentHighestBid;

        public Auction(String auctionId, Item item, double startingPrice) {
            this.auctionId = auctionId;
            this.item = item;
            this.currentHighestBid = startingPrice;
        }

        public String getAuctionId() { return auctionId; }
        public Item getItem() { return item; }
        public double getCurrentHighestBid() { return currentHighestBid; }

        public void placeBid(double amount) {
            if (amount > currentHighestBid) {
                this.currentHighestBid = amount;
            }
        }
    }

    private ConcurrentHashMap<String, Auction> auctions;

    private AuctionManager() {
        auctions = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // --- CÁC HÀM NGHIỆP VỤ ---

    // Thêm một phiên đấu giá mới vào hệ thống
    public void addAuction(String auctionId, Auction auction) {
        if (auction != null && auctionId != null) {
            auctions.put(auctionId, auction);
        }
    }

    // Lấy thông tin một phiên đấu giá dựa trên ID
    public Auction getAuction(String auctionId) {
        return auctions.get(auctionId);
    }

    // Lấy toàn bộ danh sách các phiên đang diễn ra (để hiển thị lên GUI)
    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }

    // Kết thúc / Xóa phiên đấu giá
    public void removeAuction(String auctionId) {
        auctions.remove(auctionId);
    }
}