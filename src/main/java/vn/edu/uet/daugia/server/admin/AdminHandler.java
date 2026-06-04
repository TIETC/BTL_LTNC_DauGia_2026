package vn.edu.uet.daugia.server.admin;

import com.google.gson.JsonObject;

import java.io.PrintWriter;

/**
 * AdminHandler – định tuyến (route) các lệnh ADMIN từ ClientHandler.
 *
 * ClientHandler gọi {@link #handle(JsonObject, PrintWriter)} mỗi khi
 * nhận được một JSON có role = "ADMIN". Tất cả logic nghiệp vụ nằm
 * trong {@link AdminService}.
 *
 * Danh sách lệnh hỗ trợ (field "type"):
 * ┌──────────────────────────────┬──────────────────────────────────────────┐
 * │ type                         │ Tham số JSON bắt buộc                    │
 * ├──────────────────────────────┼──────────────────────────────────────────┤
 * │ ADMIN_GET_ALL_AUCTIONS       │ –                                        │
 * │ ADMIN_DELETE_AUCTION         │ auctionId                                │
 * │ ADMIN_CANCEL_AUCTION         │ auctionId                                │
 * │ ADMIN_GET_ALL_BIDDERS        │ –                                        │
 * │ ADMIN_BAN_BIDDER             │ targetUsername                           │
 * │ ADMIN_UNBAN_BIDDER           │ targetUsername                           │
 * │ ADMIN_DELETE_BIDDER          │ targetUsername                           │
 * │ ADMIN_GET_ALL_SELLERS        │ –                                        │
 * │ ADMIN_BAN_SELLER             │ targetUsername                           │
 * │ ADMIN_UNBAN_SELLER           │ targetUsername                           │
 * │ ADMIN_DELETE_SELLER_AUCTIONS │ sellerName                               │
 * │ ADMIN_GET_STATS              │ –                                        │
 * └──────────────────────────────┴──────────────────────────────────────────┘
 */
public class AdminHandler {

    private final AdminService adminService = new AdminService();

    /**
     * Xử lý một lệnh admin.
     *
     * @param obj JSON object đã parse từ client
     * @param out PrintWriter để ghi phản hồi về client
     * @return true nếu đây là lệnh admin (đã xử lý), false nếu không phải
     */
    public boolean handle(JsonObject obj, PrintWriter out) {
        String type = obj.has("type") ? obj.get("type").getAsString() : "";

        switch (type) {

            // ── Phiên đấu giá ──────────────────────────────────────────────
            case "ADMIN_GET_ALL_AUCTIONS" -> {
                out.println(adminService.handleGetAllAuctions());
                return true;
            }
            case "ADMIN_DELETE_AUCTION" -> {
                String id = getString(obj, "auctionId");
                out.println(adminService.handleAdminDeleteAuction(id));
                return true;
            }
            case "ADMIN_CANCEL_AUCTION" -> {
                String id = getString(obj, "auctionId");
                out.println(adminService.handleAdminCancelAuction(id));
                return true;
            }

            // ── Bidder ──────────────────────────────────────────────────────
            case "ADMIN_GET_ALL_BIDDERS" -> {
                out.println(adminService.handleGetAllBidders());
                return true;
            }
            case "ADMIN_BAN_BIDDER" -> {
                out.println(adminService.handleBanBidder(getString(obj, "targetUsername")));
                return true;
            }
            case "ADMIN_UNBAN_BIDDER" -> {
                out.println(adminService.handleUnbanBidder(getString(obj, "targetUsername")));
                return true;
            }
            case "ADMIN_DELETE_BIDDER" -> {
                out.println(adminService.handleDeleteBidder(getString(obj, "targetUsername")));
                return true;
            }

            // ── Seller ──────────────────────────────────────────────────────
            case "ADMIN_GET_ALL_SELLERS" -> {
                out.println(adminService.handleGetAllSellers());
                return true;
            }
            case "ADMIN_BAN_SELLER" -> {
                out.println(adminService.handleBanSeller(getString(obj, "targetUsername")));
                return true;
            }
            case "ADMIN_UNBAN_SELLER" -> {
                out.println(adminService.handleUnbanSeller(getString(obj, "targetUsername")));
                return true;
            }
            case "ADMIN_DELETE_SELLER_AUCTIONS" -> {
                out.println(adminService.handleDeleteAllAuctionsBySeller(
                        getString(obj, "sellerName")));
                return true;
            }

            // ── Thống kê ────────────────────────────────────────────────────
            case "ADMIN_GET_STATS" -> {
                out.println(adminService.handleGetSystemStats());
                return true;
            }

            default -> { return false; } // Không phải lệnh admin → để ClientHandler xử lý
        }
    }

    private String getString(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsString() : "";
    }
}
