package vn.edu.uet.daugia.client.Controller;

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

import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.shared.model.RegisterMessage;
import com.google.gson.Gson;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    protected void switchToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/Login.fxml")
            );
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onRegisterButtonClick(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        new Thread(() -> {
            try {
                RegisterMessage register = new RegisterMessage("REGISTER", username, password);
                Gson gson = new Gson();
                NetworkClient.getInstance().sendRaw(gson.toJson(register));

                String response = NetworkClient.getInstance().readResponse();
                System.out.println("Server phản hồi REGISTER: " + response);

                javafx.application.Platform.runLater(() -> {
                    if ("REGISTER_SUCCESS".equals(response)) {
                        showSuccess("Đăng ký thành công! Mời đăng nhập.");
                        switchToLogin(event);
                    } else if ("REGISTER_FAILED:USERNAME_EXISTS".equals(response)) {
                        showError("Tên đăng nhập đã tồn tại!");
                    } else {
                        showError("Đăng ký thất bại, vui lòng thử lại!");
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        showError("Không thể kết nối tới Server!")
                );
                e.printStackTrace();
            }
        }).start();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}