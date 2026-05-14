package vn.edu.uet.daugia.client.model;

import java.time.LocalDateTime;

public class Product {
    private String id;
    private String name;
    private String session;
    private double startPrice;
    private double currentPrice;
    private String description;
    private LocalDateTime endTime;
    private LocalDateTime startTime;

    public Product(String id, String name, String session, double startPrice, double currentPrice, String description,LocalDateTime startTime,LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.session = session;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSession() { return session; }
    public double getStartPrice() { return startPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public String getDescription() { return description; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getStartTime() { return startTime; }

    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
}