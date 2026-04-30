package model;
public class Bidder extends User {
    private double accountBalance;
    public Bidder(String username, String password, String email, double accountBalance) {
        super(username, password, email);
        this.accountBalance = accountBalance;
    }
    public double getAccountBalance() {
        return accountBalance;
    }
    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }
    public void placeBid(double bidAmount) {
        System.out.println("Bidder " + this.username + " vừa đặt giá: " + bidAmount);
    }
}