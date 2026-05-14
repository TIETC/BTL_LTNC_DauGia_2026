package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.util.AlertUtil;
import vn.edu.uet.daugia.client.util.SceneManager;

// THÊM MỚI: để gửi dữ liệu lên Server
import vn.edu.uet.daugia.client.network.NetworkClient;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SellerDashboardController {

    @FXML private TableView<Product> tableProducts;
    @FXML private TableColumn<Product, String> colId, colName, colSession;
    @FXML private TableColumn<Product, Double> colStartPrice;
    @FXML private TableColumn<Product, LocalDateTime> colEndTime;

    @FXML private TextField txtId, txtName, txtSession, txtStartPrice;
    @FXML private TextArea txtDescription;
    @FXML private DatePicker dpEndTime;

    private ObservableList<Product> productList;

    @FXML
    public void initialize() {
        productList = FXCollections.observableArrayList();

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("session"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        colEndTime.setCellFactory(tc -> new TableCell<Product, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) setText(null);
                else setText(formatter.format(date));
            }
        });

        tableProducts.setItems(productList);

        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtId.setText(newSelection.getId());
                txtName.setText(newSelection.getName());
                txtSession.setText(newSelection.getSession());
                txtStartPrice.setText(String.valueOf(newSelection.getStartPrice()));
                txtDescription.setText(newSelection.getDescription());
                if (newSelection.getEndTime() != null) {
                    dpEndTime.setValue(newSelection.getEndTime().toLocalDate());
                }
                txtId.setDisable(true);
            }
        });
    }

    @FXML
    private void handleAdd() {
        try {
            // 1. Kiểm tra dữ liệu nhập
            if (txtId.getText().isEmpty() || txtName.getText().isEmpty()) {
                AlertUtil.showError("Lỗi nhập liệu", "Vui lòng nhập đầy đủ Mã SP và Tên SP!");
                return;
            }
            double price = Double.parseDouble(txtStartPrice.getText());

            LocalDateTime endTime;
            if (dpEndTime.getValue() != null) {
                endTime = dpEndTime.getValue().atTime(23, 59, 59);
            } else {
                endTime = LocalDateTime.now().plusMinutes(10);
            }

            // 2. Tính số phút còn lại từ bây giờ đến endTime (để gửi lên Server)
            long durationMinutes = java.time.Duration.between(
                    LocalDateTime.now(), endTime
            ).toMinutes();

            if (durationMinutes <= 0) durationMinutes = 10; // tối thiểu 10 phút

            // 3. Đóng gói JSON và gửi lên Server
            //    Server sẽ tạo Auction và lưu vào AuctionManager + Database
            String json = String.format(
                    "{\"type\":\"CREATE_AUCTION\"," +
                            "\"itemId\":\"%s\"," +
                            "\"itemName\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"startPrice\":%.0f," +
                            "\"sellerName\":\"seller01\"," +   // sau này thay bằng tên người đang đăng nhập
                            "\"durationMinutes\":%d}",
                    txtId.getText(),
                    txtName.getText(),
                    txtDescription.getText(),
                    price,
                    durationMinutes
            );

            NetworkClient.getInstance().sendRaw(json);
            System.out.println("Đã gửi CREATE_AUCTION lên Server: " + txtId.getText());

            // 4. Cập nhật TableView trên màn hình ngay (không cần chờ Server phản hồi)
            Product newProduct = new Product(
                    txtId.getText(), txtName.getText(), txtSession.getText(),
                    price, price, txtDescription.getText(), endTime
            );
            productList.add(newProduct);

            AlertUtil.showSuccess("Thành công", "Đã tạo phiên đấu giá: " + txtName.getText());
            handleClear();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi định dạng", "Giá khởi điểm phải là một số hợp lệ!");
        }
    }

    @FXML
    private void handleUpdate() {
        // Phần update chỉ cập nhật UI cục bộ, chưa cần gửi Server
        // (vì đề bài không yêu cầu sửa phiên đang chạy)
        Product selectedProduct = tableProducts.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            AlertUtil.showError("Lỗi", "Vui lòng chọn một sản phẩm trong bảng để cập nhật!");
            return;
        }

        try {
            tableProducts.getItems().remove(selectedProduct);
            double price = Double.parseDouble(txtStartPrice.getText());

            LocalDateTime endTime = (dpEndTime.getValue() != null)
                    ? dpEndTime.getValue().atTime(23, 59, 59)
                    : selectedProduct.getEndTime();

            Product updatedProduct = new Product(
                    txtId.getText(), txtName.getText(), txtSession.getText(),
                    price, price, txtDescription.getText(), endTime
            );

            productList.add(updatedProduct);
            tableProducts.refresh();
            AlertUtil.showSuccess("Thành công", "Cập nhật sản phẩm thành công!");
            handleClear();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi định dạng", "Giá khởi điểm phải là số hợp lệ!");
        }
    }

    @FXML
    private void handleDelete() {
        Product selectedProduct = tableProducts.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            productList.remove(selectedProduct);
            AlertUtil.showSuccess("Thành công", "Đã xóa sản phẩm!");
            handleClear();
        } else {
            AlertUtil.showError("Lỗi", "Vui lòng chọn sản phẩm cần xóa!");
        }
    }

    @FXML
    private void handleClear() {
        txtId.clear();
        txtId.setDisable(false);
        txtName.clear();
        txtSession.clear();
        txtStartPrice.clear();
        txtDescription.clear();
        dpEndTime.setValue(null);
        tableProducts.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
    }

    @FXML
    private void handleLogout() {
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập hệ thống");
    }
}