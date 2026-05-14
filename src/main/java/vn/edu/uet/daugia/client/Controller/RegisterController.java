package vn.edu.uet.daugia.client.Controller;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;

import javafx.stage.Stage;

import java.io.IOException;

import vn.edu.uet.daugia.client.AuctionClient;

public class RegisterController {

    // Kết nối với fx:id="usernameField"
    @FXML
    private TextField usernameField;

    // Kết nối với fx:id="passwordField"
    @FXML
    private PasswordField passwordField;

    // Chuyển sang màn hình login
    @FXML
    protected void switchToLogin(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/login/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập");

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/com/example/login/login-view.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.setScene(
                    new Scene(root)
            );

            stage.setTitle(
                    "Đăng nhập"
            );

            stage.show();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    // Xử lý nút đăng ký
    @FXML
    protected void onRegisterButtonClick(
            ActionEvent event
    ) {

        // Lấy dữ liệu từ ô nhập
        String username =
                usernameField.getText();

        String password =
                passwordField.getText();

        System.out.println(
                "Username: " + username
        );

        System.out.println(
                "Password: " + password
        );

        // Gửi dữ liệu tới server
        AuctionClient.sendRegister(
                username,
                password
        );

        System.out.println(
                "Đã gửi yêu cầu đăng ký!"
        );

        // Sau khi đăng ký xong
        // quay về login
        switchToLogin(event);
    }
}