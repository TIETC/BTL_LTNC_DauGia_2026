package com.example.list;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class AuctionController {
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colPrice;

    @FXML private TextField txtName;
    @FXML private TextField txtPrice;

    // Danh sách quan sát (ObservableList) để TableView tự cập nhật khi có thay đổi
    private ObservableList<Product> list;

    @FXML
    public void initialize() {
        // Kết nối các cột với thuộc tính trong class Product
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Khởi tạo danh sách mẫu ban đầu
        list = FXCollections.observableArrayList(
                new Product("Sản phẩm 1", "1.000.000 VNĐ"),
                new Product("Sản phẩm 2", "5.500.000 VNĐ"),
                new Product("Sản phẩm 3", "200.000 VNĐ")
        );

        productTable.setItems(list);
    }

    @FXML
    protected void onAddClick() {
        String name = txtName.getText();
        String price = txtPrice.getText();

        // Kiểm tra nếu người dùng chưa nhập gì thì không thêm
        if (name != null && !name.trim().isEmpty() && price != null && !price.trim().isEmpty()) {
            list.add(new Product(name, price + " VNĐ"));

            // Xóa sạch ô nhập sau khi thêm thành công
            txtName.clear();
            txtPrice.clear();
        }
    }

    @FXML
    protected void onDeleteClick() {
        // Lấy dòng đang được người dùng chọn (bôi xanh)
        Product selected = productTable.getSelectionModel().getSelectedItem();

        if (selected != null) {
            list.remove(selected);
        }
    }
}