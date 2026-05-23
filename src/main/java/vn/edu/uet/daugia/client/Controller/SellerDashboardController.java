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

    // ===== TABLEVIEW + CỘT (GIỮ NGUYÊN) =====
    @FXML private TableView<Product> tableProducts;
    @FXML private TableColumn<Product, String> colId, colName, colSession;
    @FXML private TableColumn<Product, Double> colStartPrice;
    @FXML private TableColumn<Product, LocalDateTime> colStartTime, colEndTime;

    // ===== TEXTFIELD CŨ (GIỮ NGUYÊN) =====
    @FXML private TextField txtId, txtName, txtSession, txtStartPrice;
    @FXML private TextArea txtDescription;

    // ===== TEXTFIELD MỚI =====
    @FXML private TextField txtMaxPrice;    // Giá mua đứt
    @FXML private TextField txtImageUrl;    // Link ảnh Drive

    // ===== DATE/TIME KẾT THÚC (GIỮ NGUYÊN) =====
    @FXML private DatePicker dpEndTime;
    @FXML private Spinner<Integer> spinHour, spinMin, spinSec;

    // ===== DATE/TIME BẮT ĐẦU (GIỮ NGUYÊN) =====
    @FXML private DatePicker dpStartTime;
    @FXML private Spinner<Integer> spinStartHour, spinStartMin, spinStartSec;

    private ObservableList<Product> productList;

    // ===== INITIALIZE (GIỮ NGUYÊN HOÀN TOÀN, chỉ thêm sync 2 field mới) =====

    @FXML
    public void initialize() {
        // --- KHÓA CỬA BẢO MẬT (GIỮ NGUYÊN) ---
        String role = SessionManager.getRole();
        if (!"ADMIN".equals(role) && !"SELLER".equals(role)) {
            javafx.application.Platform.runLater(() -> {
                AlertUtil.showError("Cảnh báo bảo mật",
                        "Bạn không có quyền truy cập khu vực này!");
                SceneManager.switchScene(
                        "/view/AuctionList.fxml", "Danh sách sản phẩm");
            });
            return;
        }

        productList = FXCollections.observableArrayList();

        // --- Khởi tạo Spinner (GIỮ NGUYÊN) ---
        initSpinner(spinHour, 23, 23);
        initSpinner(spinMin, 59, 59);
        initSpinner(spinSec, 59, 59);
        LocalDateTime now = LocalDateTime.now();
        initSpinner(spinStartHour, 23, now.getHour());
        initSpinner(spinStartMin,  59, now.getMinute());
        initSpinner(spinStartSec,  59, now.getSecond());
        dpStartTime.setValue(now.toLocalDate());

        // --- Cấu hình cột TableView (GIỮ NGUYÊN) ---
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("session"));
        colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Format tiền tệ (GIỮ NGUYÊN)
        colStartPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else setText(String.format("%,.0f VNĐ", price));
            }
        });

        // Định dạng ngày tháng (GIỮ NGUYÊN)
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        setupDateCell(colStartTime, formatter);
        setupDateCell(colEndTime, formatter);

        tableProducts.setItems(productList);

        // --- Lắng nghe chọn dòng (GIỮ NGUYÊN + THÊM 2 field mới) ---
        tableProducts.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        // Cũ (GIỮ NGUYÊN)
                        txtId.setText(newSelection.getId());
                        txtName.setText(newSelection.getName());
                        txtSession.setText(newSelection.getSession());
                        txtStartPrice.setText(
                                String.format("%.0f", newSelection.getStartPrice()));
                        txtDescription.setText(newSelection.getDescription());

                        if (newSelection.getStartTime() != null) {
                            dpStartTime.setValue(
                                    newSelection.getStartTime().toLocalDate());
                            spinStartHour.getValueFactory().setValue(
                                    newSelection.getStartTime().getHour());
                        }
                        if (newSelection.getEndTime() != null) {
                            dpEndTime.setValue(
                                    newSelection.getEndTime().toLocalDate());
                            spinHour.getValueFactory().setValue(
                                    newSelection.getEndTime().getHour());
                        }
                        txtId.setDisable(true);

                        // MỚI: Đổ dữ liệu 2 field mới khi chọn dòng
                        txtMaxPrice.setText(newSelection.getMaxPrice() > 0
                                ? String.format("%.0f", newSelection.getMaxPrice())
                                : "");
                        txtImageUrl.setText(newSelection.getImageUrl() != null
                                ? newSelection.getImageUrl()
                                : "");
                    }
                });
    }

    // ===== HELPER SPINNER + DATE CELL (GIỮ NGUYÊN) =====

    private void initSpinner(Spinner<Integer> s, int max, int val) {
        s.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, val));
    }

    private void setupDateCell(TableColumn<Product, LocalDateTime> col,
                               DateTimeFormatter fmt) {
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime d, boolean e) {
                super.updateItem(d, e);
                if (e || d == null) setText(null);
                else setText(fmt.format(d));
            }
        });
    }

    // ===== THÊM SẢN PHẨM (GIỮ NGUYÊN LOGIC + thêm maxPrice, imageUrl) =====

    @FXML
    private void handleAdd() {
        try {
            if (txtId.getText().isEmpty()
                    || txtName.getText().isEmpty()
                    || dpEndTime.getValue() == null) {
                AlertUtil.showError("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
                return;
            }

            double price = Double.parseDouble(txtStartPrice.getText());

            // MỚI: Đọc maxPrice (bỏ trống = 0)
            double maxPrice = 0;
            if (!txtMaxPrice.getText().trim().isEmpty()) {
                maxPrice = Double.parseDouble(txtMaxPrice.getText().trim());
            }

            // MỚI: Đọc imageUrl
            String imageUrl = txtImageUrl.getText().trim();

            LocalDateTime start = dpStartTime.getValue().atTime(
                    spinStartHour.getValue(),
                    spinStartMin.getValue(),
                    spinStartSec.getValue());
            LocalDateTime end = dpEndTime.getValue().atTime(
                    spinHour.getValue(),
                    spinMin.getValue(),
                    spinSec.getValue());

            if (end.isBefore(start)) {
                AlertUtil.showError("Lỗi",
                        "Ngày kết thúc phải sau ngày bắt đầu!");
                return;
            }

            long durationMinutes =
                    java.time.Duration.between(start, end).toMinutes();

            // JSON gửi Server (GIỮ NGUYÊN cấu trúc cũ + thêm 2 field mới)
            String json = String.format(
                    "{\"type\":\"CREATE_AUCTION\"," +
                            "\"itemId\":\"%s\"," +
                            "\"itemName\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"startPrice\":%.0f," +
                            "\"maxPrice\":%.0f," +
                            "\"imageUrl\":\"%s\"," +
                            "\"sellerName\":\"%s\"," +
                            "\"durationMinutes\":%d}",
                    txtId.getText(),
                    txtName.getText(),
                    txtDescription.getText(),
                    price,
                    maxPrice,
                    imageUrl,
                    SessionManager.getUsername(),
                    durationMinutes);

            NetworkClient.getInstance().sendRaw(json);

            // Dùng constructor mới 10 tham số
            productList.add(new Product(
                    txtId.getText(), txtName.getText(), txtSession.getText(),
                    price, price, maxPrice,
                    txtDescription.getText(), imageUrl,
                    start, end));

            AlertUtil.showSuccess("Thành công", "Đã tạo phiên đấu giá mới!");
            handleClear();

        } catch (Exception e) {
            AlertUtil.showError("Lỗi", "Dữ liệu không hợp lệ!");
        }
    }

    // ===== SỬA SẢN PHẨM (GIỮ NGUYÊN LOGIC + cập nhật maxPrice, imageUrl) =====

    @FXML
    private void handleUpdate() {
        Product selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            productList.remove(selected);

            double maxPrice = 0;
            if (!txtMaxPrice.getText().trim().isEmpty()) {
                maxPrice = Double.parseDouble(txtMaxPrice.getText().trim());
            }
            String imageUrl = txtImageUrl.getText().trim();

            LocalDateTime start = dpStartTime.getValue().atTime(
                    spinStartHour.getValue(),
                    spinStartMin.getValue(),
                    spinStartSec.getValue());
            LocalDateTime end = dpEndTime.getValue().atTime(
                    spinHour.getValue(),
                    spinMin.getValue(),
                    spinSec.getValue());

            double price = Double.parseDouble(txtStartPrice.getText());

            Product updated = new Product(
                    txtId.getText(), txtName.getText(), txtSession.getText(),
                    price, price, maxPrice,
                    txtDescription.getText(), imageUrl,
                    start, end);

            productList.add(updated);
            tableProducts.refresh();
            handleClear();

        } catch (Exception e) {
            AlertUtil.showError("Lỗi", "Sai định dạng!");
        }
    }

    // ===== XÓA (GIỮ NGUYÊN) =====

    @FXML
    private void handleDelete() {
        Product s = tableProducts.getSelectionModel().getSelectedItem();
        if (s != null) {
            productList.remove(s);
            handleClear();
        }
    }

    // ===== LÀM SẠCH FORM (GIỮ NGUYÊN + thêm 2 field mới) =====

    @FXML
    private void handleClear() {
        // Cũ (GIỮ NGUYÊN)
        txtId.clear();
        txtId.setDisable(false);
        txtName.clear();
        txtSession.clear();
        txtStartPrice.clear();
        txtDescription.clear();

        // MỚI
        txtMaxPrice.clear();
        txtImageUrl.clear();

        LocalDateTime now = LocalDateTime.now();
        dpStartTime.setValue(now.toLocalDate());
        dpEndTime.setValue(null);
        tableProducts.getSelectionModel().clearSelection();
    }

    // ===== BACK + LOGOUT (GIỮ NGUYÊN HOÀN TOÀN) =====

    @FXML
    private void handleBack() {
        if ("ADMIN".equals(SessionManager.getRole())) {
            SceneManager.switchScene(
                    "/view/AuctionList.fxml", "Danh sách sản phẩm");
        } else {
            AlertUtil.showError("Hạn chế",
                    "Người bán không có quyền quay lại danh sách mua!");
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập hệ thống");
    }
}
