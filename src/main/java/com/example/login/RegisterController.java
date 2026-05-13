package com.example.login;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML
    protected void onRegisterButtonClick(ActionEvent event) {
        // Giả sử đăng ký thành công
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setContentText("Đăng ký tài khoản thành công!");
        alert.showAndWait();

        // CHUYỂN VỀ TRANG ĐĂNG NHẬP
        backToLogin(event);
    }

    @FXML
    protected void switchToLogin(ActionEvent event) {
        backToLogin(event);
    }

    private void backToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/login/login-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}