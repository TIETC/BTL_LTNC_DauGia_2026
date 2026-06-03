package com.example.list;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import vn.edu.uet.daugia.client.NetworkClient;

import java.io.File;

public class SellerController {

    @FXML private TextField txtItemId;
    @FXML private TextField txtItemName;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtBidIncrement;
    @FXML private TextField txtStartTime;
    @FXML private TextField txtEndTime;
    @FXML private TextArea txtDescription;
    @FXML private Label lblImagePath;

    private String selectedImagePath = "";
    private NetworkClient networkClient = new NetworkClient();
    private Gson gson = new Gson();

    private MainDashboardController dashboardController;
    private String currentSellerName = "Ẩn danh";

    public void setDashboardController(MainDashboardController controller, String username) {
        this.dashboardController = controller;
        this.currentSellerName = username;
    }

    @FXML
    public void initialize() {
        // Cảm biến tự động tính 10% bước giá
        txtStartPrice.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!newValue.isEmpty()) {
                    double price = Double.parseDouble(newValue);
                    txtBidIncrement.setText(String.format("%.0f", price * 0.1));
                } else {
                    txtBidIncrement.clear();
                }
            } catch (NumberFormatException e) {
                txtBidIncrement.clear();
            }
        });
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            lblImagePath.setText(file.getName());
            lblImagePath.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handlePostItem() {
        String id = txtItemId.getText().trim();
        String name = txtItemName.getText().trim();
        String priceStr = txtStartPrice.getText().trim();
        String startTime = txtStartTime.getText().trim();
        String endTime = txtEndTime.getText().trim();

        // Kiểm tra xem đã nhập đủ 6 thông tin chưa
        if (id.isEmpty() || name.isEmpty() || priceStr.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || selectedImagePath.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng điền đủ thông tin và chọn ảnh!");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);

            if (!networkClient.connect()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến Máy chủ.");
                return;
            }

            // [SỬA LỖI TẠI ĐÂY] Sử dụng JsonObject để đóng gói dữ liệu an toàn tuyệt đối
            JsonObject requestJson = new JsonObject();
            requestJson.addProperty("type", "ADD_ITEM");
            requestJson.addProperty("item_id", id);
            requestJson.addProperty("name", name);
            requestJson.addProperty("starting_price", price);
            requestJson.addProperty("start_time", startTime);
            requestJson.addProperty("end_time", endTime);
            requestJson.addProperty("image_path", selectedImagePath);
            requestJson.addProperty("seller_name", currentSellerName);
            requestJson.addProperty("description", txtDescription.getText().trim());

            String jsonRequest = requestJson.toString(); // Tự động chuyển thành chuỗi chuẩn JSON

            // Gửi đi và nhận kết quả
            String response = networkClient.sendRequest(jsonRequest);

            if (response != null && !response.isEmpty()) {
                JsonObject resObj = gson.fromJson(response, JsonObject.class);
                String status = resObj.get("status").getAsString();

                if (status.equals("SUCCESS")) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đẩy sản phẩm lên hệ thống!");

                    // Xóa sạch form
                    txtItemId.clear(); txtItemName.clear(); txtStartPrice.clear();
                    txtDescription.clear(); txtStartTime.clear(); txtEndTime.clear();
                    lblImagePath.setText("Chưa có ảnh nào được chọn");
                    lblImagePath.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
                    selectedImagePath = "";

                    // Tự động quay về trang Sàn Đấu Giá
                    handleExit();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Thất bại", "Lỗi từ Server: " + resObj.get("message").getAsString());
                }
            } else {
                // Thêm cảnh báo nếu Server im lặng ngắt kết nối
                showAlert(Alert.AlertType.ERROR, "Lỗi ngầm", "Server không phản hồi dữ liệu. Hãy kiểm tra tab Run của Server!");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá khởi điểm phải là một con số hợp lệ!");
        }
    }

    @FXML
    private void handleExit() {
        if (dashboardController != null) {
            // Lệnh gọi về trang lưới
            dashboardController.showAuctionList(false);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}