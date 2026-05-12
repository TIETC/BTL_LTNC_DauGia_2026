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
        lblStartPrice.setText(product.getStartPrice() + " VNĐ");
        lblCurrentPrice.setText(product.getCurrentPrice() + " VNĐ");
        lblDescription.setText(product.getDescription());
        startCountdown();
    }

    private void startCountdown() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            java.time.Duration diff = java.time.Duration.between(LocalDateTime.now(), product.getEndTime());
            if (diff.isNegative()) {
                lblTimer.setText("ĐÃ KẾT THÚC");
            } else {
                long h = diff.toHours();
                long m = diff.toMinutesPart();
                long s = diff.toSecondsPart();
                lblTimer.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handleBid() {
        try {
            long current = Long.parseLong(product.getCurrentPrice());
            long bid = Long.parseLong(txtBidAmount.getText());
            long total = current + bid;
            product.setCurrentPrice(String.valueOf(total));
            lblCurrentPrice.setText(total + " VNĐ");
            txtBidAmount.clear();
        } catch (Exception e) {
            txtBidAmount.setText("Lỗi số!");
        }
    }
}