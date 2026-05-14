package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;

public class AuctionListController {
    @FXML private TableView<Product> tableView;
    @FXML private TableColumn<Product, String> colId, colName, colSession;
    @FXML private TableColumn<Product, Double> colStartPrice;

    @FXML
    public void initialize() {
        // Cập nhật dữ liệu thật siêu chi tiết được tra cứu từ thực tế
        ObservableList<Product> data = FXCollections.observableArrayList(
                new Product("SP01", "iPhone 15 Pro Max", "Sáng", 28000000, 28500000,
                        "Khung viền Titan cấp hàng không vũ trụ, chip A17 Pro mạnh mẽ nhất. Màn hình 6.7 inch Super Retina XDR. Hệ thống camera chuyên nghiệp 48MP với khả năng zoom quang học 5x. Cổng sạc USB-C tốc độ cao.", LocalDateTime.now().plusMinutes(3)),

                new Product("SP02", "MacBook Pro M3", "Sáng", 45000000, 45000000,
                        "Chip Apple M3 đột phá với kiến trúc 3nm. Màn hình 14.2 inch Liquid Retina XDR siêu nét, độ sáng 1000 nits. RAM 16GB, SSD 512GB. Thời lượng pin lên đến 22 giờ, tản nhiệt quạt tàng hình siêu êm, lý tưởng cho lập trình và đồ họa.", LocalDateTime.now().plusMinutes(5)),

                new Product("SP03", "iPad Pro M2", "Sáng", 22000000, 22200000,
                        "Trang bị chip M2 mạnh ngang ngửa laptop. Màn hình 12.9 inch Liquid Retina XDR cao cấp. Hỗ trợ Apple Pencil 2 và Magic Keyboard. Camera kép 12MP/10MP, kết hợp cảm biến LiDAR hỗ trợ AR tuyệt vời.", LocalDateTime.now().plusMinutes(10)),

                new Product("SP04", "Apple Watch Ultra 2", "Chiều", 18000000, 18100000,
                        "Khung Titanium 49mm siêu bền, chống nước 100m. Màn hình luôn sáng (Always-On) độ sáng lên tới 3000 nits. Tích hợp GPS băng tần kép, la bàn chính xác, pin sử dụng liên tục 36 giờ. Phù hợp thể thao mạo hiểm.", LocalDateTime.now().plusMinutes(4)),

                new Product("SP05", "AirPods Pro Gen 2", "Chiều", 5500000, 5600000,
                        "Trang bị chip H2 mang lại âm thanh không gian (Spatial Audio) vượt trội. Công nghệ chống ồn chủ động (ANC) tốt gấp 2 lần thế hệ trước. Chế độ Xuyên âm tự động. Hộp sạc tích hợp loa tìm kiếm.", LocalDateTime.now().plusMinutes(6)),

                new Product("SP06", "Bàn phím cơ Custom", "Chiều", 3500000, 3500000,
                        "Bàn phím layout 75% nhỏ gọn. Sử dụng Switch Cherry MX Blue cho cảm giác gõ tactile lách cách đặc trưng. Keycap PBT Double-shot siêu bền không mờ chữ. Tích hợp LED RGB 16.8 triệu màu.", LocalDateTime.now().plusMinutes(8)),

                new Product("SP07", "Chuột Logitech G502", "Sáng", 1500000, 1550000,
                        "Chuột gaming huyền thoại với cảm biến HERO 25K siêu chính xác. Đi kèm bộ tạ để tùy chỉnh trọng lượng. 11 nút bấm có thể lập trình macro qua phần mềm G Hub. Switch cơ học tuổi thọ 50 triệu lần nhấn.", LocalDateTime.now().plusMinutes(3)),

                new Product("SP08", "Màn hình Dell Ultrasharp", "Sáng", 12000000, 12000000,
                        "Màn hình 27 inch độ phân giải 4K UHD. Tấm nền IPS cho góc nhìn rộng 178 độ, độ phủ màu 99% sRGB chuẩn đồ họa. Viền siêu mỏng InfinityEdge, chân đế xoay gập linh hoạt.", LocalDateTime.now().plusMinutes(7)),

                new Product("SP09", "Loa Marshall Emberton", "Chiều", 4000000, 4100000,
                        "Loa bluetooth di động mang phong cách retro cổ điển. Công nghệ True Stereophonic cho âm thanh đa hướng 360 độ sống động. Chống nước IPX7, thời lượng pin 20+ giờ chỉ với một lần sạc đầy.", LocalDateTime.now().plusMinutes(5)),

                new Product("SP10", "Máy ảnh Sony A7 IV", "Chiều", 55000000, 55000000,
                        "Máy ảnh Mirrorless Full-frame cảm biến 33MP thế hệ mới. Khả năng quay video 4K 60fps. Hệ thống lấy nét tự động Real-time Eye AF cho người, động vật và chim. Kèm ống kính Kit 28-70mm đa dụng.", LocalDateTime.now().plusMinutes(9))
        );

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("session"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));

        colStartPrice.setCellFactory(tc -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) { setText(null); }
                else { setText(String.format("%,.0f VNĐ", price)); }
            }
        });

        tableView.setItems(data);

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                Product selectedProduct = tableView.getSelectionModel().getSelectedItem();
                BiddingRoomController controller = SceneManager.switchSceneAndGetController(
                        "/view/BiddingRoom.fxml", "Đấu giá trực tiếp: " + selectedProduct.getName());
                if (controller != null) { controller.setProductData(selectedProduct); }
            }
        });
    }

    // NÚT MỚI: Dùng để test màn hình Seller
    @FXML
    protected void handleSwitchToSeller(ActionEvent event) {
        SceneManager.switchScene("/view/SellerDashboard.fxml", "Quản lý sản phẩm (Seller)");
    }

    @FXML
    protected void handleLogout(ActionEvent event) {
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập hệ thống");
    }
}