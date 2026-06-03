package com.example.list;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import vn.edu.uet.daugia.client.NetworkClient;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProductDetailController implements ProductDataReceiver {

    @FXML private Label lblProductId, lblProductName, lblCurrentPrice, lblStartTime, lblEndTime, lblDescription, lblTimer;
    @FXML private ImageView imgProduct;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;

    private MainDashboardController dashboardController;
    private String currentUsername;
    private String currentUserRole;
    private JsonObject currentProduct;
    private NetworkClient networkClient = new NetworkClient();
    private Timeline pollingTimeline;
    private int tickCount = 0;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void setDashboardController(MainDashboardController controller, String username, String role) {
        this.dashboardController = controller;
        this.currentUsername = username;
        this.currentUserRole = role;
    }

    // [CÔNG CỤ NÂNG CẤP]: Hàm lấy chuỗi an toàn tuyệt đối, chống lỗi sập do NULL
    private String getSafeString(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultValue;
    }

    @Override
    public void setProductDetails(JsonObject productData) {
        this.currentProduct = productData;

        lblProductId.setText(getSafeString(productData, "item_id", "--"));
        lblProductName.setText(getSafeString(productData, "name", "Chưa có tên"));

        double price = productData.has("starting_price") ? productData.get("starting_price").getAsDouble() : 0;
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", price));

        String start = getSafeString(productData, "start_time", "--");
        String end = getSafeString(productData, "end_time", "--");
        lblStartTime.setText(start);
        lblEndTime.setText(end);

        lblDescription.setText(getSafeString(productData, "description", "Chưa có mô tả."));

        String imagePathStr = getSafeString(productData, "image_path", "");
        try {
            File imageFile = new File(imagePathStr);
            if (imageFile.exists()) imgProduct.setImage(new Image(imageFile.toURI().toString()));
        } catch (Exception e) {}

        txtBidAmount.setText(String.format("%.0f", price + (price * 0.1)));

        // [TÍNH NĂNG MỚI]: Khóa ô Đặt Giá nếu là Người Bán HOẶC đang xem chính sản phẩm của mình
        String itemSeller = getSafeString(productData, "seller_name", "");
        if ("SELLER".equals(currentUserRole) || currentUsername.equals(itemSeller)) {
            txtBidAmount.setDisable(true);
            btnPlaceBid.setDisable(true);
            txtBidAmount.setPromptText("Tính năng đặt giá chỉ dành cho Người Mua");
            txtBidAmount.setText(""); // Xóa gợi ý giá đi để giao diện sạch sẽ
            btnPlaceBid.setText("CHỈ XEM");
        }

        if (!start.equals("--") && !end.equals("--")) {
            startCountdownAndPolling(start, end);
        }
    }

    private void startCountdownAndPolling(String startTimeStr, String endTimeStr) {
        if (pollingTimeline != null) pollingTimeline.stop();

        try {
            LocalDateTime start = LocalDateTime.parse(startTimeStr, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTimeStr, DATE_TIME_FORMATTER);

            pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                LocalDateTime now = LocalDateTime.now();

                // 1. Cập nhật Đồng hồ
                if (now.isBefore(start)) {
                    lblTimer.setText("CHƯA BẮT ĐẦU");
                    lblTimer.setStyle("-fx-text-fill: #7f8c8d;");
                    btnPlaceBid.setDisable(true); txtBidAmount.setDisable(true);
                } else if (now.isAfter(end)) {
                    lblTimer.setText("ĐÃ KẾT THÚC");
                    lblTimer.setStyle("-fx-text-fill: #e74c3c;");
                    btnPlaceBid.setDisable(true); txtBidAmount.setDisable(true);
                } else {
                    java.time.Duration diff = java.time.Duration.between(now, end);
                    lblTimer.setText(String.format("%02d:%02d:%02d", diff.toHours(), diff.toMinutesPart(), diff.toSecondsPart()));
                    lblTimer.setStyle("-fx-text-fill: #27ae60;");

                    // Chỉ mở khóa nếu tài khoản không phải là Người Bán
                    String itemSeller = getSafeString(currentProduct, "seller_name", "");
                    if (!"SELLER".equals(currentUserRole) && !currentUsername.equals(itemSeller)) {
                        btnPlaceBid.setDisable(false);
                        txtBidAmount.setDisable(false);
                    }
                }

                // 2. Radar cập nhật giá liên tục
                tickCount++;
                if (tickCount >= 2) {
                    tickCount = 0;
                    fetchLatestPrice();
                }
            }));
            pollingTimeline.setCycleCount(Timeline.INDEFINITE);
            pollingTimeline.play();
        } catch (Exception e) { lblTimer.setText("Lỗi giờ"); }
    }

    private void fetchLatestPrice() {
        if (!networkClient.connect()) return;
        JsonObject req = new JsonObject();
        req.addProperty("type", "GET_ITEM");
        req.addProperty("item_id", currentProduct.get("item_id").getAsString());

        String res = networkClient.sendRequest(req.toString());
        if (res != null && !res.isEmpty()) {
            JsonObject resObj = new Gson().fromJson(res, JsonObject.class);
            if (resObj.get("status").getAsString().equals("SUCCESS")) {
                double latestPrice = resObj.getAsJsonObject("data").get("starting_price").getAsDouble();
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", latestPrice));
                currentProduct.addProperty("starting_price", latestPrice);
            }
        }
    }

    @FXML
    private void handlePlaceBid() {
        try {
            double bidAmount = Double.parseDouble(txtBidAmount.getText().trim());
            double currentPrice = currentProduct.get("starting_price").getAsDouble();

            if (bidAmount <= currentPrice) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bạn phải đặt giá cao hơn giá hiện tại!");
                return;
            }

            if (!networkClient.connect()) return;

            JsonObject req = new JsonObject();
            req.addProperty("type", "BID");
            req.addProperty("auctionId", currentProduct.get("item_id").getAsString());
            req.addProperty("bidderId", currentUsername);
            req.addProperty("price", bidAmount);

            String res = networkClient.sendRequest(req.toString());
            if (res != null && res.contains("SUCCESS")) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đặt giá thành công!");
                fetchLatestPrice();

                // Cập nhật lại gợi ý mức giá an toàn cho người tiếp theo
                txtBidAmount.setText(String.format("%.0f", bidAmount + (bidAmount * 0.1)));
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đặt giá lúc này!");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ!");
        }
    }

    @FXML
    private void handleBackToList() {
        if (pollingTimeline != null) pollingTimeline.stop();
        if (dashboardController != null) dashboardController.showAuctionList(false);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }
}