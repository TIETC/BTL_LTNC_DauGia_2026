package vn.edu.uet.daugia.model.user;

public class Seller extends User {
    private String shopName;

    public Seller(String username, String email, String password, String shopName) {
        super(username, email, password);
        this.shopName = shopName;
    }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    @Override
    public String getRole() {
        return "SELLER";
    }
    @Override
    public String getInfo() {
        return super.getInfo() + " | Tên Shop: " + shopName;
    }
}