package vn.edu.uet.daugia.shared.model.user;

/**
 * Factory Method Pattern – interface Creator cho User.
 *
 * Mỗi loại người dùng (Bidder, Seller, Admin) có ConcreteFactory riêng.
 * Giúp tách logic tạo đối tượng ra khỏi ClientHandler, UserManager…
 */
public interface UserFactory {

    /**
     * Factory Method – tạo một User từ các tham số chuẩn.
     *
     * @param username  Tên đăng nhập
     * @param email     Email
     * @param password  Mật khẩu (raw hoặc đã hash)
     * @param extra     Thông tin đặc thù theo role:
     *                  Bidder  → số dư tài khoản (String của double), ví dụ "1000000"
     *                  Seller  → tên shop, ví dụ "MyShop"
     *                  Admin   → bỏ qua (truyền null)
     * @return          User đã được khởi tạo đúng kiểu
     */
    User createUser(String username, String email, String password, String extra);
}
