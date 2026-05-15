package vn.edu.uet.daugia.server;

import vn.edu.uet.daugia.shared.model.Auction;
import vn.edu.uet.daugia.shared.model.AuctionStatus;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class AuctionManager {

    private static volatile AuctionManager instance;
    private ConcurrentHashMap<String, Auction> auctions;

    // Danh sách tất cả client đang kết nối (dùng PrintWriter để gửi)
    private CopyOnWriteArrayList<PrintWriter> connectedClients = new CopyOnWriteArrayList<>();

    // Danh sách observer (cho BID realtime)
    private CopyOnWriteArrayList<AuctionObserver> observers = new CopyOnWriteArrayList<>();

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

    // =========================
    // QUẢN LÝ CLIENT KẾT NỐI
    // =========================

    // Khi ClientHandler mới kết nối → đăng ký vào đây
    public void addClient(PrintWriter clientOut) {
        if (clientOut != null) {
            connectedClients.add(clientOut);
            System.out.println("Client mới kết nối. Tổng: " + connectedClients.size());
        }
    }

    // Khi client ngắt kết nối → xóa khỏi danh sách
    public void removeClient(PrintWriter clientOut) {
        connectedClients.remove(clientOut);
        System.out.println("Client ngắt kết nối. Còn lại: " + connectedClients.size());
    }

    // Push JSON tới TẤT CẢ client đang kết nối
    // Dùng khi có auction mới, hoặc có bid mới
    public void notifyAllClients(String json) {
        for (PrintWriter client : connectedClients) {
            client.println(json);
        }
        System.out.println("Đã push tới " + connectedClients.size() + " client: " + json);
    }

    // =========================
    // QUẢN LÝ AUCTION
    // =========================

    public void addAuction(Auction auction) {
        if (auction != null && auction.getId() != null) {
            auctions.put(auction.getId(), auction);
        }
    }

    public void addAuction(String key, Auction auction) {
        if (auction != null && key != null) {
            auctions.put(key, auction);
        }
    }

    public Auction findById(String auctionId) {
        return auctions.get(auctionId);
    }

    public List<Auction> getActiveAuctions() {
        return auctions.values().stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .collect(Collectors.toList());
    }

    public void removeAuction(String auctionId) {
        auctions.remove(auctionId);
    }

    // =========================
    // OBSERVER (cho BID realtime)
    // =========================

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(Auction auction) {
        for (AuctionObserver obs : observers) {
            obs.onNewBid(auction);
        }
    }
}