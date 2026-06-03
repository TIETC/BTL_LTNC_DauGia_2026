package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.SceneManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;

public class ProductCardController {

    @FXML private ImageView imgProduct;
    @FXML private VBox    vboxPlaceholder;  // hiển thị khi chưa có / đang load ảnh
    @FXML private Label   lblNoImage;
    @FXML private Label   lblName;
    @FXML private Label   lblId;
    @FXML private Label   lblSession;
    @FXML private Label   lblStartPrice;
    @FXML private Label   lblMaxPrice;
    @FXML private HBox    hboxMaxPrice;
    @FXML private Label   lblTimer;
    @FXML private Button  btnJoin;
    @FXML private Button  btnDetail;        // NÚT MỚI

    private Product product;
    private Timeline countdownTimeline;

    // AuctionListController gọi hàm này để truyền dữ liệu vào card
    public void setProduct(Product product) {
        this.product = product;
        populateUI();
    }

    private void populateUI() {
        if (product == null) return;

        lblName.setText(product.getName());
        lblId.setText("Mã: " + product.getId());
        lblSession.setText(product.getSession());
        lblStartPrice.setText(String.format("%,.0f VNĐ", product.getStartPrice()));

        if (product.getMaxPrice() > 0) {
            lblMaxPrice.setText(String.format("%,.0f VNĐ", product.getMaxPrice()));
            hboxMaxPrice.setVisible(true);
            hboxMaxPrice.setManaged(true);
        }

        loadImage(product.getImageUrl());
        startCountdown();
    }

    // ===== FIX: Load ảnh với nhiều định dạng URL =====

    private void loadImage(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            showPlaceholder("Chưa có ảnh");
            return;
        }

        // Thử các dạng URL theo thứ tự ưu tiên
        String directUrl = convertDriveUrl(rawUrl);

        try {
            // Khởi tạo Image với backgroundLoading=true → không block UI thread
            Image img = new Image(directUrl, 200, 150, true, true, true);

            // Lắng nghe khi load xong để ẩn placeholder
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0) {
                    Platform.runLater(() -> {
                        if (!img.isError()) {
                            imgProduct.setImage(img);
                            imgProduct.setVisible(true);
                            vboxPlaceholder.setVisible(false);
                            vboxPlaceholder.setManaged(false);
                        } else {
                            showPlaceholder("Lỗi tải ảnh");
                        }
                    });
                }
            });

            // Nếu ảnh đã cache sẵn (progress = 1 ngay) thì check luôn
            if (img.getProgress() >= 1.0 && !img.isError()) {
                imgProduct.setImage(img);
                imgProduct.setVisible(true);
                vboxPlaceholder.setVisible(false);
                vboxPlaceholder.setManaged(false);
            }

        } catch (Exception e) {
            showPlaceholder("Không tải được ảnh");
            System.err.println("Lỗi load ảnh: " + e.getMessage());
        }
    }

    private void showPlaceholder(String msg) {
        imgProduct.setVisible(false);
        vboxPlaceholder.setVisible(true);
        vboxPlaceholder.setManaged(true);
        lblNoImage.setText(msg);
    }

    /**
     * FIX: Hỗ trợ nhiều dạng URL Drive + URL thông thường
     *
     * Dạng 1: https://drive.google.com/file/d/FILE_ID/view  → dùng thumbnail API
     * Dạng 2: https://drive.google.com/open?id=FILE_ID      → dùng thumbnail API
     * Dạng 3: https://drive.google.com/uc?id=FILE_ID        → giữ nguyên (download)
     * Dạng 4: URL thường (http/https)                       → giữ nguyên
     * Dạng 5: file:///...  (đường dẫn local)                → giữ nguyên
     */
    static String convertDriveUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return rawUrl;

        // Dạng .../file/d/ID/view hoặc .../file/d/ID/preview
        if (rawUrl.contains("drive.google.com/file/d/")) {
            String id = rawUrl.replaceAll(
                    ".*drive\\.google\\.com/file/d/([^/?&]+).*", "$1");
            if (!id.equals(rawUrl)) {
                // Dùng thumbnail: JavaFX load được, không bị redirect
                return "https://drive.google.com/thumbnail?id=" + id + "&sz=w400";
            }
        }

        // Dạng open?id=ID hoặc uc?id=ID
        if (rawUrl.contains("drive.google.com") && rawUrl.contains("id=")) {
            String id = rawUrl.replaceAll(".*[?&]id=([^&]+).*", "$1");
            if (!id.equals(rawUrl)) {
                return "https://drive.google.com/thumbnail?id=" + id + "&sz=w400";
            }
        }

        // URL thường hoặc local path → giữ nguyên
        return rawUrl;
    }

    // ===== Đồng hồ đếm ngược =====

    private void startCountdown() {
        if (product.getEndTime() == null) {
            lblTimer.setText("Không giới hạn");
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            java.time.Duration diff = java.time.Duration.between(
                    LocalDateTime.now(), product.getEndTime());

            if (diff.isNegative() || diff.isZero()) {
                lblTimer.setText("ĐÃ KẾT THÚC");
                lblTimer.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;");
                setEndedState();
                countdownTimeline.stop();
            } else {
                long hours  = diff.toHours();
                int minutes = diff.toMinutesPart();
                int seconds = diff.toSecondsPart();
                lblTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void setEndedState() {
        btnJoin.setDisable(true);
        btnJoin.setText("Đã kết thúc");
        btnJoin.setStyle(
                "-fx-background-color: #bdc3c7; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8;");
    }

    public void stopCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    // ===== NÚT MỚI: Chi tiết sản phẩm =====

    @FXML
    private void handleDetail() {
        if (product == null) return;
        // KHÔNG clear push listener ở đây — AuctionList vẫn cần nhận push
        // khi người dùng back về từ ProductDetail
        ProductDetailController controller = SceneManager.switchSceneAndGetController(
                "/view/ProductDetail.fxml",
                "Chi tiết: " + product.getName());
        if (controller != null) {
            controller.setProductData(product);
        }
    }

    // ===== NÚT CŨ: Tham gia đấu giá =====

    @FXML
    private void handleJoin() {
        if (product == null) return;
        // KHÔNG gọi clearPushListener ở đây nữa
        // → AuctionListController.handleLogout/handleSwitchToSeller mới clear
        BiddingRoomController controller = SceneManager.switchSceneAndGetController(
                "/view/BiddingRoom.fxml",
                "Phòng đấu giá: " + product.getName());
        if (controller != null) {
            controller.setAuctionData(product);
        }
    }
}