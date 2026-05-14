module vn.edu.uet.daugia {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.sql;
    // Cho phép JavaFX truy cập vào package login
    opens com.example.login to javafx.fxml;
    exports com.example.login;
    opens vn.edu.uet.daugia.shared.model to com.google.gson;
    // Cần thêm dòng này để chạy được List
    opens com.example.list to javafx.fxml;
    exports com.example.list;
}