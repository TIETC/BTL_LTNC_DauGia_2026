package com.example.list;

import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;

public class MainDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button btnAuctionList, btnMyItems, btnProfile;
    @FXML private Label lblUsername, lblClock;

    private String currentUserRole = "BIDDER";
    private String currentUsername = "";

    @FXML
    public void initialize() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss  -  dd/MM/yyyy");
            lblClock.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public void setUsername(String username) {
        this.currentUsername = username;
        lblUsername.setText(username);
    }

    public void setRole(String role) {
        this.currentUserRole = role;
        if (role.equals("SELLER")) {
            btnMyItems.setVisible(true);
            btnMyItems.setManaged(true);
        }
        showAuctionList(false);
    }

    public void showAuctionList(boolean onlyShowMine) {
        setActiveButton(btnAuctionList);
        loadViewWithData("/com/example/list/auction-grid-view.fxml", onlyShowMine, null);
    }

    @FXML private void handleMenuAuctionList() { showAuctionList(false); }

    @FXML private void handleMenuMyItems() {
        setActiveButton(btnMyItems);
        loadViewWithData("/com/example/list/auction-grid-view.fxml", true, null);
    }

    public void showProductDetail(JsonObject productData) {
        loadViewWithData("/com/example/list/product-detail-view.fxml", false, productData);
    }

    public void showPostItemPage() {
        loadViewWithData("/com/example/list/seller-post-item.fxml", false, null);
    }

    @FXML private void showProfile() { setActiveButton(btnProfile); }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/login/login-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Hệ Thống Đăng Nhập");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadViewWithData(String fxmlPath, boolean onlyShowMine, JsonObject productDataForDetail) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            Object controller = loader.getController();

            if (controller instanceof AuctionGridController) {
                AuctionGridController gridCtrl = (AuctionGridController) controller;
                gridCtrl.setDashboardInfo(this, currentUserRole, currentUsername, onlyShowMine);
            }
            if (controller instanceof SellerController) {
                SellerController sellerCtrl = (SellerController) controller;
                sellerCtrl.setDashboardController(this, currentUsername);
            }
            if (controller instanceof ProductDetailController) {
                ProductDetailController detailCtrl = (ProductDetailController) controller;

                // [NÂNG CẤP TẠI ĐÂY]: Truyền thêm Role sang để Phòng Đấu Giá biết đường khóa nút
                detailCtrl.setDashboardController(this, currentUsername, currentUserRole);
                detailCtrl.setProductDetails(productDataForDetail);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            System.err.println("Lỗi nạp file FXML: " + fxmlPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button activeBtn) {
        btnAuctionList.getStyleClass().remove("nav-btn-active");
        btnMyItems.getStyleClass().remove("nav-btn-active");
        btnProfile.getStyleClass().remove("nav-btn-active");
        activeBtn.getStyleClass().add("nav-btn-active");
    }
}