module vn.edu.uet.daugia {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    // Fix lỗi cho phần DatabaseConnection bên phía Server
    requires java.sql;

    // --- BỔ SUNG: Cấp quyền cho Gson để có thể dịch class BidMessage sang JSON ---
    exports vn.edu.uet.daugia.shared.model;
    opens vn.edu.uet.daugia.shared.model to com.google.gson;

    // 1. Cấp quyền cho package gốc của Client
    exports vn.edu.uet.daugia.client;
    opens vn.edu.uet.daugia.client to javafx.graphics, javafx.fxml;

    // 2. Cấp quyền cho Controller
    exports vn.edu.uet.daugia.client.Controller;
    opens vn.edu.uet.daugia.client.Controller to javafx.fxml;

    // 3. Cấp quyền cho Model để TableView đọc được dữ liệu
    exports vn.edu.uet.daugia.client.model;
    opens vn.edu.uet.daugia.client.model to javafx.base;

    // 4. Cấp quyền cho các package tiện ích
    exports vn.edu.uet.daugia.client.network;
    exports vn.edu.uet.daugia.client.util;
}