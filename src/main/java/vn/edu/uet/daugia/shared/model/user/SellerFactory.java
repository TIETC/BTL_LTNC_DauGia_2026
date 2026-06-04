package vn.edu.uet.daugia.shared.model.user;

/**
 * ConcreteFactory cho Seller.
 *
 * Tham số {@code extra}: tên shop.
 *   - "MyShop"  → shopName = "MyShop"
 *   - null/rỗng → shopName = "" (chưa đặt tên)
 */
public class SellerFactory implements UserFactory {

    @Override
    public User createUser(String username, String email, String password, String extra) {
        String shopName = (extra != null) ? extra.trim() : "";
        return new Seller(username, email, password, shopName);
    }
}
