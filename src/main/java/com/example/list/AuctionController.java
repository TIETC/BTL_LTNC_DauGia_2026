package com.example.list;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDateTime;

public class AuctionController {
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> colId, colName, colSession, colPrice;

    @FXML
    public void initialize() {
        // Ánh xạ chính xác các cột
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("session"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));

        ObservableList<Product> list = FXCollections.observableArrayList(
                new Product("SP01", "iPhone 15", "Sáng", "25000000", "25000000", "Máy mới", LocalDateTime.now().plusMinutes(3)),
                new Product("SP02", "MacBook M3", "Sáng", "40000000", "40000000", "RAM 16GB", LocalDateTime.now().plusMinutes(5)),
                new Product("SP03", "iPad Pro", "Sáng", "20000000", "20000000", "Màn Liquid", LocalDateTime.now().plusMinutes(10)),
                new Product("SP04", "Apple Watch", "Chiều", "10000000", "10000000", "Dây đen", LocalDateTime.now().plusMinutes(3)),
                new Product("SP05", "AirPods 2", "Chiều", "5000000", "5000000", "Chống ồn", LocalDateTime.now().plusMinutes(5)),
                new Product("SP06", "Bàn phím cơ", "Chiều", "2500000", "2500000", "RGB", LocalDateTime.now().plusMinutes(10)),
                new Product("SP07", "Chuột G502", "Sáng", "1200000", "1200000", "Gaming", LocalDateTime.now().plusMinutes(3)),
                new Product("SP08", "Màn Dell", "Sáng", "12000000", "12000000", "4K", LocalDateTime.now().plusMinutes(5)),
                new Product("SP09", "Loa Marshall", "Chiều", "8000000", "8000000", "Cổ điển", LocalDateTime.now().plusMinutes(10)),
                new Product("SP10", "Sony A7IV", "Chiều", "55000000", "55000000", "Full-frame", LocalDateTime.now().plusMinutes(15))
        );
        productTable.setItems(list);

        productTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Product selected = productTable.getSelectionModel().getSelectedItem();
                if (selected != null) openDetailWindow(selected);
            }
        });
    }

    private void openDetailWindow(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("product-detail-view.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            ProductDetailController controller = loader.getController();
            controller.setProductData(product);
            stage.setTitle("Chi tiết: " + product.getName());
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}