package vn.edu.uet.daugia.client.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private String id;
    private String name;
    private String session;
    private double startPrice;
    private double currentPrice;
    private double maxPrice;       // MỚI: Giá mua đứt (Giá Max)
    private String description;
    private String imageUrl;       // MỚI: Link ảnh Google Drive
    private LocalDateTime endTime;
    private LocalDateTime startTime;

    // ==========================================
    // === BỔ SUNG MỚI: LỊCH SỬ TĂNG GIÁ ===
    // ==========================================
    private List<String> bidHistory = new ArrayList<>();

    // Constructor MỚI (đầy đủ 10 tham số) - dùng cho SellerDashboard khi có maxPrice + imageUrl
    public Product(String id, String name, String session,
                   double startPrice, double currentPrice, double maxPrice,
                   String description, String imageUrl,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.session = session;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.maxPrice = maxPrice;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Constructor CŨ (8 tham số) - GIỮ NGUYÊN để không vỡ các chỗ khác đang gọi
    public Product(String id, String name, String session,
                   double startPrice, double currentPrice,
                   String description,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this(id, name, session, startPrice, currentPrice, 0, description, "", startTime, endTime);
    }

    // ---- Getters cũ - GIỮ NGUYÊN ----
    public String getId()               { return id; }
    public String getName()             { return name; }
    public String getSession()          { return session; }
    public double getStartPrice()       { return startPrice; }
    public double getCurrentPrice()     { return currentPrice; }
    public String getDescription()      { return description; }
    public LocalDateTime getEndTime()   { return endTime; }
    public LocalDateTime getStartTime() { return startTime; }

    // ---- Getters MỚI ----
    public double getMaxPrice()  { return maxPrice; }
    public String getImageUrl()  { return imageUrl; }

    // ==========================================
    // === BỔ SUNG MỚI: GETTER/SETTER LỊCH SỬ ===
    // ==========================================
    public List<String> getBidHistory() { return bidHistory; }
    public void addBidHistory(String log) { this.bidHistory.add(log); }

    // ---- Setters - GIỮ NGUYÊN + THÊM MỚI ----
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setMaxPrice(double maxPrice)          { this.maxPrice = maxPrice; }
    public void setImageUrl(String imageUrl)          { this.imageUrl = imageUrl; }
}