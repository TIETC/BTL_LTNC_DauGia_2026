package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.AlertUtil;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager;
import vn.edu.uet.daugia.shared.model.BidMessage;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView; // BỔ SUNG MỚI
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BiddingRoomController {

    // ===== FXML CŨ (GIỮ NGUYÊN) =====
    @FXML private Label lblName, lblId, lblSession, lblDescription, lblCurrentPrice, lblTimer;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;

    // ===== FXML MỚI =====
    @FXML private LineChart<String, Number> priceChart;
    @FXML private ImageView productImageView;
    @FXML private Label lblStartPrice;
    @FXML private Label lblMaxPrice;

    // ==========================================
    // === BỔ SUNG MỚI: GIAO DIỆN LỊCH SỬ ===
    // ==========================================
    @FXML private ListView<String> listHistory;

    // ===== BIẾN NỘI BỘ =====
    private XYChart.Series<String, Number> priceSeries;
    private Product product;
    private Timeline timeline;
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // ===== ENTRY POINTS (GIỮ NGUYÊN CẢ 2 HÀM) =====
    public void setAuctionData(Product product) {
        this.product = product;
        setupUI();
    }

    public void setProductData(Product product) {
        this.product = product;
        setupUI();
    }

    // ===== SETUP UI =====
    private void setupUI() {
        if (product == null) return;

        lblName.setText(product.getName());
        lblId.setText("Mã SP: " + product.getId());
        lblSession.setText("Phiên: " + product.getSession());
        lblDescription.setText("Mô tả: " + product.getDescription());
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));

        lblStartPrice.setText(String.format("%,.0f VNĐ", product.getStartPrice()));

        if (product.getMaxPrice() > 0) {
            lblMaxPrice.setText(String.format("%,.0f VNĐ", product.getMaxPrice()));
        } else {
            lblMaxPrice.setText("Không giới hạn");
        }

        // --- BỔ SUNG MỚI: Đổ lịch sử cũ vào ListView nếu có ---
        if (listHistory != null) {
            listHistory.getItems().clear();
            if (product.getBidHistory() != null) {
                listHistory.getItems().addAll(product.getBidHistory());
            }
        }

        loadProductImage(product.getImageUrl());
        setupChart();

        if (timeline != null) timeline.stop();
        startCountdown();

        sendSubscribe();
        registerSocketListener();
    }

    // ===== LOAD ẢNH =====
    private void loadProductImage(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return;
        try {
            String directUrl = convertDriveUrl(rawUrl);
            Image image = new Image(directUrl, true);
            productImageView.setImage(image);
        } catch (Exception e) {
            System.err.println("Không load được ảnh: " + e.getMessage());
        }
    }

    private String convertDriveUrl(String rawUrl) {
        if (rawUrl != null && rawUrl.contains("drive.google.com/file/d/")) {
            String id = rawUrl.replaceAll(".*drive\\.google\\.com/file/d/([^/]+).*", "$1");
            return "https://drive.google.com/uc?export=download&id=" + id;
        }
        return rawUrl;
    }

    // ===== SETUP CHART =====
    private void setupChart() {
        if (priceSeries == null) {
            priceSeries = new XYChart.Series<>();
            priceSeries.setName("Diễn biến giá");
            priceChart.getData().add(priceSeries);
            String initLabel = LocalTime.now().format(TIME_FMT);
            priceSeries.getData().add(new XYChart.Data<>(initLabel, product.getStartPrice()));
        }
    }

    public void updateChart(String timeLabel, double newPrice) {
        Platform.runLater(() -> {
            priceSeries.getData().add(new XYChart.Data<>(timeLabel, newPrice));
            if (priceSeries.getData().size() > 10) {
                priceSeries.getData().remove(0);
            }
        });
    }

    // ===== LẮNG NGHE SOCKET (ĐÃ THÊM LOGIC GHI LỊCH SỬ KHI CÓ NGƯỜI KHÁC BID) =====
    private void registerSocketListener() {
        NetworkClient.getInstance().setPushListener((type, json) -> {
            if ("NEW_BID".equals(type)) {
                try {
                    JsonObject data = json.getAsJsonObject("data");
                    String auctionId = data.get("id").getAsString();

                    if (!auctionId.equals(product.getId())) return;

                    double newPrice = data.get("currentPrice").getAsDouble();
                    String leader   = data.get("currentLeader").getAsString();
                    String timeLabel = LocalTime.now().format(TIME_FMT);

                    Platform.runLater(() -> {
                        product.setCurrentPrice(newPrice);
                        lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
                        updateChart(timeLabel, newPrice);

                        // === BỔ SUNG MỚI: Thêm dòng lịch sử từ người khác vào bảng ===
                        String logEntry = String.format("[%s] Người dùng %s đã trả giá: %,.0f VNĐ", timeLabel, leader, newPrice);
                        product.addBidHistory(logEntry);
                        if (listHistory != null) {
                            listHistory.getItems().add(0, logEntry); // Thêm lên đầu bảng
                        }

                        System.out.println("✅ Giá mới từ " + leader + ": " + newPrice);
                    });
                } catch (Exception e) {
                    System.err.println("Lỗi xử lý NEW_BID: " + e.getMessage());
                }
            }
        });
    }

    // ===== COUNTDOWN (GIỮ NGUYÊN) =====
    private void startCountdown() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (product.getEndTime() == null) return;

            java.time.Duration diff = java.time.Duration.between(LocalDateTime.now(), product.getEndTime());

            if (diff.isNegative() || diff.isZero()) {
                lblTimer.setText("KẾT THÚC");
                btnBid.setDisable(true);
                timeline.stop();
                AlertUtil.showSuccess("Kết thúc", "Phiên đấu giá đã kết thúc!");
            } else {
                long hours  = diff.toHours();
                int minutes = diff.toMinutesPart();
                int seconds = diff.toSecondsPart();
                lblTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // ===== XỬ LÝ ĐẶT GIÁ (ĐÃ THÊM LOGIC GHI LỊCH SỬ KHI CHÍNH BẠN ĐẶT GIÁ) =====
    @FXML
    private void handleBid() {
        try {
            String input = txtBidAmount.getText().trim();
            if (input.isEmpty()) return;

            double increment = Double.parseDouble(input);
            if (increment <= 0) {
                AlertUtil.showError("Lỗi", "Số tiền tăng thêm phải lớn hơn 0!");
                return;
            }

            double newPrice = product.getCurrentPrice() + increment;
            String bidderId = SessionManager.getUsername();

            new Thread(() -> {
                try {
                    BidMessage bidMsg = new BidMessage("BID", product.getId(), bidderId, newPrice);
                    NetworkClient.getInstance().sendBidMessage(bidMsg);

                    String response = NetworkClient.getInstance().readResponse();
                    System.out.println("Server phản hồi BID: " + response);

                    Platform.runLater(() -> {
                        if (response != null && response.contains("\"status\":\"OK\"")) {
                            product.setCurrentPrice(newPrice);
                            lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));

                            String timeLabel = LocalTime.now().format(TIME_FMT);
                            updateChart(timeLabel, newPrice);

                            // === BỔ SUNG MỚI: Thêm lượt đấu giá của chính mình vào bảng ===
                            String logEntry = String.format("[%s] Bạn (%s) đã nâng giá lên: %,.0f VNĐ", timeLabel, bidderId, newPrice);
                            product.addBidHistory(logEntry);
                            if (listHistory != null) {
                                listHistory.getItems().add(0, logEntry);
                            }

                            txtBidAmount.clear();
                        } else {
                            AlertUtil.showError("Lỗi", "Đặt giá thất bại: " + response);
                        }
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> AlertUtil.showError("Lỗi hệ thống", "Không thể gửi lượt đấu giá!"));
                }
            }).start();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Vui lòng nhập con số hợp lệ!");
        }
    }

    // ===== ĐIỀU HƯỚNG =====
    @FXML
    private void handleBackToList() {
        if (timeline != null) timeline.stop();
        sendUnsubscribe();
        NetworkClient.getInstance().clearPushListener();
        SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
    }

    @FXML
    private void handleBackToDetail() {
        if (timeline != null) timeline.stop();
        sendUnsubscribe();
        NetworkClient.getInstance().clearPushListener();
        ProductDetailController controller = SceneManager.switchSceneAndGetController("/view/ProductDetail.fxml", "Chi tiết sản phẩm");
        if (controller != null) controller.setProductData(this.product);
    }

    // ===== SUBSCRIBE / UNSUBSCRIBE OBSERVER =====
    private void sendSubscribe() {
        new Thread(() -> {
            try {
                String json = String.format("{\"action\":\"SUBSCRIBE_AUCTION\",\"roomId\":\"%s\",\"username\":\"%s\"}",
                        product.getId(), SessionManager.getUsername());
                NetworkClient.getInstance().sendRaw(json);
                System.out.println("📡 SUBSCRIBE gửi cho phòng: " + product.getId());
            } catch (Exception e) {
                System.err.println("Lỗi SUBSCRIBE: " + e.getMessage());
            }
        }).start();
    }

    private void sendUnsubscribe() {
        new Thread(() -> {
            try {
                String json = String.format("{\"action\":\"UNSUBSCRIBE_AUCTION\",\"roomId\":\"%s\",\"username\":\"%s\"}",
                        product.getId(), SessionManager.getUsername());
                NetworkClient.getInstance().sendRaw(json);
                System.out.println("🔕 UNSUBSCRIBE khỏi phòng: " + product.getId());
            } catch (Exception e) {
                System.err.println("Lỗi UNSUBSCRIBE: " + e.getMessage());
            }
        }).start();
    }
}