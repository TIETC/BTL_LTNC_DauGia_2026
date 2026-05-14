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
    @FXML private Label lblName, lblCurrentPrice, lblTimer;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid; // Nút đặt giá để khóa khi hết giờ

    @FXML private LineChart<Number, Number> priceChart;
    private XYChart.Series<Number, Number> priceSeries;
    private int bidCount = 0;
    private Product product;
    private Timeline timeline;

    public void setProductData(Product product) {
        this.product = product;
        lblName.setText(product.getName());
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));

        setupChart();
        startCountdown();
    }

    private void setupChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Lịch sử giá");
        priceChart.getData().add(priceSeries);
        priceSeries.getData().add(new XYChart.Data<>(bidCount++, product.getStartPrice()));
    }

    private void startCountdown() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            java.time.Duration diff = java.time.Duration.between(LocalDateTime.now(), product.getEndTime());

            if (diff.isNegative() || diff.isZero()) {
                lblTimer.setText("PHIÊN ĐÃ KẾT THÚC");
                lblTimer.setStyle("-fx-text-fill: gray;");
                btnBid.setDisable(true); // Khóa đặt giá theo yêu cầu 3.1.4
                txtBidAmount.setDisable(true);
                timeline.stop();

                AlertUtil.showSuccess("Thông báo", "Phiên đấu giá cho " + product.getName() + " đã kết thúc!\n" +
                        "Giá cuối cùng: " + String.format("%,.0f VNĐ", product.getCurrentPrice()));
            } else {
                lblTimer.setText(String.format("%02d:%02d:%02d", diff.toHours(), diff.toMinutesPart(), diff.toSecondsPart()));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handleBid() {
        try {
            double increment = Double.parseDouble(txtBidAmount.getText());
            if (increment <= 0) {
                AlertUtil.showError("Lỗi", "Mức tăng phải lớn hơn 0!");
                return;
            }

            // Tính toán giá mới
            double newPrice = product.getCurrentPrice() + increment;

            // --- PHẦN MỚI THÊM: Gửi dữ liệu qua Socket lên Server ---
            // Khởi tạo gói tin BidMessage ("Trang" là tên người dùng đang test)
            vn.edu.uet.daugia.shared.model.BidMessage bidMsg =
                    new vn.edu.uet.daugia.shared.model.BidMessage("BID", product.getId(), "Trang", newPrice);

            // Gửi gói tin thông qua NetworkClient
            vn.edu.uet.daugia.client.network.NetworkClient.getInstance().sendBidMessage(bidMsg);
            // ---------------------------------------------------------

            // Cập nhật lại giao diện (UI) cục bộ
            product.setCurrentPrice(newPrice);
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
            priceSeries.getData().add(new XYChart.Data<>(bidCount++, newPrice));
            txtBidAmount.clear();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    private void handleBack() {
        if (timeline != null) timeline.stop();
        SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
    }
}