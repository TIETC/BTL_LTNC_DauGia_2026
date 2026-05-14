package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.util.AlertUtil;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager;
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
    @FXML private TableColumn<Product, LocalDateTime> colStartTime, colEndTime;

    @FXML private TextField txtId, txtName, txtSession, txtStartPrice;
    @FXML private TextArea txtDescription;

    // Các trường thời gian Kết thúc
    @FXML private DatePicker dpEndTime;
    @FXML private Spinner<Integer> spinHour, spinMin, spinSec;

    // Các trường thời gian Bắt đầu
    @FXML private DatePicker dpStartTime;
    @FXML private Spinner<Integer> spinStartHour, spinStartMin, spinStartSec;

    private ObservableList<Product> productList;

    @FXML
    public void initialize() {
        // --- BỔ SUNG: KHÓA CỬA BẢO MẬT ---
        String role = SessionManager.getRole();
        if (!"ADMIN".equals(role) && !"SELLER".equals(role)) {
            // Nếu là kẻ đột nhập (Bidder hoặc khách), đá về trang Login hoặc Danh sách
            javafx.application.Platform.runLater(() -> {
                AlertUtil.showError("Cảnh báo bảo mật", "Bạn không có quyền truy cập khu vực này!");
                SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
            });
            return; // Dừng khởi tạo các thành phần bên dưới
        }
        productList = FXCollections.observableArrayList();

        // 1. Khởi tạo Spinner
        initSpinner(spinHour, 23, 23); initSpinner(spinMin, 59, 59); initSpinner(spinSec, 59, 59);
        LocalDateTime now = LocalDateTime.now();
        initSpinner(spinStartHour, 23, now.getHour()); initSpinner(spinStartMin, 59, now.getMinute()); initSpinner(spinStartSec, 59, now.getSecond());
        dpStartTime.setValue(now.toLocalDate());

        // 2. Cấu hình Column TableView
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("session"));
        colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Format tiền tệ tránh 1.0E8
        colStartPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else setText(String.format("%,.0f VNĐ", price));
            }
        });

        // Định dạng ngày tháng
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        setupDateCell(colStartTime, formatter);
        setupDateCell(colEndTime, formatter);

        tableProducts.setItems(productList);

        // 3. Lắng nghe chọn dòng để đổ dữ liệu (SYNC FORM)
        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtId.setText(newSelection.getId());
                txtName.setText(newSelection.getName());
                txtSession.setText(newSelection.getSession());
                txtStartPrice.setText(String.format("%.0f", newSelection.getStartPrice()));
                txtDescription.setText(newSelection.getDescription());
                if (newSelection.getStartTime() != null) {
                    dpStartTime.setValue(newSelection.getStartTime().toLocalDate());
                    spinStartHour.getValueFactory().setValue(newSelection.getStartTime().getHour());
                }
                if (newSelection.getEndTime() != null) {
                    dpEndTime.setValue(newSelection.getEndTime().toLocalDate());
                    spinHour.getValueFactory().setValue(newSelection.getEndTime().getHour());
                }
                txtId.setDisable(true);
            }
        });
    }

    private void initSpinner(Spinner<Integer> s, int max, int val) {
        s.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, val));
    }

    private void setupDateCell(TableColumn<Product, LocalDateTime> col, DateTimeFormatter fmt) {
        col.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime d, boolean e) {
                super.updateItem(d, e);
                if (e || d == null) setText(null); else setText(fmt.format(d));
            }
        });
    }

    @FXML
    private void handleAdd() {
        try {
            if (txtId.getText().isEmpty() || txtName.getText().isEmpty() || dpEndTime.getValue() == null) {
                AlertUtil.showError("Lỗi", "Vui lòng nhập đầy đủ thông tin!"); return;
            }
            double price = Double.parseDouble(txtStartPrice.getText());
            LocalDateTime start = dpStartTime.getValue().atTime(spinStartHour.getValue(), spinStartMin.getValue(), spinStartSec.getValue());
            LocalDateTime end = dpEndTime.getValue().atTime(spinHour.getValue(), spinMin.getValue(), spinSec.getValue());

            if (end.isBefore(start)) {
                AlertUtil.showError("Lỗi", "Ngày kết thúc phải sau ngày bắt đầu!"); return;
            }

            long durationMinutes = java.time.Duration.between(start, end).toMinutes();

            // --- LOGIC GỬI JSON NGUYÊN BẢN CỦA BẠN ---
            String json = String.format(
                    "{\"type\":\"CREATE_AUCTION\"," +
                            "\"itemId\":\"%s\"," +
                            "\"itemName\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"startPrice\":%.0f," +
                            "\"sellerName\":\"%s\"," +
                            "\"durationMinutes\":%d}",
                    txtId.getText(), txtName.getText(), txtDescription.getText(),
                    price, SessionManager.getUsername(), durationMinutes
            );

            NetworkClient.getInstance().sendRaw(json);
            productList.add(new Product(txtId.getText(), txtName.getText(), txtSession.getText(), price, price, txtDescription.getText(), start, end));
            AlertUtil.showSuccess("Thành công", "Đã tạo phiên đấu giá mới!");
            handleClear();
        } catch (Exception e) { AlertUtil.showError("Lỗi", "Dữ liệu không hợp lệ!"); }
    }

    @FXML
    private void handleUpdate() {
        Product selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            productList.remove(selected);
            LocalDateTime start = dpStartTime.getValue().atTime(spinStartHour.getValue(), spinStartMin.getValue(), spinStartSec.getValue());
            LocalDateTime end = dpEndTime.getValue().atTime(spinHour.getValue(), spinMin.getValue(), spinSec.getValue());
            Product updated = new Product(txtId.getText(), txtName.getText(), txtSession.getText(), Double.parseDouble(txtStartPrice.getText()), Double.parseDouble(txtStartPrice.getText()), txtDescription.getText(), start, end);
            productList.add(updated);
            tableProducts.refresh(); handleClear();
        } catch (Exception e) { AlertUtil.showError("Lỗi", "Sai định dạng!"); }
    }

    @FXML
    private void handleDelete() {
        Product s = tableProducts.getSelectionModel().getSelectedItem();
        if (s != null) { productList.remove(s); handleClear(); }
    }

    @FXML
    private void handleClear() {
        txtId.clear(); txtId.setDisable(false); txtName.clear(); txtSession.clear(); txtStartPrice.clear(); txtDescription.clear();
        LocalDateTime now = LocalDateTime.now();
        dpStartTime.setValue(now.toLocalDate()); dpEndTime.setValue(null);
        tableProducts.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleBack() {
        if ("ADMIN".equals(SessionManager.getRole())) {
            SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
        } else {
            AlertUtil.showError("Hạn chế", "Người bán không có quyền quay lại danh sách mua!");
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập hệ thống");
    }
}