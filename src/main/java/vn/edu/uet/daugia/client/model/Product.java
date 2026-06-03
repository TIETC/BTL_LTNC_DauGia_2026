package vn.edu.uet.daugia.client.model;

import java.time.LocalDateTime;

public class Product {
    private String id;
    private String name;
    private String status;       // RUNNING / FINISHED / CANCELED / OPEN
    private String leader;       // Người đang dẫn đầu
    private double startPrice;
    private double currentPrice;
    private double maxPrice;
    private String description;
    private String imageUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // ---- Constructor đầy đủ (dùng chính) ----
    public Product(String id, String name, String status,
                   double startPrice, double currentPrice, double maxPrice,
                   String description, String imageUrl,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.id           = id;
        this.name         = name;
        this.status       = status != null ? status : "";
        this.leader       = "";
        this.startPrice   = startPrice;
        this.currentPrice = currentPrice;
        this.maxPrice     = maxPrice;
        this.description  = description != null ? description : "";
        this.imageUrl     = imageUrl != null ? imageUrl : "";
        this.startTime    = startTime;
        this.endTime      = endTime;
    }

    // ---- Constructor cũ 8 tham số — giữ để không vỡ code cũ ----
    public Product(String id, String name, String session,
                   double startPrice, double currentPrice,
                   String description,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this(id, name, session, startPrice, currentPrice, 0, description, "", startTime, endTime);
    }

    // ---- Getters ----
    public String getId()               { return id; }
    public String getName()             { return name; }
    public String getStatus()           { return status; }
    public String getLeader()           { return leader != null ? leader : ""; }
    public double getStartPrice()       { return startPrice; }
    public double getCurrentPrice()     { return currentPrice; }
    public double getMaxPrice()         { return maxPrice; }
    public String getDescription()      { return description; }
    public String getImageUrl()         { return imageUrl; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime()   { return endTime; }

    // getSession() giữ lại để không vỡ code cũ còn dùng
    public String getSession()          { return status; }

    // ---- Setters ----
    public void setName(String name)                 { this.name = name; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setMaxPrice(double maxPrice)          { this.maxPrice = maxPrice; }
    public void setImageUrl(String imageUrl)          { this.imageUrl = imageUrl; }
    public void setStatus(String status)              { this.status = status; }
    public void setLeader(String leader)              { this.leader = leader != null ? leader : ""; }
}