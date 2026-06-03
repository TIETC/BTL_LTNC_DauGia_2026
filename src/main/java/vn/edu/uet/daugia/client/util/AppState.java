package vn.edu.uet.daugia.client.util;

import vn.edu.uet.daugia.client.Controller.AuctionListController;
import vn.edu.uet.daugia.client.network.NetworkClient;

/**
 * Trạng thái client dùng chung khi đổi tài khoản / đăng xuất.
 * Tránh lẫn phản hồi TCP giữa Seller (tạo SP) và Bidder (xem danh sách).
 */
public final class AppState {

    private AppState() {}

    /** Gọi trước LOGIN hoặc ngay sau LOGOUT — xóa hàng đợi tin cũ + bắt buộc tải lại danh sách SP. */
    public static void resetForAccountSwitch() {
        NetworkClient.getInstance().clearPendingResponses();
        AuctionListController.prepareForNewLogin();
    }
}
