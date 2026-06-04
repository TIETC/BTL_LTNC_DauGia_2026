package vn.edu.uet.daugia.client.ui.login;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import vn.edu.uet.daugia.client.network.NetworkClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private RadioButton radioBuyer;
    @FXML private RadioButton radioSeller;

    private NetworkClient networkClient = new NetworkClient();
    private Gson gson = new Gson();

    @FXML
    protected void switchToLogin() { // Xóa bỏ hoàn toàn chữ 'event' trong ngoặc
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vn/edu/uet/daugia/client/ui/login/login-view.fxml"));
            Parent root = loader.load();

            // Lấy cửa sổ hiện tại thông qua ô nhập liệu usernameField
            Stage stage = (Stage) usernameField.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onRegisterButtonClick(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        // Mở kết nối sử dụng cấu hình chung
        if (!networkClient.connect()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến Máy chủ.");
            return;
        }

        // ĐỌC QUYỀN TỪ GIAO DIỆN: Kiểm tra xem người dùng đang tích vào nút nào
        String role = "BIDDER"; // Mặc định là người mua
        if (radioSeller != null && radioSeller.isSelected()) {
            role = "SELLER";    // Nếu tích vào nút người bán thì đổi thành SELLER
        }

        // Đóng gói JSON gửi đi (Nhét biến role vào thay vì gán cứng chữ "BIDDER" như cũ)
        String jsonRequest = String.format(
                "{\"type\":\"REGISTER\", \"username\":\"%s\", \"password\":\"%s\", \"role\":\"%s\"}",
                username, password, role
        );
        // Gửi và nhận phản hồi
        String response = networkClient.sendRequest(jsonRequest);

        if (response != null && !response.isEmpty()) {
            JsonObject resObj = gson.fromJson(response, JsonObject.class);
            String status = resObj.get("status").getAsString();

            if (status.equals("SUCCESS")) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
                switchToLogin(); // Chuyển về trang đăng nhập
            } else {
                String message = resObj.has("message") ? resObj.get("message").getAsString() : "Đăng ký thất bại.";
                showAlert(Alert.AlertType.ERROR, "Lỗi", message);
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không nhận được phản hồi từ Server.");
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