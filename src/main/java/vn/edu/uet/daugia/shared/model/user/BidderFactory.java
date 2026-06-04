package vn.edu.uet.daugia.shared.model.user;

/**
 * ConcreteFactory cho Bidder.
 *
 * Tham số {@code extra}: số dư tài khoản ban đầu dạng chuỗi.
 *   - "1000000"  → balance = 1_000_000
 *   - null/rỗng  → balance = 0 (mặc định)
 */
public class BidderFactory implements UserFactory {

    private static final double DEFAULT_BALANCE = 0;

    @Override
    public User createUser(String username, String email, String password, String extra) {
        double balance = DEFAULT_BALANCE;
        if (extra != null && !extra.isBlank()) {
            try {
                balance = Double.parseDouble(extra.trim());
            } catch (NumberFormatException ignored) { }
        }
        return new Bidder(username, email, password, balance);
    }
}
