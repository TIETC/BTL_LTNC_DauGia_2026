package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager; // Bổ sung import
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

        // ##########################################################################
        // ###  PHẦN TÀI KHOẢN MẪU - GIỮ NGUYÊN VÀ BỔ SUNG LƯU ROLE (SESSION)     ###
        // ##########################################################################
        if (username.equals("admin01") && password.equals("admin123")) {
            SessionManager.setUser(username, "ADMIN");
            SceneManager.switchScene("/view/AuctionList.fxml", "Admin - Quản trị hệ thống");
            return;
        }
        else if (username.equals("seller01") && password.equals("seller123")) {
            SessionManager.setUser(username, "SELLER");
            SceneManager.switchScene("/view/SellerDashboard.fxml", "Kênh người bán");
            return;
        }
        else if (username.equals("bidder01") && password.equals("bidder123")) {
            SessionManager.setUser(username, "BIDDER");
            SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách đấu giá");
            return;
        }

        // ##########################################################################
        // ###  LOGIC GỬI SERVER NGUYÊN BẢN CỦA BẠN - ĐÃ BỔ SUNG LƯU SESSION      ###
        // ##########################################################################
        try {
            LoginMessage loginMsg = new LoginMessage("LOGIN", username, password);
            Gson gson = new Gson();
            String json = gson.toJson(loginMsg);

            NetworkClient.getInstance().sendRaw(json);
            System.out.println("Đã gửi LOGIN lên máy chủ: " + json);

            String response = NetworkClient.getInstance().readResponse();
            System.out.println("Máy chủ phản hồi: " + response);

            if ("LOGIN_SUCCESS".equals(response)) {
                // Giả lập lưu quyền BIDDER khi đăng nhập qua Server thành công
                SessionManager.setUser(username, "BIDDER");
                SceneManager.switchScene("/view/AuctionList.fxml", "Hệ thống Đấu giá");
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