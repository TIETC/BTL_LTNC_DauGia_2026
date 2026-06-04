package vn.edu.uet.daugia.shared.model.user;

/**
 * Enum các role người dùng trong hệ thống.
 * Dùng cùng với {@link UserFactoryProvider} để tạo User theo Factory Method.
 */
public enum UserRole {
    BIDDER,   // Người đấu giá
    SELLER,   // Người bán
    ADMIN     // Quản trị viên
}
