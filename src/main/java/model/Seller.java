package model;
public class Seller extends User {
    private double ratingScore;
    public Seller(String username, String password, String email, double ratingScore) {
        super(username, password, email);
        this.ratingScore = ratingScore;
    }
    public double getRatingScore() {
        return ratingScore;
    }
    public void setRatingScore(double ratingScore) {
        this.ratingScore = ratingScore;
    }
    public void createItem(String itemName) {
        System.out.println("Seller " + this.username + " vừa đăng bán món hàng: " + itemName);
    }
}