package vn.edu.uet.daugia.client.controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.AlertUtil;
import vn.edu.uet.daugia.client.util.DateTimeParseUtil;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionListController {

    @FXML private TilePane productGrid;
    @FXML private VBox     vboxEmpty;
    @FXML private Label    lblCount;
    @FXML private Button   btnSellerMode;

    // Cache static: giữ danh sách khi back từ BiddingRoom (không gọi server lại)
    private static final List<Product> productCache = new ArrayList<>();

    // Sau logout / đổi tài khoản → bắt buộc GET_AUCTIONS mới từ server
    private static boolean mustReloadFromServer = true;

    /** Gọi từ AppState khi logout hoặc trước login — xóa cache cũ của tài khoản trước. */
    public static void prepareForNewLogin() {
        productCache.clear();
        mustReloadFromServer = true;
    }

    // Danh sách controller của card để stop countdown khi rời màn hình
    private final List<ProductCardController> cardControllers = new ArrayList<>();

    @FXML
    public void initialize() {

        // Ẩn nút Seller với Bidder
        if ("BIDDER".equals(SessionManager.getRole())) {
            if (btnSellerMode != null) {
                btnSellerMode.setVisible(false);
                btnSellerMode.setManaged(false);
            }
        }

        // ===== FIX: Luôn đăng ký lại push listener mỗi khi vào màn hình =====
        // (BiddingRoom đã clear listener, phải đăng ký lại)
        registerPushListener();

        if (mustReloadFromServer || productCache.isEmpty()) {
            // Đăng nhập Bidder mới / vừa logout Seller → luôn hỏi server
            mustReloadFromServer = false;
            loadAuctionsFromServer();
        } else {
            // Chỉ quay lại từ BiddingRoom / Chi tiết — dùng cache
            rebuildGridFromCache();
        }
    }

    // ===== ĐĂNG KÝ PUSH LISTENER (tách ra hàm riêng để gọi lại khi cần) =====

    private void registerPushListener() {
        NetworkClient.getInstance().setPushListener((type, json) -> {
            if ("NEW_AUCTION".equals(type)) {
                try {
                    String id         = json.get("itemId").getAsString();
                    String name       = json.get("itemName").getAsString();
                    double price      = json.get("startPrice").getAsDouble();
                    String endTimeStr = json.get("endTime").getAsString();

                    String imageUrl = "";
                    if (json.has("imageUrl") && !json.get("imageUrl").isJsonNull()) {
                        imageUrl = json.get("imageUrl").getAsString();
                    }
                    double maxPrice = 0;
                    if (json.has("maxPrice") && !json.get("maxPrice").isJsonNull()) {
                        maxPrice = json.get("maxPrice").getAsDouble();
                    }

                    Product p = new Product(
                            id, name, "Đang diễn ra",
                            price, price, maxPrice,
                            "", imageUrl,
                            LocalDateTime.now(),
                            DateTimeParseUtil.parseFlexible(endTimeStr));

                    Platform.runLater(() -> {
                        // Kiểm tra tránh thêm trùng
                        boolean exists = productCache.stream()
                                .anyMatch(existing -> existing.getId().equals(id));
                        if (!exists) {
                            productCache.add(p);
                            addCardToGrid(p);
                            updateCountBadge();
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Lỗi xử lý NEW_AUCTION push: " + e.getMessage());
                }
            }
        });
    }

    // ===== RENDER LẠI TỪ CACHE (khi back về) =====

    private void rebuildGridFromCache() {
        // Xóa card cũ trên màn hình nhưng GIỮ cache
        for (ProductCardController c : cardControllers) c.stopCountdown();
        cardControllers.clear();
        productGrid.getChildren().clear();

        for (Product p : productCache) {
            addCardToGrid(p);
        }
        updateCountBadge();
        System.out.println("Rebuild từ cache: " + productCache.size() + " SP");
    }

    // ===== LOAD TỪ SERVER (chỉ gọi lần đầu) =====

    private void loadAuctionsFromServer() {
        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendRaw("{\"type\":\"GET_AUCTIONS\"}");
                String response = NetworkClient.getInstance().readResponse();
                System.out.println("GET_AUCTIONS response: " + response);

                if (response == null || response.trim().isEmpty()) {
                    Platform.runLater(() -> {
                        showEmptyState();
                        AlertUtil.showError("Lỗi", "Không nhận được danh sách từ server!");
                    });
                    return;
                }

                String trimmed = response.trim();
                // Phản hồi đúng phải là JSON mảng — nếu không, thường là đọc nhầm hàng đợi socket
                if (!trimmed.startsWith("[")) {
                    System.err.println("GET_AUCTIONS sai định dạng: " + trimmed);
                    Platform.runLater(() -> AlertUtil.showError("Lỗi đồng bộ",
                            "Dữ liệu danh sách không hợp lệ. Hãy đăng xuất và đăng nhập lại."));
                    return;
                }

                if (trimmed.equals("[]")) {
                    Platform.runLater(this::showEmptyState);
                    return;
                }

                Gson gson = new Gson();
                JsonObject[] auctions = gson.fromJson(trimmed, JsonObject[].class);

                Platform.runLater(() -> {
                    productCache.clear();
                    for (ProductCardController c : cardControllers) c.stopCountdown();
                    cardControllers.clear();
                    productGrid.getChildren().clear();

                    for (JsonObject a : auctions) {
                        try {
                            String id         = a.get("itemId").getAsString();
                            String name       = a.get("itemName").getAsString();
                            double price      = a.get("startPrice").getAsDouble();
                            String endTimeStr = a.get("endTime").getAsString();

                            String imageUrl = "";
                            if (a.has("imageUrl") && !a.get("imageUrl").isJsonNull()) {
                                imageUrl = a.get("imageUrl").getAsString();
                            }
                            double maxPrice = 0;
                            if (a.has("maxPrice") && !a.get("maxPrice").isJsonNull()) {
                                maxPrice = a.get("maxPrice").getAsDouble();
                            }

                            Product p = new Product(
                                    id, name, "Đang diễn ra",
                                    price, price, maxPrice,
                                    "", imageUrl,
                                    LocalDateTime.now(),
                                    DateTimeParseUtil.parseFlexible(endTimeStr));

                            productCache.add(p);
                            addCardToGrid(p);
                        } catch (Exception ex) {
                            System.err.println("Lỗi parse auction: " + ex.getMessage());
                        }
                    }
                    updateCountBadge();
                    System.out.println("Đã load " + productCache.size() + " phiên đấu giá");
                });

            } catch (Exception e) {
                System.err.println("Lỗi load danh sách auction: " + e.getMessage());
                Platform.runLater(this::showEmptyState);
            }
        }).start();
    }

    // ===== TẠO VÀ THÊM CARD =====

    private void addCardToGrid(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/ProductCard.fxml"));
            Node cardNode = loader.load();
            ProductCardController cardCtrl = loader.getController();
            cardCtrl.setProduct(product);
            cardControllers.add(cardCtrl);
            productGrid.getChildren().add(cardNode);
            hideEmptyState();
        } catch (Exception e) {
            System.err.println("Lỗi load ProductCard.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== TIỆN ÍCH =====

    private void updateCountBadge() {
        int count = productCache.size();
        if (lblCount != null) {
            lblCount.setText(count + " sản phẩm");
        }
        if (count == 0) showEmptyState();
        else hideEmptyState();
    }

    private void showEmptyState() {
        if (vboxEmpty != null) { vboxEmpty.setVisible(true); vboxEmpty.setManaged(true); }
    }

    private void hideEmptyState() {
        if (vboxEmpty != null) { vboxEmpty.setVisible(false); vboxEmpty.setManaged(false); }
    }

    // ===== ĐIỀU HƯỚNG =====

    @FXML
    protected void handleSwitchToSeller() {
        stopAllCountdowns();
        // Clear listener khi thực sự rời màn hình
        NetworkClient.getInstance().clearPushListener();
        SceneManager.switchScene("/view/SellerDashboard.fxml", "Quản lý sản phẩm");
    }

    @FXML
    protected void handleLogout() {
        stopAllCountdowns();
        NetworkClient.getInstance().clearPushListener();
        // SessionManager.logout() → AppState dọn cache + hàng đợi socket
        SessionManager.logout();
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập");
    }

    private void stopAllCountdowns() {
        for (ProductCardController c : cardControllers) c.stopCountdown();
    }
}