package com.example.list;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import java.time.LocalDateTime;

public class ProductDetailController {
    @FXML private Label lblName, lblId, lblSession, lblStartPrice, lblDescription, lblCurrentPrice, lblTimer;
    @FXML private TextField txtBidAmount;
    private Product product;

    public void setProductData(Product product) {
        this.product = product;
        lblName.setText(product.getName());
        lblId.setText(product.getId());
        lblSession.setText(product.getSession());
        lblStartPrice.setText(String.format("%.0f VNĐ", product.getStartPrice()));
        lblCurrentPrice.setText(String.format("%.0f VNĐ", product.getCurrentPrice()));
        lblDescription.setText(product.getDescription());
        startCountdown();
    }

    private void startCountdown() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            java.time.Duration diff = java.time.Duration.between(LocalDateTime.now(), product.getEndTime());
            if (diff.isNegative()) lblTimer.setText("KẾT THÚC");
            else lblTimer.setText(String.format("%02d:%02d:%02d", diff.toHours(), diff.toMinutesPart(), diff.toSecondsPart()));
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handleBid() {
        try {
            double bid = Double.parseDouble(txtBidAmount.getText());
            product.setCurrentPrice(product.getCurrentPrice() + bid);
            lblCurrentPrice.setText(String.format("%.0f VNĐ", product.getCurrentPrice()));
            txtBidAmount.clear();
        } catch (Exception e) { txtBidAmount.setText("Lỗi số!"); }
    }
}