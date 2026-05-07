package vn.edu.uet.daugia.model.user;

public class Bidder extends User {
    private double balance;

    public Bidder(String username, String email, String password, double balance) {
        super(username, email, password);
        this.balance = balance;
    }
    public boolean hasSufficientBalance(double amount) {
        return this.balance >= amount;
    }
    public void deductBalance(double amount) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Số dư tài khoản không đủ để thanh toán!");
        }
        this.balance -= amount;
    }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    @Override
    public String getInfo() {
        return super.getInfo() + " | Số dư: " + balance + " VND";
    }
}