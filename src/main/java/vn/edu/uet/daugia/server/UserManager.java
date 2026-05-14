package vn.edu.uet.daugia.server;

import vn.edu.uet.daugia.shared.model.user.Bidder;

public class UserManager {
    // Hàm này tạm thời "Hardcode" (trả về dữ liệu cứng) để code không bị lỗi đỏ.
    // Sau này sẽ sửa nó để móc dữ liệu thật từ Database (MySQL).
    public static Bidder findBidder(String bidderId) {
        // Giả lập tìm thấy người dùng
        return new Bidder("trung", "t@gmail.com", "123", 1000000);
    }
}