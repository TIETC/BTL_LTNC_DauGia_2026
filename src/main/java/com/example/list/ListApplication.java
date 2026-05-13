package com.example.list;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ListApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Nạp file giao diện danh sách bạn đã tạo
        FXMLLoader fxmlLoader = new FXMLLoader(ListApplication.class.getResource("auction-view.fxml"));

        // Tạo cửa sổ với kích thước 600x500
        Scene scene = new Scene(fxmlLoader.load(), 600, 500);

        stage.setTitle("Hệ thống Đấu giá - Danh sách sản phẩm");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}