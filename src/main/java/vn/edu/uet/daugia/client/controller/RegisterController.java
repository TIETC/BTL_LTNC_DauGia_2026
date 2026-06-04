package vn.edu.uet.daugia.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox; // Bổ sung
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.shared.model.RegisterMessage;
import com.google.gson.Gson;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    // BỔ SUNG: Khớp với fx:id="roleChoiceBox" trong FXML
    @FXML private ChoiceBox<String> roleChoiceBox;

    @FXML
    public void initialize() {
        // Đặt giá trị mặc định khi vừa mở trang
        if (roleChoiceBox != null) {
            roleChoiceBox.setValue("BIDDER");
        }
    }

    @FXML
    protected void switchToLogin(ActionEvent event) {
        // Giữ nguyên logic FXMLLoader của bạn
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
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

        // Lấy giá trị Role từ ChoiceBox
        String role = (roleChoiceBox != null) ? roleChoiceBox.getValue() : "BIDDER";

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // ##########################################################################
        // ###  PHẦN GIẢ LẬP ĐĂNG KÝ MẪU (TEST NHANH - CÓ THỂ XÓA KHI NỐI SERVER)  ###
        // ##########################################################################
        if (username.startsWith("test")) {
            showSuccess("Đăng ký giả lập thành công cho: " + username + " (" + role + ")");
            switchToLogin(event);
            return;
        }
        // ##########################################################################

        new Thread(() -> {
            try {
                // Giữ nguyên logic RegisterMessage của bạn (Bổ sung role vào nếu class RegisterMessage có hỗ trợ)
                // Nếu class RegisterMessage chưa có trường Role, bạn hãy bảo người làm shared model thêm vào nhé
                RegisterMessage register = new RegisterMessage("REGISTER", username, password, role);

                Gson gson = new Gson();
                // Gửi thêm role kèm theo nếu cần thiết (ví dụ dùng JSON tùy chỉnh)
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
