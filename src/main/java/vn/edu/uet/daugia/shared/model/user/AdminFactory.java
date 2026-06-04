package vn.edu.uet.daugia.shared.model.user;

/**
 * ConcreteFactory cho Admin.
 *
 * Tham số {@code extra} không dùng đến (Admin không có thông tin đặc thù).
 */
public class AdminFactory implements UserFactory {

    @Override
    public User createUser(String username, String email, String password, String extra) {
        return new Admin(username, email, password);
    }
}
