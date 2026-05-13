package com.example.list;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDateTime;

public class AuctionController {
    @FXML private TableView<Product> tableView;
    @FXML private TableColumn<Product, String> colId, colName, colSession;
    @FXML private TableColumn<Product, Double> colStartPrice;

    @FXML
    public void initialize() {
        ObservableList<Product> data = FXCollections.observableArrayList(
                new Product("SP01", "iPhone 15 Pro Max", "Sáng", 28000000, 28500000, "Máy quốc tế, mới 99%", LocalDateTime.now().plusMinutes(3)),
                new Product("SP02", "MacBook Pro M3", "Sáng", 45000000, 45000000, "RAM 16GB, SSD 512GB", LocalDateTime.now().plusMinutes(5)),
                new Product("SP03", "iPad Pro M2", "Sáng", 22000000, 22200000, "Màn hình Liquid Retina XDR", LocalDateTime.now().plusMinutes(10)),
                new Product("SP04", "Apple Watch Ultra 2", "Chiều", 18000000, 18100000, "Dây đeo Alpine Loop", LocalDateTime.now().plusMinutes(4)),
                new Product("SP05", "AirPods Pro Gen 2", "Chiều", 5500000, 5600000, "Chống ồn chủ động vượt trội", LocalDateTime.now().plusMinutes(6)),
                new Product("SP06", "Bàn phím cơ Custom", "Chiều", 3500000, 3500000, "Switch Cherry MX Blue", LocalDateTime.now().plusMinutes(8)),
                new Product("SP07", "Chuột Logitech G502", "Sáng", 1500000, 1550000, "Cảm biến HERO 25K", LocalDateTime.now().plusMinutes(3)),
                new Product("SP08", "Màn hình Dell Ultrasharp", "Sáng", 12000000, 12000000, "Độ phân giải 4K chuyên đồ họa", LocalDateTime.now().plusMinutes(7)),
                new Product("SP09", "Loa Marshall Emberton", "Chiều", 4000000, 4100000, "Âm thanh đa hướng 360 độ", LocalDateTime.now().plusMinutes(5)),
                new Product("SP10", "Máy ảnh Sony A7 IV", "Chiều", 55000000, 55000000, "Body kèm Lens Kit 28-70mm", LocalDateTime.now().plusMinutes(9))
        );

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("session"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        tableView.setItems(data);

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                showDetail(tableView.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void showDetail(Product product) {
        try {
            // Sửa đường dẫn theo đúng cấu trúc thư mục của bạn
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/list/product-detail-view.fxml"));
            Parent root = loader.load();
            ProductDetailController controller = loader.getController();
            controller.setProductData(product);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Chi tiết: " + product.getName());
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    protected void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/login/login-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}