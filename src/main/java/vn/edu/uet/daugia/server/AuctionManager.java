package vn.edu.uet.daugia.server;

import vn.edu.uet.daugia.shared.model.Auction;
import vn.edu.uet.daugia.shared.model.AuctionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AuctionManager {

    private static volatile AuctionManager instance;

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

//Hàm nghiệp vụ
    public void addAuction(Auction auction) {
        if (auction != null && auction.getId() != null) {
            auctions.put(auction.getId(), auction);
        }
    }

    public Auction findById(String auctionId) {
        return auctions.get(auctionId);
    }
    //Chỉ lấy những phiên đang RUNNING
    public List<Auction> getActiveAuctions() {
        return auctions.values().stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .collect(Collectors.toList());
    }

    public void removeAuction(String auctionId) {
        auctions.remove(auctionId);
    }

    // 1. Cuốn sổ ghi danh sách những Client đang kết nối
    // Dùng CopyOnWriteArrayList để chống lỗi Đa luồng khi có người thêm/xóa liên tục
    private java.util.concurrent.CopyOnWriteArrayList<AuctionObserver> observers = new java.util.concurrent.CopyOnWriteArrayList<>();

    // 2. Hàm đăng ký nhận thông báo
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    // 3. Hàm gọi khi có giá mới
    public void notifyObservers(Auction auction) {
        for (AuctionObserver obs : observers) {
            obs.onNewBid(auction); // Báo cho từng người một
        }
    }
}