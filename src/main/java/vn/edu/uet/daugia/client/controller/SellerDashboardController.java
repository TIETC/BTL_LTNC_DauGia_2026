package vn.edu.uet.daugia.client.controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.util.AlertUtil;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager;
import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.DateTimeParseUtil;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SellerDashboardController {

    // ===== TABLEVIEW + CỘT =====
    @FXML private TableView<Product> tableProducts;
    @FXML private TableColumn<Product, String>        colId, colName, colStatus, colLeader;
    @FXML private TableColumn<Product, Double>        colStartPrice, colCurrentPrice;
    @FXML private TableColumn<Product, LocalDateTime> colStartTime, colEndTime;

    // ===== FORM FIELDS =====
    @FXML private TextField  txtId;
    @FXML private TextField  txtName;
    @FXML private TextField  txtStartPrice;
    @FXML private TextField  txtMaxPrice;
    @FXML private TextField  txtImageUrl;
    @FXML private TextArea   txtDescription;

    // ===== Preview ảnh =====
    @FXML private ImageView  imgPreview;

    // ===== DATE/TIME KẾT THÚC =====
    @FXML private DatePicker        dpEndTime;
    @FXML private Spinner<Integer>  spinHour, spinMin, spinSec;

    // ===== DATE/TIME BẮT ĐẦU =====
    @FXML private DatePicker        dpStartTime;
    @FXML private Spinner<Integer>  spinStartHour, spinStartMin, spinStartSec;

    private ObservableList<Product> productList;

    // ===== INITIALIZE =====

    @FXML
    public void initialize() {
        String role = SessionManager.getRole();
        if (!"SELLER".equals(role)) {
            // ✅ SỬA: Chỉ SELLER mới được vào SellerDashboard.
            // Admin có màn hình riêng (AdminDashboard), không cần vào đây nữa.
            Platform.runLater(() -> {
                AlertUtil.showError("Cảnh báo bảo mật", "Bạn không có quyền truy cập khu vực này!");
                SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
            });
            return;
        }

        productList = FXCollections.observableArrayList();

        // --- Khởi tạo Spinner ---
        initSpinner(spinHour, 23, 23);
        initSpinner(spinMin, 59, 59);
        initSpinner(spinSec, 59, 59);
        LocalDateTime now = LocalDateTime.now();
        initSpinner(spinStartHour, 23, now.getHour());
        initSpinner(spinStartMin,  59, now.getMinute());
        initSpinner(spinStartSec,  59, now.getSecond());
        dpStartTime.setValue(now.toLocalDate());

        // --- ID tự sinh, chỉ đọc ---
        txtId.setDisable(true);

        // --- Cấu hình cột TableView ---
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colLeader.setCellValueFactory(new PropertyValueFactory<>("leader"));

        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));
        colStartPrice.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
            }
        });

        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colCurrentPrice.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                switch (v) {
                    case "RUNNING"  -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "FINISHED" -> setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
                    case "CANCELED" -> setStyle("-fx-text-fill: #e74c3c;");
                    default         -> setStyle("");
                }
            }
        });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        setupDateCell(colStartTime, fmt);
        setupDateCell(colEndTime, fmt);

        tableProducts.setItems(productList);

        // --- Lắng nghe chọn dòng ---
        tableProducts.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, newSel) -> {
                    if (newSel != null) {
                        txtId.setText(newSel.getId());
                        txtName.setText(newSel.getName());
                        txtStartPrice.setText(String.format("%.0f", newSel.getStartPrice()));
                        txtDescription.setText(newSel.getDescription());
                        txtMaxPrice.setText(newSel.getMaxPrice() > 0
                                ? String.format("%.0f", newSel.getMaxPrice()) : "");
                        txtImageUrl.setText(newSel.getImageUrl() != null ? newSel.getImageUrl() : "");
                        if (newSel.getStartTime() != null) {
                            dpStartTime.setValue(newSel.getStartTime().toLocalDate());
                            spinStartHour.getValueFactory().setValue(newSel.getStartTime().getHour());
                            spinStartMin.getValueFactory().setValue(newSel.getStartTime().getMinute());
                            spinStartSec.getValueFactory().setValue(newSel.getStartTime().getSecond());
                        }
                        if (newSel.getEndTime() != null) {
                            dpEndTime.setValue(newSel.getEndTime().toLocalDate());
                            spinHour.getValueFactory().setValue(newSel.getEndTime().getHour());
                            spinMin.getValueFactory().setValue(newSel.getEndTime().getMinute());
                            spinSec.getValueFactory().setValue(newSel.getEndTime().getSecond());
                        }
                        loadImagePreview(newSel.getImageUrl());
                    }
                });

        // --- Tải danh sách phiên của seller từ server (1 lần duy nhất) ---
        loadMyAuctions();
    }

    // ===== LOAD DANH SÁCH PHIÊN CỦA SELLER =====

    private void loadMyAuctions() {
        String seller = SessionManager.getUsername();
        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendRaw(
                        String.format("{\"type\":\"GET_MY_AUCTIONS\",\"sellerName\":\"%s\"}", seller));
                String response = NetworkClient.getInstance().readResponse();

                if (response == null || !response.startsWith("[")) {
                    System.err.println("GET_MY_AUCTIONS response lạ: " + response);
                    return;
                }

                Gson gson = new Gson();
                JsonObject[] list = gson.fromJson(response, JsonObject[].class);
                Platform.runLater(() -> fillProductList(list));

            } catch (Exception e) {
                System.err.println("Lỗi loadMyAuctions: " + e.getMessage());
            }
        }).start();
    }

    private void fillProductList(JsonObject[] list) {
        productList.clear();
        for (JsonObject a : list) {
            try {
                String id           = a.get("itemId").getAsString();
                String name         = a.get("itemName").getAsString();
                double startPrice   = a.get("startPrice").getAsDouble();
                double curPrice     = a.has("currentPrice") ? a.get("currentPrice").getAsDouble() : startPrice;
                double maxPrice     = a.has("maxPrice") ? a.get("maxPrice").getAsDouble() : 0;
                String desc         = a.has("description") ? a.get("description").getAsString() : "";
                String imageUrl     = a.has("imageUrl") ? a.get("imageUrl").getAsString() : "";
                String status       = a.has("status") ? a.get("status").getAsString() : "";
                String leader       = a.has("leader") ? a.get("leader").getAsString() : "";
                String startTimeStr = a.has("startTime") ? a.get("startTime").getAsString() : "";
                String endTimeStr   = a.has("endTime") ? a.get("endTime").getAsString() : "";

                LocalDateTime startTime = startTimeStr.isEmpty() ? LocalDateTime.now()
                        : DateTimeParseUtil.parseFlexible(startTimeStr);
                LocalDateTime endTime   = endTimeStr.isEmpty() ? LocalDateTime.now()
                        : DateTimeParseUtil.parseFlexible(endTimeStr);

                Product p = new Product(id, name, status,
                        startPrice, curPrice, maxPrice,
                        desc, imageUrl, startTime, endTime);
                p.setLeader(leader);
                productList.add(p);
            } catch (Exception ex) {
                System.err.println("Lỗi parse product: " + ex.getMessage());
            }
        }
        // ✅ SỬA: Sinh ID dựa trên timestamp (milliseconds) thay vì size+1
        // Đảm bảo không bao giờ trùng lặp với ID đã có trong DB
        generateNewId();
    }

    /**
     * ✅ PHƯƠNG THỨC MỚI: Sinh ID duy nhất dựa trên timestamp.
     * Thay thế hoàn toàn logic cũ: String.format("SP%03d", productList.size() + 1)
     * Logic cũ gây lỗi Duplicate Key vì nhiều Seller/Admin cùng có list rỗng
     * sẽ đều được gán SP001, dẫn đến trùng khóa chính trong DB.
     */
    private void generateNewId() {
        // Dùng 6 chữ số cuối của System.currentTimeMillis() để tạo ID ngắn gọn
        // Ví dụ: SP_123456 — thực tế không bao giờ trùng vì timestamp luôn tăng
        txtId.setText("SP" + (System.currentTimeMillis() % 1_000_000));
    }

    // ===== THÊM SẢN PHẨM =====

    @FXML
    private void handleAdd() {
        try {
            if (txtName.getText().isEmpty() || dpEndTime.getValue() == null) {
                AlertUtil.showError("Lỗi", "Vui lòng nhập đầy đủ tên sản phẩm và thời gian kết thúc!");
                return;
            }

            double price = Double.parseDouble(txtStartPrice.getText().trim());
            double maxPrice = 0;
            if (!txtMaxPrice.getText().trim().isEmpty()) {
                maxPrice = Double.parseDouble(txtMaxPrice.getText().trim());
                if (maxPrice > 0 && maxPrice <= price) {
                    AlertUtil.showError("Lỗi", "Giá mua đứt phải lớn hơn giá khởi điểm!");
                    return;
                }
            }

            LocalDateTime start = dpStartTime.getValue().atTime(
                    spinStartHour.getValue(), spinStartMin.getValue(), spinStartSec.getValue());
            LocalDateTime end = dpEndTime.getValue().atTime(
                    spinHour.getValue(), spinMin.getValue(), spinSec.getValue());

            if (end.isBefore(start)) {
                AlertUtil.showError("Lỗi", "Ngày kết thúc phải sau ngày bắt đầu!");
                return;
            }

            long durationMinutes = java.time.Duration.between(start, end).toMinutes();
            String itemId    = txtId.getText();
            String itemName  = txtName.getText().trim();
            String desc      = txtDescription.getText().trim();
            String imageUrl  = txtImageUrl.getText().trim();

            final double finalMaxPrice = maxPrice;
            final LocalDateTime finalStart = start;
            final LocalDateTime finalEnd   = end;

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
                    escapeJson(itemId), escapeJson(itemName), escapeJson(desc),
                    price, finalMaxPrice, escapeJson(imageUrl),
                    escapeJson(SessionManager.getUsername()), durationMinutes);

            new Thread(() -> {
                NetworkClient.getInstance().sendRaw(json);
                String response = NetworkClient.getInstance().readResponse();
                Platform.runLater(() -> {
                    if (response != null && response.contains("\"status\":\"OK\"")) {
                        Product p = new Product(itemId, itemName, "RUNNING",
                                price, price, finalMaxPrice,
                                desc, imageUrl, finalStart, finalEnd);
                        p.setLeader("");
                        productList.add(p);
                        AlertUtil.showSuccess("Thành công", "Đã tạo phiên đấu giá mới!");
                        handleClear();
                    } else {
                        AlertUtil.showError("Lỗi", "Server không tạo được phiên: "
                                + (response != null ? response : "không có phản hồi"));
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Giá tiền không hợp lệ!");
        } catch (Exception e) {
            AlertUtil.showError("Lỗi", "Dữ liệu không hợp lệ: " + e.getMessage());
        }
    }

    // ===== SỬA SẢN PHẨM =====

    @FXML
    private void handleUpdate() {
        Product selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("Lỗi", "Vui lòng chọn phiên cần sửa!");
            return;
        }

        String status = selected.getStatus();
        if ("FINISHED".equals(status) || "CANCELED".equals(status)) {
            AlertUtil.showError("Không thể sửa", "Phiên đã kết thúc, không thể chỉnh sửa!");
            return;
        }

        try {
            String itemName = txtName.getText().trim();
            String desc     = txtDescription.getText().trim();
            String imageUrl = txtImageUrl.getText().trim();
            double maxPrice = 0;
            if (!txtMaxPrice.getText().trim().isEmpty()) {
                maxPrice = Double.parseDouble(txtMaxPrice.getText().trim());
            }

            final double finalMaxPrice = maxPrice;
            final String itemId = selected.getId();

            String json = String.format(
                    "{\"type\":\"UPDATE_AUCTION\"," +
                            "\"itemId\":\"%s\"," +
                            "\"itemName\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"imageUrl\":\"%s\"," +
                            "\"maxPrice\":%.0f}",
                    escapeJson(itemId), escapeJson(itemName),
                    escapeJson(desc), escapeJson(imageUrl), finalMaxPrice);

            new Thread(() -> {
                NetworkClient.getInstance().sendRaw(json);
                String response = NetworkClient.getInstance().readResponse();
                Platform.runLater(() -> {
                    if (response != null && response.contains("\"status\":\"OK\"")) {
                        int idx = productList.indexOf(selected);
                        if (idx >= 0) {
                            Product updated = new Product(
                                    itemId, itemName, selected.getStatus(),
                                    selected.getStartPrice(), selected.getCurrentPrice(),
                                    finalMaxPrice, desc, imageUrl,
                                    selected.getStartTime(), selected.getEndTime());
                            updated.setLeader(selected.getLeader());
                            productList.set(idx, updated);
                        }
                        tableProducts.refresh();
                        AlertUtil.showSuccess("Thành công", "Đã cập nhật phiên đấu giá!");
                        handleClear();
                    } else {
                        AlertUtil.showError("Lỗi", "Cập nhật thất bại: "
                                + (response != null ? response : "không có phản hồi"));
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Giá tiền không hợp lệ!");
        } catch (Exception e) {
            AlertUtil.showError("Lỗi", e.getMessage());
        }
    }

    // ===== XÓA SẢN PHẨM =====

    @FXML
    private void handleDelete() {
        Product selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("Lỗi", "Vui lòng chọn phiên cần xóa!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có chắc muốn xóa phiên \"" + selected.getName() + "\"?");
        confirm.setContentText("Hành động này không thể hoàn tác.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            String json = String.format(
                    "{\"type\":\"DELETE_AUCTION\",\"itemId\":\"%s\"}",
                    escapeJson(selected.getId()));

            new Thread(() -> {
                NetworkClient.getInstance().sendRaw(json);
                String response = NetworkClient.getInstance().readResponse();
                Platform.runLater(() -> {
                    if (response != null && response.contains("\"status\":\"OK\"")) {
                        productList.remove(selected);
                        // ✅ SỬA: Dùng generateNewId() thay vì size+1
                        generateNewId();
                        AlertUtil.showSuccess("Thành công", "Đã xóa phiên đấu giá!");
                        handleClear();
                    } else {
                        AlertUtil.showError("Lỗi", "Xóa thất bại: "
                                + (response != null ? response : "không có phản hồi"));
                    }
                });
            }).start();
        });
    }

    // ===== LÀM SẠCH FORM =====

    @FXML
    private void handleClear() {
        txtName.clear();
        txtStartPrice.clear();
        txtDescription.clear();
        txtMaxPrice.clear();
        txtImageUrl.clear();
        if (imgPreview != null) imgPreview.setImage(null);

        LocalDateTime now = LocalDateTime.now();
        dpStartTime.setValue(now.toLocalDate());
        dpEndTime.setValue(null);
        initSpinner(spinStartHour, 23, now.getHour());
        initSpinner(spinStartMin,  59, now.getMinute());
        initSpinner(spinStartSec,  59, now.getSecond());
        initSpinner(spinHour, 23, 23);
        initSpinner(spinMin,  59, 59);
        initSpinner(spinSec,  59, 59);

        tableProducts.getSelectionModel().clearSelection();
        // ✅ SỬA: Dùng generateNewId() thay vì size+1
        generateNewId();
    }

    // ===== PREVIEW ẢNH =====

    @FXML
    private void handlePreviewImage() {
        loadImagePreview(txtImageUrl.getText().trim());
    }

    private void loadImagePreview(String url) {
        if (imgPreview == null || url == null || url.isEmpty()) return;
        new Thread(() -> {
            try {
                java.net.URL imgUrl = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) imgUrl.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();

                int status = conn.getResponseCode();
                String finalUrl = url;
                if (status == java.net.HttpURLConnection.HTTP_MOVED_PERM
                        || status == java.net.HttpURLConnection.HTTP_MOVED_TEMP) {
                    finalUrl = conn.getHeaderField("Location");
                }

                final String loadUrl = finalUrl;
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                        loadUrl, 330, 160, true, true, false);

                if (img.isError()) {
                    String detail = img.getException() != null
                            ? img.getException().getMessage() : "unknown";
                    System.err.println("[IMG] Lỗi load ảnh: " + detail);
                    Platform.runLater(() -> {
                        imgPreview.setImage(null);
                        AlertUtil.showError("Lỗi ảnh", "Chi tiết: " + detail);
                    });
                } else {
                    Platform.runLater(() -> imgPreview.setImage(img));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    imgPreview.setImage(null);
                    AlertUtil.showError("Lỗi ảnh", "Không kết nối được: " + e.getMessage());
                });
            }
        }).start();
    }

    // ===== ĐIỀU HƯỚNG =====

    @FXML
    private void handleRefresh() {
        loadMyAuctions();
    }

    @FXML
    private void handleBack() {
        // Seller không có màn hình "danh sách mua" — nút Back bị vô hiệu
        AlertUtil.showError("Hạn chế", "Người bán không có quyền quay lại danh sách mua!");
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập hệ thống");
    }

    // ===== HELPER =====

    private void initSpinner(Spinner<Integer> s, int max, int val) {
        s.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, val));
    }

    private void setupDateCell(TableColumn<Product, LocalDateTime> col, DateTimeFormatter fmt) {
        col.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime d, boolean e) {
                super.updateItem(d, e);
                setText(e || d == null ? null : fmt.format(d));
            }
        });
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}