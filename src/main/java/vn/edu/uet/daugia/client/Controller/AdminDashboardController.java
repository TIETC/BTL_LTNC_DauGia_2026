package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.AlertUtil;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminDashboardController {

    // ===== TAB THỐNG KÊ =====
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalBidders;
    @FXML private Label lblTotalSellers;
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblRunning;
    @FXML private Label lblFinished;
    @FXML private Label lblCanceled;

    // ===== TAB PHIÊN ĐẤU GIÁ =====
    @FXML private TableView<JsonObject>           tblAuctions;
    @FXML private TableColumn<JsonObject, String> colAucId, colAucName, colAucSeller,
            colAucStatus, colAucStart, colAucEnd;
    @FXML private TableColumn<JsonObject, String> colAucPrice;

    // ===== DATA =====
    private final ObservableList<JsonObject> auctionList = FXCollections.observableArrayList();

    private final Gson gson = new Gson();

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {
        if (!"ADMIN".equals(SessionManager.getRole())) {
            Platform.runLater(() -> {
                AlertUtil.showError("Bảo mật", "Bạn không có quyền Admin!");
                SceneManager.switchScene("/view/Login.fxml", "Đăng nhập");
            });
            return;
        }

        setupAuctionTable();

        handleRefreshStats();
        handleRefreshAuctions();
    }

    // =========================================================
    // SETUP TABLES
    // =========================================================

    private void setupAuctionTable() {
        colAucId.setCellValueFactory(d -> new SimpleStringProperty(safeGet(d.getValue(), "id")));
        colAucName.setCellValueFactory(d -> new SimpleStringProperty(safeGet(d.getValue(), "itemName")));
        colAucSeller.setCellValueFactory(d -> new SimpleStringProperty(safeGet(d.getValue(), "sellerName")));
        colAucStatus.setCellValueFactory(d -> new SimpleStringProperty(safeGet(d.getValue(), "status")));
        colAucPrice.setCellValueFactory(d -> new SimpleStringProperty(
                formatPrice(safeGetDouble(d.getValue(), "currentPrice"))));
        colAucStart.setCellValueFactory(d -> new SimpleStringProperty(safeGet(d.getValue(), "startTime")));
        colAucEnd.setCellValueFactory(d -> new SimpleStringProperty(safeGet(d.getValue(), "endTime")));

        colAucStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                switch (v) {
                    case "RUNNING"  -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "FINISHED" -> setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
                    case "CANCELED" -> setStyle("-fx-text-fill: #e74c3c;");
                    default         -> setStyle("");
                }
            }
        });

        tblAuctions.setItems(auctionList);
    }

    // =========================================================
    // THỐNG KÊ
    // =========================================================

    @FXML
    public void handleRefreshStats() {
        sendAsync("{\"type\":\"ADMIN_GET_STATS\"}", response -> {
            try {
                JsonObject stats = gson.fromJson(response, JsonObject.class);
                Platform.runLater(() -> {
                    lblTotalUsers.setText(safeGet(stats, "totalUsers"));
                    lblTotalBidders.setText(safeGet(stats, "totalBidders"));
                    lblTotalSellers.setText(safeGet(stats, "totalSellers"));
                    lblTotalAuctions.setText(safeGet(stats, "totalAuctions"));
                    lblRunning.setText(safeGet(stats, "runningAuctions"));
                    lblFinished.setText(safeGet(stats, "finishedAuctions"));
                    lblCanceled.setText(safeGet(stats, "canceledAuctions"));
                });
            } catch (Exception e) {
                showErr("Lỗi tải thống kê: " + e.getMessage());
            }
        });
    }

    // =========================================================
    // QUẢN LÝ PHIÊN ĐẤU GIÁ
    // =========================================================

    @FXML
    public void handleRefreshAuctions() {
        sendAsync("{\"type\":\"ADMIN_GET_ALL_AUCTIONS\"}", response -> {
            try {
                JsonObject[] list = gson.fromJson(response, JsonObject[].class);
                Platform.runLater(() -> {
                    auctionList.clear();
                    if (list != null) auctionList.addAll(list);
                });
            } catch (Exception e) {
                showErr("Lỗi tải danh sách phiên: " + e.getMessage());
            }
        });
    }

    @FXML
    public void handleCancelAuction() {
        JsonObject selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("Chưa chọn", "Vui lòng chọn phiên cần hủy!");
            return;
        }
        String id   = safeGet(selected, "id");
        String name = safeGet(selected, "itemName");

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận hủy phiên");
        confirm.setHeaderText("Hủy phiên đấu giá: \"" + name + "\"?");
        confirm.setContentText("Phiên sẽ bị đánh dấu CANCELED. Lịch sử vẫn được giữ lại.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String json = String.format("{\"type\":\"ADMIN_CANCEL_AUCTION\",\"auctionId\":\"%s\"}", id);
            sendAsync(json, response -> {
                if (response != null && response.contains("\"status\":\"OK\"")) {
                    Platform.runLater(() -> {
                        AlertUtil.showSuccess("Thành công", "Đã hủy phiên: " + name);
                        handleRefreshAuctions();
                    });
                } else {
                    showErr("Hủy thất bại: " + response);
                }
            });
        });
    }

    @FXML
    public void handleDeleteAuction() {
        JsonObject selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("Chưa chọn", "Vui lòng chọn phiên cần xóa!");
            return;
        }
        String id   = safeGet(selected, "id");
        String name = safeGet(selected, "itemName");

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa phiên");
        confirm.setHeaderText("XÓA VĨNH VIỄN phiên: \"" + name + "\"?");
        confirm.setContentText("⚠ Hành động này không thể hoàn tác. Toàn bộ lịch sử đặt giá sẽ bị xóa.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String json = String.format("{\"type\":\"ADMIN_DELETE_AUCTION\",\"auctionId\":\"%s\"}", id);
            sendAsync(json, response -> {
                if (response != null && response.contains("\"status\":\"OK\"")) {
                    Platform.runLater(() -> {
                        AlertUtil.showSuccess("Thành công", "Đã xóa phiên: " + name);
                        handleRefreshAuctions();
                    });
                } else {
                    showErr("Xóa thất bại: " + response);
                }
            });
        });
    }

    // =========================================================
    // ĐIỀU HƯỚNG
    // =========================================================

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập hệ thống");
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private void sendAsync(String json, java.util.function.Consumer<String> onResponse) {
        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendRaw(json);
                String response = NetworkClient.getInstance().readResponse();
                onResponse.accept(response);
            } catch (Exception e) {
                showErr("Lỗi kết nối: " + e.getMessage());
            }
        }).start();
    }

    private void showErr(String msg) {
        Platform.runLater(() -> AlertUtil.showError("Lỗi", msg));
    }

    private String safeGet(JsonObject obj, String key) {
        try {
            return obj.has(key) && !obj.get(key).isJsonNull()
                    ? obj.get(key).getAsString() : "";
        } catch (Exception e) { return ""; }
    }

    private double safeGetDouble(JsonObject obj, String key) {
        try {
            return obj.has(key) ? obj.get(key).getAsDouble() : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private String formatPrice(double value) {
        return String.format("%,.0f VNĐ", value);
    }
}