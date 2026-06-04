package vn.edu.uet.daugia.shared.model.user;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry tập trung: map UserRole → UserFactory.
 *
 * Cách dùng ở bất kỳ đâu trong code (thay thế {@code new Bidder(…)},
 * {@code new Seller(…)}, {@code new Admin(…)} trực tiếp):
 *
 * <pre>
 *   // Tạo Bidder với balance 500_000
 *   User u = UserFactoryProvider.create(UserRole.BIDDER, username, email, password, "500000");
 *
 *   // Tạo Seller với tên shop lấy từ DB
 *   User u = UserFactoryProvider.create(UserRole.SELLER, username, email, password, shopName);
 *
 *   // Tạo từ chuỗi role lấy từ DB/JSON (ví dụ: "BIDDER", "seller")
 *   User u = UserFactoryProvider.createFromString(role, username, email, password, extra);
 * </pre>
 */
public class UserFactoryProvider {

    private static final Map<UserRole, UserFactory> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put(UserRole.BIDDER, new BidderFactory());
        REGISTRY.put(UserRole.SELLER, new SellerFactory());
        REGISTRY.put(UserRole.ADMIN,  new AdminFactory());
    }

    /**
     * Tạo User theo role đã biết.
     *
     * @throws IllegalArgumentException nếu role chưa được đăng ký
     */
    public static User create(UserRole role,
                              String username, String email, String password, String extra) {
        UserFactory factory = REGISTRY.get(role);
        if (factory == null) {
            throw new IllegalArgumentException("Chưa có factory cho role: " + role);
        }
        return factory.createUser(username, email, password, extra);
    }

    /**
     * Tạo User từ chuỗi tên role (lấy từ DB / JSON).
     * Mặc định là BIDDER nếu không nhận ra role.
     */
    public static User createFromString(String roleName,
                                        String username, String email, String password, String extra) {
        UserRole role;
        try {
            role = UserRole.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            role = UserRole.BIDDER; // fallback mặc định
        }
        return create(role, username, email, password, extra);
    }

    /** Tiện ích: tạo Bidder nhanh (hay dùng nhất). */
    public static Bidder createBidder(String username, String email, String password, double balance) {
        return (Bidder) create(UserRole.BIDDER, username, email, password, String.valueOf(balance));
    }

    /** Tiện ích: tạo Seller nhanh. */
    public static Seller createSeller(String username, String email, String password, String shopName) {
        return (Seller) create(UserRole.SELLER, username, email, password, shopName);
    }

    /** Tiện ích: tạo Admin nhanh. */
    public static Admin createAdmin(String username, String email, String password) {
        return (Admin) create(UserRole.ADMIN, username, email, password, null);
    }
}
