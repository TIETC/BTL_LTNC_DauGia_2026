package com.example.login;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML private TextField regUsername;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField confirmPassword;

    // CỰC KỲ QUAN TRỌNG: Phải có chữ 'static' để dữ liệu không bị mất khi chuyển trang
    private static final Map<String, String> userDatabase = new HashMap<>();

    static {
        // Tài khoản mặc định
        userDatabase.put("admin", "123");
    }

    // Hàm Đăng ký
    @FXML
    protected void handleRegisterSubmit(ActionEvent event) {
        String user = regUsername.getText();
        String pass = regPassword.getText();
        String confirm = confirmPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Lỗi", "Vui lòng điền đầy đủ thông tin!");
        } else if (userDatabase.containsKey(user)) {
            showAlert("Lỗi", "Tên đăng nhập này đã tồn tại!");
        } else if (!pass.equals(confirm)) {
            showAlert("Lỗi", "Mật khẩu xác nhận không khớp!");
        } else {
            // Lưu vào kho dữ liệu chung (static)
            userDatabase.put(user, pass);
            showAlert("Thành công", "Đăng ký thành công tài khoản: " + user);

            // LỆNH QUAY VỀ TRANG ĐĂNG NHẬP TỰ ĐỘNG
            try {
                switchToLogin(event);
            } catch (IOException e) {
                System.out.println("Lỗi chuyển trang: " + e.getMessage());
            }
        }
    }

    // Hàm Đăng nhập
    @FXML
    protected void onLoginButtonClick() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // Kiểm tra trong kho dữ liệu chung
        if (userDatabase.containsKey(username) && userDatabase.get(username).equals(password)) {
            showAlert("Thông báo", "Đăng nhập thành công! Chào mừng " + username);
        } else {
            showAlert("Lỗi", "Sai tài khoản hoặc mật khẩu!");
        }
    }

    // Hàm chuyển sang trang Đăng ký
    @FXML
    public void switchToRegister(ActionEvent event) throws IOException {
        changeScene(event, "register-view.fxml");
    }

    // Hàm chuyển sang trang Đăng nhập
    @FXML
    public void switchToLogin(ActionEvent event) throws IOException {
        changeScene(event, "login-view.fxml");
    }

    // Hàm phụ để tối ưu việc chuyển trang, tránh viết lặp code
    private void changeScene(ActionEvent event, String fxmlFile) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlFile));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}