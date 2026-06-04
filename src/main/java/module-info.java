module vn.edu.uet.daugia {
    // Các thư viện lõi hệ thống cần sử dụng
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.sql;

    // 1. Khối UI - Login: Cấp quyền cho JavaFX vẽ giao diện Đăng nhập
    opens vn.edu.uet.daugia.client.ui.login to javafx.fxml;
    exports vn.edu.uet.daugia.client.ui.login;

    // 2. Khối UI - List: Cấp quyền cho JavaFX vẽ giao diện Dashboard & Chi tiết
    opens vn.edu.uet.daugia.client.ui.list to javafx.fxml;
    exports vn.edu.uet.daugia.client.ui.list;

    // 3. Khối OOP - Shared: Cấp quyền cho GSON đóng gói/mở gói JSON thành các Đối tượng Java
    opens vn.edu.uet.daugia.shared.model to com.google.gson;
    exports vn.edu.uet.daugia.shared.model;

    opens vn.edu.uet.daugia.shared.model.user to com.google.gson;
    exports vn.edu.uet.daugia.shared.model.user;

    opens vn.edu.uet.daugia.shared.model.item to com.google.gson;
    exports vn.edu.uet.daugia.shared.model.item;
}