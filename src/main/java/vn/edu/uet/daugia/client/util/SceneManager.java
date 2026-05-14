package vn.edu.uet.daugia.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {
    private static Stage mainStage;

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    // Hàm chuyển màn hình thông thường (dùng cho Login, Register)
    public static void switchScene(String fxmlPath, String title) {
        switchSceneAndGetController(fxmlPath, title);
    }

    // Hàm MỚI: Vừa chuyển màn hình, vừa trả về Controller để truyền dữ liệu
    public static <T> T switchSceneAndGetController(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            mainStage.setTitle(title);
            mainStage.setScene(new Scene(root));
            return loader.getController();
        } catch (IOException e) {
            System.err.println("Lỗi: Không thể tải file giao diện " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }
}