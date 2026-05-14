package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;

public class AuctionListController {
    @FXML private TableView<Product> tableView;
    @FXML private TableColumn<Product, String> colId, colName, colSession;
    @FXML private TableColumn<Product, Double> colStartPrice;
    @FXML private Button btnSellerMode;

    @FXML
    public void initialize() {
        // --- CHẶN ĐỨNG TỪ VÒNG GỬI XE ---
        if ("BIDDER".equals(SessionManager.getRole())) {
            if (btnSellerMode != null) {
                btnSellerMode.setVisible(false);
                btnSellerMode.setManaged(false); // Dòng này quan trọng để nút không chiếm chỗ
            }
        }
        // --- GIỮ NGUYÊN DỮ LIỆU MẪU CỦA BẠN (8 tham số) ---
        ObservableList<Product> data = FXCollections.observableArrayList(
                new Product("SP01", "iPhone 15 Pro Max", "Sáng", 28000000, 28500000, "Khung Titan, chip A17 Pro mạnh mẽ.", LocalDateTime.now(), LocalDateTime.now().plusMinutes(10)),
                new Product("SP02", "MacBook Pro M3", "Sáng", 45000000, 45000000, "Chip M3 đột phá, màn hình Retina XDR.", LocalDateTime.now(), LocalDateTime.now().plusMinutes(15))
        );

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("session"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));

        colStartPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else setText(String.format("%,.0f VNĐ", price));
            }
        });

        tableView.setItems(data);

        // --- PHÂN QUYỀN NÚT SELLER: Ẩn nút nếu là BIDDER ---
        if ("BIDDER".equals(SessionManager.getRole())) {
            if (btnSellerMode != null) {
                btnSellerMode.setVisible(false);
                btnSellerMode.setManaged(false);
            }
        }

        // --- GIỮ NGUYÊN CLICK ĐÚP SANG TRANG 2 ---
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                Product selected = tableView.getSelectionModel().getSelectedItem();
                ProductDetailController controller = SceneManager.switchSceneAndGetController(
                        "/view/ProductDetail.fxml", "Trang 2: Chi tiết: " + selected.getName());
                if (controller != null) controller.setProductData(selected);
            }
        });
    }

    @FXML
    protected void handleSwitchToSeller() {
        SceneManager.switchScene("/view/SellerDashboard.fxml", "Quản lý sản phẩm");
    }

    @FXML
    protected void handleLogout() {
        SessionManager.logout();
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập");
    }
}