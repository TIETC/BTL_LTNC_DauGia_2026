package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager;
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

        // =========================
        // TÀI KHOẢN MẪU CỨNG
        // Admin tạo thủ công, không đăng ký qua form
        // =========================
        if (username.equals("admin01") && password.equals("admin123")) {
            SessionManager.setUser(username, "ADMIN");
            SceneManager.switchScene("/view/AuctionList.fxml", "Admin - Quản trị hệ thống");
            return;
        }

        // =========================
        // ĐĂNG NHẬP QUA SERVER
        // Bidder và Seller đăng ký qua form → lưu DB → login ở đây
        // =========================
        new Thread(() -> {
            try {
                LoginMessage loginMsg = new LoginMessage("LOGIN", username, password);
                Gson gson = new Gson();
                String json = gson.toJson(loginMsg);

                NetworkClient.getInstance().sendRaw(json);
                System.out.println("Đã gửi LOGIN: " + json);

                // Server trả về "LOGIN_SUCCESS:BIDDER" hoặc "LOGIN_SUCCESS:SELLER"
                String response = NetworkClient.getInstance().readResponse();
                System.out.println("Server phản hồi: " + response);

                javafx.application.Platform.runLater(() -> {
                    if (response == null) {
                        showError("Không nhận được phản hồi từ Server!");

                    } else if (response.startsWith("LOGIN_SUCCESS")) {
                        // Tách role ra từ "LOGIN_SUCCESS:BIDDER"
                        String role = response.contains(":")
                                ? response.split(":")[1]
                                : "BIDDER"; // mặc định nếu DB chưa có cột role

                        // Lưu session
                        SessionManager.setUser(username, role);

                        // Chuyển màn hình đúng theo role
                        switch (role) {
                            case "SELLER":
                                SceneManager.switchScene("/view/SellerDashboard.fxml", "Kênh người bán");
                                break;
                            case "BIDDER":
                            default:
                                SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách đấu giá");
                                break;
                        }

                    } else if ("LOGIN_FAILED".equals(response)) {
                        showError("Sai tài khoản hoặc mật khẩu!");
                    } else {
                        showError("Đăng nhập thất bại!");
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