package vn.edu.uet.daugia.client;

import javafx.application.Application;
import javafx.stage.Stage;

import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.shared.model.RegisterMessage;
import com.google.gson.Gson;

public class AuctionClient extends Application {

    // =========================
    // CONFIG
    // =========================
    public static final String SERVER_IP = "10.11.6.187";
    public static final int SERVER_PORT = 5000;

    // =========================
    // ĐIỂM KHỞI ĐỘNG JAVAFX
    // =========================
    @Override
    public void start(Stage stage) {
        // Bước 1: Đăng ký Stage với SceneManager
        SceneManager.setMainStage(stage);

        // Bước 2: Kết nối tới Server
        NetworkClient.getInstance().connect(SERVER_IP, SERVER_PORT);

        // Bước 3: Mở màn hình Login
        SceneManager.switchScene("/view/Login.fxml", "Hệ thống đấu giá");

        stage.show();
    }

    // =========================
    // REGISTER — dùng kết nối chung
    // =========================
    public static void sendRegister(String username, String password) {
        try {
            RegisterMessage register = new RegisterMessage("REGISTER", username, password);
            Gson gson = new Gson();
            String json = gson.toJson(register);

            NetworkClient.getInstance().sendRaw(json);
            System.out.println("Đã gửi REGISTER: " + json);

            String response = NetworkClient.getInstance().readResponse();
            System.out.println("Server phản hồi: " + response);

        } catch (Exception e) {
            System.out.println("Lỗi REGISTER!");
            e.printStackTrace();
        }
    }

    // =========================
    // MAIN
    // =========================
    public static void main(String[] args) {
        launch(args); // gọi start() ở trên
    }
}