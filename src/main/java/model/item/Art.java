package model.item;

import java.time.LocalDateTime;

public class Art extends Item {
    private String artist;
    private int creationYear;

    public Art(String id, String name, String description, double startingPrice,
               LocalDateTime startTime, LocalDateTime endTime, String artist, int creationYear) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.artist = artist;
        this.creationYear = creationYear;
    }

    @Override
    public void printInfo() {
        System.out.println("[Art] " + name + " - Tác giả: " + artist + " - Giá hiện tại: " + currentHighestBid);
    }
}