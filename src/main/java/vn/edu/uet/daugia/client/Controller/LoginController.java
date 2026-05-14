package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.shared.model.LoginMessage;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import com.google.gson.Gson;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    protected void onLoginButtonClick(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu!");
            return;
        }

        try {
            // Gửi yêu cầu LOGIN lên Server
            LoginMessage loginMsg = new LoginMessage("LOGIN", username, password);
            Gson gson = new Gson();
            String json = gson.toJson(loginMsg);

            NetworkClient.getInstance().sendRaw(json);
            System.out.println("Đã gửi LOGIN: " + json);

            // Đọc phản hồi từ Server
            String response = NetworkClient.getInstance().readResponse();
            System.out.println("Server phản hồi: " + response);

            if ("LOGIN_SUCCESS".equals(response)) {
                SceneManager.switchScene("/view/AuctionList.fxml", "Trang Đấu Giá");
            } else {
                showError("Sai tài khoản hoặc mật khẩu!");
            }

        } catch (Exception e) {
            showError("Không thể kết nối tới Server!");
            e.printStackTrace();
        }
    }

    @FXML
    protected void switchToRegister(ActionEvent event) {
        SceneManager.switchScene("/view/Register.fxml", "Đăng ký tài khoản");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}