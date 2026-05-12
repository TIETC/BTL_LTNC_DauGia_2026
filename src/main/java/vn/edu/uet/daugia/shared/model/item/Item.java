package vn.edu.uet.daugia.shared.model.item;

import java.time.LocalDateTime;

public abstract class Item {
    protected String id;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentHighestBid;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    public Item(String id, String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public abstract void printInfo();

    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }
}