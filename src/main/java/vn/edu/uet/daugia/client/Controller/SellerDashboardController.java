package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.util.AlertUtil;
import vn.edu.uet.daugia.client.util.SceneManager;
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

    // Đã bổ sung cột EndTime theo yêu cầu
    @FXML private TableColumn<Product, LocalDateTime> colEndTime;

    @FXML private TextField txtId, txtName, txtSession, txtStartPrice;
    @FXML private TextArea txtDescription;

    // Đã bổ sung DatePicker
    @FXML private DatePicker dpEndTime;

    private ObservableList<Product> productList;

    @FXML
    public void initialize() {
        // Khởi tạo danh sách trống hoặc load từ DB/Server
        productList = FXCollections.observableArrayList();

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("session"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Format lại cột ngày kết thúc cho đẹp (VD: 15/05/2026 23:59:59)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        colEndTime.setCellFactory(tc -> new TableCell<Product, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(formatter.format(date));
                }
            }
        });

        tableProducts.setItems(productList);

        // Lắng nghe sự kiện click vào bảng để đổ dữ liệu sang form
        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtId.setText(newSelection.getId());
                txtName.setText(newSelection.getName());
                txtSession.setText(newSelection.getSession());
                txtStartPrice.setText(String.valueOf(newSelection.getStartPrice()));
                txtDescription.setText(newSelection.getDescription());

                // Đổ dữ liệu ngày kết thúc vào DatePicker
                if (newSelection.getEndTime() != null) {
                    dpEndTime.setValue(newSelection.getEndTime().toLocalDate());
                }

                txtId.setDisable(true); // Không cho sửa mã SP khi cập nhật
            }
        });
    }

    @FXML
    private void handleAdd() {
        try {
            if (txtId.getText().isEmpty() || txtName.getText().isEmpty()) {
                AlertUtil.showError("Lỗi nhập liệu", "Vui lòng nhập đầy đủ Mã SP và Tên SP!");
                return;
            }
            double price = Double.parseDouble(txtStartPrice.getText());

            // Xử lý thời gian kết thúc: Nếu không chọn thì mặc định cộng thêm 10 phút
            LocalDateTime endTime;
            if (dpEndTime.getValue() != null) {
                // Lấy ngày chọn và set thời gian mặc định là 23:59:59 của ngày đó
                endTime = dpEndTime.getValue().atTime(23, 59, 59);
            } else {
                endTime = LocalDateTime.now().plusMinutes(10);
            }

            // Tạo sản phẩm mới
            Product newProduct = new Product(
                    txtId.getText(), txtName.getText(), txtSession.getText(),
                    price, price, txtDescription.getText(), endTime
            );

            productList.add(newProduct);
            AlertUtil.showSuccess("Thành công", "Đã thêm sản phẩm mới!");
            handleClear();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi định dạng", "Giá khởi điểm phải là một số hợp lệ!");
        }
    }

    @FXML
    private void handleUpdate() {
        Product selectedProduct = tableProducts.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            AlertUtil.showError("Lỗi", "Vui lòng chọn một sản phẩm trong bảng để cập nhật!");
            return;
        }

        try {
            // Xóa sản phẩm cũ đi (Cập nhật UI cục bộ)
            tableProducts.getItems().remove(selectedProduct);

            double price = Double.parseDouble(txtStartPrice.getText());

            // Xử lý cập nhật thời gian
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
        dpEndTime.setValue(null); // Reset lại DatePicker
        tableProducts.getSelectionModel().clearSelection();
    }

    // Nút điều hướng quay lại
    @FXML
    private void handleBack() {
        SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
    }

    @FXML
    private void handleLogout() {
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập hệ thống");
    }
}