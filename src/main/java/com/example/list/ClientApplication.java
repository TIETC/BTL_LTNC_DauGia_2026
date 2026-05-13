package com.example.list; // Giữ nguyên package cha

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Đường dẫn tuyệt đối để tìm thấy file login dù nó ở package khác
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/login/login-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Hệ thống Đấu giá trực tuyến - Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}