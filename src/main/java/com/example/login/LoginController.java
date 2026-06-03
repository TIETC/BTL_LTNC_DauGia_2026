package com.example.login;

import com.example.list.MainDashboardController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import vn.edu.uet.daugia.client.NetworkClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.scene.input.MouseEvent;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    // Khởi tạo trạm phát tín hiệu mạng và bộ dịch JSON
    private NetworkClient networkClient = new NetworkClient();
    private Gson gson = new Gson();

    @FXML
    protected void onLoginButtonClick(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // 1. Kiểm tra không cho phép nhập rỗng
        if(username.isEmpty() || password.isEmpty()){
            showError("Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }
        // 2. Mở đường truyền tới Server bằng Cấu hình động
        String targetIP = vn.edu.uet.daugia.shared.ServerConfig.SERVER_IP;
        int targetPort = vn.edu.uet.daugia.shared.ServerConfig.SERVER_PORT;

        if (!networkClient.connect()) {
            showError("Không thể kết nối đến Máy chủ Đấu giá tại IP: " + targetIP);
            return;
        }
        // 3. Đóng gói dữ liệu thành JSON theo chuẩn giao thức đã thống nhất
        String jsonRequest = String.format(
                "{\"type\":\"LOGIN\", \"username\":\"%s\", \"password\":\"%s\"}",
                username, password
        );

        // 4. Bắn sang Server và nín thở chờ kết quả...
        String response = networkClient.sendRequest(jsonRequest);
        System.out.println("[Client] Server trả về: " + response);

        if (response == null || response.isEmpty()) {
            showError("Không nhận được phản hồi từ Server!");
            return;
        }

        // 5. Bóc tách JSON do Server trả về để rẽ nhánh giao diện
        JsonObject resObj = gson.fromJson(response, JsonObject.class);
        String status = resObj.get("status").getAsString();

        if (status.equals("SUCCESS")) {
            // Lấy ra quyền hạn (BIDDER hay SELLER) để sau này hiển thị màn hình cho chuẩn
            String role = resObj.get("role").getAsString();

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/list/main-dashboard.fxml"));
                Parent root = loader.load();

                // === BẮT ĐẦU ĐOẠN NỐI DÂY DỮ LIỆU ===
                // Lấy quyền điều khiển của trang Dashboard vừa load
                MainDashboardController dashboardController = loader.getController();
                // Truyền tên tài khoản mà người dùng vừa gõ vào
                dashboardController.setUsername(username);
                dashboardController.setRole(role); // Truyền quyền (BIDDER/SELLER) sang Dashboard

                Stage stage = (Stage) txtUsername.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Trang Chủ - Hệ Thống Đấu Giá");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            // Đăng nhập sai MySQL (Sai pass, tài khoản không tồn tại...)
            String message = resObj.has("message") ? resObj.get("message").getAsString() : "Đăng nhập thất bại";
            showError(message);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Thông báo hệ thống");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    protected void switchToRegister() { // Xóa bỏ hoàn toàn chữ 'event' trong ngoặc
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/login/register-view.fxml"));
            Parent root = loader.load();

            // Lấy cửa sổ hiện tại thông qua ô nhập liệu txtUsername
            Stage stage = (Stage) txtUsername.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}