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
import javafx.util.Duration;

import com.google.gson.JsonObject;

import java.time.LocalDateTime;

public class BiddingRoomController {

    @FXML private Label lblName, lblId, lblSession, lblDescription, lblCurrentPrice, lblTimer;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private LineChart<Number, Number> priceChart;

    private XYChart.Series<Number, Number> priceSeries;
    private int bidCount = 0;
    private Product product;
    private Timeline timeline;

    // Dùng khi AuctionList truyền Product sang (click đúp)
    public void setAuctionData(Product product) {
        this.product = product;
        setupUI();
    }

    // Giữ lại tên cũ để không lỗi nếu chỗ khác đang gọi
    public void setProductData(Product product) {
        this.product = product;
        setupUI();
    }

    private void setupUI() {
        if (product == null) return;

        lblName.setText(product.getName());
        lblId.setText("Mã SP: " + product.getId());
        lblSession.setText("Phiên: " + product.getSession());
        lblDescription.setText("Mô tả: " + product.getDescription());
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));

        setupChart();

        if (timeline != null) timeline.stop();
        startCountdown();

        // Đăng ký nhận push NEW_BID từ Server
        // Khi Bidder khác đặt giá → giá tự cập nhật trên màn hình mình
        NetworkClient.getInstance().setPushListener((type, json) -> {
            if ("NEW_BID".equals(type)) {
                try {
                    JsonObject data = json.getAsJsonObject("data");
                    String auctionId = data.get("id").getAsString();

                    // Chỉ cập nhật nếu là auction đang xem
                    if (!auctionId.equals(product.getId())) return;

                    double newPrice = data.get("currentPrice").getAsDouble();
                    String leader   = data.get("currentLeader").getAsString();

                    Platform.runLater(() -> {
                        product.setCurrentPrice(newPrice);
                        lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
                        priceSeries.getData().add(new XYChart.Data<>(bidCount++, newPrice));
                        System.out.println("✅ Giá mới từ " + leader + ": " + newPrice);
                    });
                } catch (Exception e) {
                    System.err.println("Lỗi xử lý NEW_BID: " + e.getMessage());
                }
            }
        });
    }

    private void setupChart() {
        if (priceSeries == null) {
            priceSeries = new XYChart.Series<>();
            priceSeries.setName("Diễn biến giá");
            priceChart.getData().add(priceSeries);
            priceSeries.getData().add(new XYChart.Data<>(bidCount++, product.getStartPrice()));
        }
    }

    private void startCountdown() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (product.getEndTime() == null) return;

            java.time.Duration diff = java.time.Duration.between(
                    LocalDateTime.now(), product.getEndTime());

            if (diff.isNegative() || diff.isZero()) {
                lblTimer.setText("KẾT THÚC");
                btnBid.setDisable(true);
                timeline.stop();
                AlertUtil.showSuccess("Kết thúc", "Phiên đấu giá đã kết thúc!");
            } else {
                long hours   = diff.toHours();
                int minutes  = diff.toMinutesPart();
                int seconds  = diff.toSecondsPart();
                lblTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

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
            String bidderId = SessionManager.getUsername(); // dùng username đang đăng nhập

            // Gửi BID lên Server trong thread riêng
            new Thread(() -> {
                try {
                    BidMessage bidMsg = new BidMessage("BID", product.getId(), bidderId, newPrice);
                    NetworkClient.getInstance().sendBidMessage(bidMsg);

                    // Đọc phản hồi từ Server
                    String response = NetworkClient.getInstance().readResponse();
                    System.out.println("Server phản hồi BID: " + response);

                    Platform.runLater(() -> {
                        if (response != null && response.contains("\"status\":\"OK\"")) {
                            // Cập nhật UI sau khi Server xác nhận
                            product.setCurrentPrice(newPrice);
                            lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
                            priceSeries.getData().add(new XYChart.Data<>(bidCount++, newPrice));
                            txtBidAmount.clear();
                        } else {
                            AlertUtil.showError("Lỗi", "Đặt giá thất bại: " + response);
                        }
                    });

                } catch (Exception e) {
                    Platform.runLater(() ->
                            AlertUtil.showError("Lỗi hệ thống", "Không thể gửi lượt đấu giá!"));
                }
            }).start();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Vui lòng nhập con số hợp lệ!");
        }
    }

    @FXML
    private void handleBackToList() {
        if (timeline != null) timeline.stop();
        NetworkClient.getInstance().clearPushListener();
        SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
    }

    @FXML
    private void handleBackToDetail() {
        if (timeline != null) timeline.stop();
        NetworkClient.getInstance().clearPushListener();
        ProductDetailController controller = SceneManager.switchSceneAndGetController(
                "/view/ProductDetail.fxml", "Chi tiết sản phẩm");
        if (controller != null) controller.setProductData(this.product);
    }
}