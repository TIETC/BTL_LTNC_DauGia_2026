package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.util.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    protected void onLoginButtonClick(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.equals("admin") && password.equals("123")) {
            // Chuyển sang màn hình danh sách đấu giá chỉ với 1 dòng code
            SceneManager.switchScene("/view/AuctionList.fxml", "Trang Đấu Giá");
        } else {
            showError("Sai tài khoản hoặc mật khẩu!");
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