package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.util.AlertUtil;
import vn.edu.uet.daugia.client.util.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import java.time.LocalDateTime;

public class BiddingRoomController {
    @FXML private Label lblName, lblId, lblSession, lblDescription, lblCurrentPrice, lblTimer;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private LineChart<Number, Number> priceChart;

    private XYChart.Series<Number, Number> priceSeries;
    private int bidCount = 0;
    private Product product;
    // Bỏ chữ static ở đây
    private Timeline timeline;

    public void setProductData(Product product) {
        this.product = product;

        // Đổ dữ liệu - Đảm bảo các fx:id này khớp với FXML
        if (product != null) {
            lblName.setText(product.getName());
            lblId.setText("Mã SP: " + product.getId());
            lblSession.setText("Phiên: " + product.getSession());
            lblDescription.setText("Mô tả: " + product.getDescription());
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));

            setupChart();

            // Luôn dừng timer cũ nếu có trước khi chạy timer mới cho UI mới
            if (timeline != null) {
                timeline.stop();
            }
            startCountdown();
        }
    }

    @FXML
    private void handleBackToDetail() {
        // Khi quay lại, bắt buộc phải dừng timer của TRANG NÀY
        // để nó không chạy lén dưới nền gây tốn RAM
        if (timeline != null) {
            timeline.stop();
        }

        ProductDetailController controller = SceneManager.switchSceneAndGetController(
                "/view/ProductDetail.fxml", "Chi tiết sản phẩm");
        if (controller != null) {
            controller.setProductData(this.product);
        }
    }
    @FXML
    private void handleBackToList() {
        // Về danh sách tổng thì nên dừng timer local
        if (timeline != null) timeline.stop();
        SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
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

            java.time.Duration diff = java.time.Duration.between(LocalDateTime.now(), product.getEndTime());

            if (diff.isNegative() || diff.isZero()) {
                lblTimer.setText("KẾT THÚC");
                btnBid.setDisable(true);
                timeline.stop();
                AlertUtil.showSuccess("Kết thúc", "Phiên đấu giá đã kết thúc!");
            } else {
                long hours = diff.toHours();
                int minutes = diff.toMinutesPart();
                int seconds = diff.toSecondsPart();
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

            // Gửi dữ liệu qua Network (Giữ nguyên logic của bạn)
            vn.edu.uet.daugia.shared.model.BidMessage bidMsg =
                    new vn.edu.uet.daugia.shared.model.BidMessage("BID", product.getId(), "Trang", newPrice);

            vn.edu.uet.daugia.client.network.NetworkClient.getInstance().sendBidMessage(bidMsg);

            // Cập nhật UI
            product.setCurrentPrice(newPrice);
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
            priceSeries.getData().add(new XYChart.Data<>(bidCount++, newPrice));
            txtBidAmount.clear();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Vui lòng nhập con số hợp lệ!");
        } catch (Exception e) {
            AlertUtil.showError("Lỗi hệ thống", "Không thể gửi lượt đấu giá!");
        }
    }
}