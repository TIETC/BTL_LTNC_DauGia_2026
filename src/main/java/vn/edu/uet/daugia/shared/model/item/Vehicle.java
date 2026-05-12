package vn.edu.uet.daugia.shared.model.item;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String licensePlate;

    public Vehicle(String id, String name, String description, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime, String licensePlate) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.licensePlate = licensePlate;
    }

    @Override
    public void printInfo() {
        System.out.println("[Vehicle] " + name + " - Biển số: " + licensePlate + " - Giá hiện tại: " + currentHighestBid);
    }
}