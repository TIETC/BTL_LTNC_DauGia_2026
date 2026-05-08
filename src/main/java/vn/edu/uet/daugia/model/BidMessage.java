package vn.edu.uet.daugia.model;

public class BidMessage {

    private String type;
    private String username;
    private int price;

    public BidMessage() {
    }

    public BidMessage(String type,
                      String username,
                      int price) {

        this.type = type;
        this.username = username;
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public int getPrice() {
        return price;
    }
}