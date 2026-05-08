package vn.edu.uet.daugia.model.item;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics(String id, String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, String brand, int warrantyMonths) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("[Electronics] " + name + " - Hãng: " + brand + " - Giá hiện tại: " + currentHighestBid);
    }
}