package com.example.list;

import java.time.LocalDateTime;

public class Product {
    private String id;
    private String name;
    private String session;
    private String startPrice;
    private String currentPrice;
    private String description;
    private LocalDateTime endTime;

    public Product(String id, String name, String session, String startPrice, String currentPrice, String description, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.session = session;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.description = description;
        this.endTime = endTime;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSession() { return session; }
    public String getStartPrice() { return startPrice; }
    public String getCurrentPrice() { return currentPrice; }
    public String getDescription() { return description; }
    public LocalDateTime getEndTime() { return endTime; }

    public void setCurrentPrice(String currentPrice) { this.currentPrice = currentPrice; }
}