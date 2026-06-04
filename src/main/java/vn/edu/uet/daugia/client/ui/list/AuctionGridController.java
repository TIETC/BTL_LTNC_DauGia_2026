package vn.edu.uet.daugia.client.ui.list;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.scene.layout.*;
import vn.edu.uet.daugia.client.network.NetworkClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionGridController {
    @FXML private FlowPane productGrid;
    @FXML private HBox sellerActionBar;
    @FXML private Button btnAll, btnPending, btnRunning, btnFinished;

    private MainDashboardController dashboardController;
    private NetworkClient networkClient = new NetworkClient();
    private Gson gson = new Gson();

    private JsonArray allItemsRaw = new JsonArray();
    private String currentUsername = "";
    private boolean onlyShowMine = false;

    // Định dạng giờ giấc chuẩn quốc tế
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void setDashboardInfo(MainDashboardController controller, String role, String username, boolean onlyShowMine) {
        this.dashboardController = controller;
        this.currentUsername = username;
        this.onlyShowMine = onlyShowMine;

        if (role.equals("SELLER")) {
            sellerActionBar.setVisible(true);
            sellerActionBar.setManaged(true);
        } else {
            sellerActionBar.setVisible(false);
            sellerActionBar.setManaged(false);
        }

        fetchItemsFromServer();
    }

    private void fetchItemsFromServer() {
        if (!networkClient.connect()) return;
        String jsonRequest = "{\"type\":\"GET_ALL_ITEMS\"}";
        String response = networkClient.sendRequest(jsonRequest);
        if (response != null && !response.isEmpty()) {
            try {
                JsonObject resObj = gson.fromJson(response, JsonObject.class);
                if (resObj.get("status").getAsString().equals("SUCCESS")) {
                    allItemsRaw = resObj.getAsJsonArray("data");
                    renderFilteredGrid("ALL");
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // --- HÀM CỐT LÕI 1: TÍNH TOÁN TRẠNG THÁI THỜI GIAN THỰC (FIX LỖI TRƯỚC 12H) ---
    private String calculateRealTimeStatus(String startTimeStr, String endTimeStr) {
        try {
            // Lật ngược giờ sản phẩm từ chuỗi sang kiểu dữ liệu Thời gian
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DATE_TIME_FORMATTER);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DATE_TIME_FORMATTER);

            // Lấy giờ hiện tại của hệ thống (lấy đúng cái giờ đang nhảy trên lblClock)
            LocalDateTime now = LocalDateTime.now();

            // So sánh
            if (now.isBefore(startTime)) {
                // Trước giờ bắt đầu -> Chưa đấu giá
                return "PENDING";
            } else if (now.isAfter(endTime)) {
                // Sau giờ kết thúc -> Đã kết thúc
                return "FINISHED";
            } else {
                // Nằm giữa giờ bắt đầu và kết thúc -> Đang diễn ra
                return "RUNNING";
            }
        } catch (Exception e) {
            // Nếu giờ giấc bị lỗi thì mặc định là đang diễn ra để chữa cháy
            return "RUNNING";
        }
    }

    private void renderFilteredGrid(String timeFilter) {
        productGrid.getChildren().clear();
        for (JsonElement element : allItemsRaw) {
            JsonObject item = element.getAsJsonObject();

            if (onlyShowMine) {
                String sellerOfItem = item.has("seller_name") ? item.get("seller_name").getAsString() : "";
                if (!sellerOfItem.equalsIgnoreCase(currentUsername)) continue;
            }

            // Bóc tách giờ giấc từ JSON
            String startTimeStr = item.has("start_time") ? item.get("start_time").getAsString() : "2026-06-10 08:00:00";
            String endTimeStr = item.has("end_time") ? item.get("end_time").getAsString() : "2026-06-10 12:00:00";

            // [FIX STATUS] TÍNH TOÁN LẠI STATUS THỰC TẾ
            String realStatus = calculateRealTimeStatus(startTimeStr, endTimeStr);

            if (timeFilter.equals("PENDING") && !realStatus.equals("PENDING")) continue;
            if (timeFilter.equals("RUNNING") && !realStatus.equals("RUNNING")) continue;
            if (timeFilter.equals("FINISHED") && !realStatus.equals("FINISHED")) continue;

            // [FIX chi tiết button] Nhét nguyên cả gói JSON "item" vào thẻ CARD để sau này gọi details button cho dễ
            VBox card = createProductCard(item, realStatus);
            productGrid.getChildren().add(card);
        }
    }

    @FXML private void handleFilterAll() { changeActiveFilterStyle(btnAll); renderFilteredGrid("ALL"); }
    @FXML private void handleFilterPending() { changeActiveFilterStyle(btnPending); renderFilteredGrid("PENDING"); }
    @FXML private void handleFilterRunning() { changeActiveFilterStyle(btnRunning); renderFilteredGrid("RUNNING"); }
    @FXML private void handleFilterFinished() { changeActiveFilterStyle(btnFinished); renderFilteredGrid("FINISHED"); }

    private void changeActiveFilterStyle(Button targetBtn) {
        btnAll.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555;");
        btnPending.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555;");
        btnRunning.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555;");
        btnFinished.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555;");
        targetBtn.setStyle("-fx-background-color: #c8a97e; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    @FXML private void goToPostPage() { if (dashboardController != null) dashboardController.showPostItemPage(); }

    // --- HÀM CỐT LÕI 2: VẼ THẺ CARD (FIX ẢNH THẬT & FIX CHI TIẾT BUTTON) ---
    private VBox createProductCard(JsonObject itemData, String calculatedStatus) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefSize(280, 390);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);");

        // Bóc tách thông tin thật từ Database
        String id = itemData.get("item_id").getAsString();
        String name = itemData.get("name").getAsString();
        double price = itemData.get("starting_price").getAsDouble();
        String imagePathStr = itemData.has("image_path") ? itemData.get("image_path").getAsString() : "";
        String start = itemData.has("start_time") ? itemData.get("start_time").getAsString() : "--/--";
        String end = itemData.has("end_time") ? itemData.get("end_time").getAsString() : "--/--";

        // [FIX ẢNH THẬT]: Tự động chuyển đường dẫn ảnh (ví dụ: C:\...\image.jpg) thành hình ảnh thật
        ImageView imageView;
        try {
            File imageFile = new File(imagePathStr);
            if (imageFile.exists() && imageFile.isFile()) {
                // Chuyển đường dẫn thành định dạng URI (ví dụ: file:/C:/.../image.jpg) để JavaFX hiểu
                Image image = new Image(imageFile.toURI().toString());
                imageView = new ImageView(image);
                imageView.setFitWidth(250); // Cố định chiều rộng 250px
                imageView.setFitHeight(140); // Cố định chiều cao 140px
                imageView.setPreserveRatio(true); // Giữ đúng tỷ lệ ảnh để không bị méo
                imageView.setStyle("-fx-background-radius: 8;");
            } else {
                // Nếu file ảnh không tồn tại thì hiện placeholder camera camera
                Label imgPlaceholder = new Label("📸 " + id + "\n(Không tìm thấy ảnh)");
                imgPlaceholder.setAlignment(Pos.CENTER);
                imgPlaceholder.setPrefSize(250, 140);
                imgPlaceholder.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-text-fill: #bdc3c7; -fx-font-size: 14px; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");
                imageView = new ImageView(); // Placeholder tạm
                card.getChildren().add(imgPlaceholder); // Nhét tạm placeholder trước
            }
        } catch (Exception e) {
            System.err.println("Lỗi nạp ảnh: " + e.getMessage());
            Label imgPlaceholder = new Label("❌ Lỗi Ảnh");
            imgPlaceholder.setAlignment(Pos.CENTER);
            imgPlaceholder.setPrefSize(250, 140);
            card.getChildren().add(imgPlaceholder);
            imageView = new ImageView();
        }

        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1a1a1a;");
        lblName.setWrapText(true);
        lblName.setPrefHeight(40);

        VBox timeBox = new VBox(3);
        Label lblStart = new Label("⏱ Bắt đầu: " + start);
        Label lblEnd = new Label("⌛ Kết thúc: " + end);
        lblStart.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        lblEnd.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        timeBox.getChildren().addAll(lblStart, lblEnd);

        Label lblPrice = new Label("GIÁ HIỆN TẠI\n" + String.format("%,.0f VNĐ", price));
        lblPrice.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a1a; -fx-font-size: 14px;");

        // Gán STATUS và MÀU SẮC chuẩn theo LacViet based on the calculated status
        String statusText = "ĐANG DIỄN RA";
        String statusColor = "#c8a97e";
        if(calculatedStatus.equals("PENDING")) { statusText = "CHƯA ĐẤU GIÁ"; statusColor = "#7f8c8d"; }
        if(calculatedStatus.equals("FINISHED")) { statusText = "ĐÃ KẾT THÚC"; statusColor = "#e74c3c"; }

        Label lblStatus = new Label(statusText);
        lblStatus.setStyle("-fx-background-color: #f4f4f4; -fx-text-fill: " + statusColor + "; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");

        HBox actionBox = new HBox();
        actionBox.setAlignment(Pos.CENTER_LEFT);
        Button btnDetail = new Button("Chi tiết đặt giá");
        btnDetail.setStyle("-fx-background-color: transparent; -fx-text-fill: #c8a97e; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0;");

        // [FIX CHI TIẾT BUTTON]: Gọi hàm mở trang chi tiết và đặt giá khi ấn
        btnDetail.setOnAction(e -> {
            if (dashboardController != null) {
                // [Nối luồng sang BƯỚC 2] Lệnh điều hướng: Mở file product-detail-view.fxml và truyền toàn bộ dữ liệu "itemData" sang
                dashboardController.showProductDetail(itemData);
            }
        });

        actionBox.getChildren().addAll(lblStatus, new Region(), btnDetail);
        HBox.setHgrow(actionBox.getChildren().get(1), Priority.ALWAYS);

        // Gom tất cả vào Card
        if(imageView.getImage() != null) card.getChildren().add(imageView); // Nhét ảnh thật nếu load thành công
        card.getChildren().addAll(lblName, timeBox, lblPrice, actionBox);

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(200,169,126,0.4), 15, 0, 0, 6); -fx-translate-y: -5;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4); -fx-translate-y: 0;"));

        return card;
    }
}