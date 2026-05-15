package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;

public class AuctionListController {

    @FXML private TableView<Product> tableView;
    @FXML private TableColumn<Product, String> colId, colName, colSession;
    @FXML private TableColumn<Product, Double> colStartPrice;
    @FXML private Button btnSellerMode;

    private ObservableList<Product> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        // Ẩn nút Seller nếu là Bidder
        if ("BIDDER".equals(SessionManager.getRole())) {
            if (btnSellerMode != null) {
                btnSellerMode.setVisible(false);
                btnSellerMode.setManaged(false);
            }
        }

        // Cài đặt cột
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

        // Click đúp → vào BiddingRoom
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                Product selected = tableView.getSelectionModel().getSelectedItem();

                // Xóa listener trước khi rời màn hình
                NetworkClient.getInstance().clearPushListener();

                BiddingRoomController controller = SceneManager.switchSceneAndGetController(
                        "/view/BiddingRoom.fxml", "Phòng đấu giá: " + selected.getName());
                if (controller != null) {
                    controller.setAuctionData(selected); // truyền sản phẩm sang BiddingRoom
                }
            }
        });

        // Load danh sách auction từ Server
        loadAuctionsFromServer();

        // Đăng ký nhận push NEW_AUCTION từ Server
        // Thay vì thread riêng, dùng PushListener của NetworkClient
        NetworkClient.getInstance().setPushListener((type, json) -> {
            if ("NEW_AUCTION".equals(type)) {
                String id        = json.get("itemId").getAsString();
                String name      = json.get("itemName").getAsString();
                double price     = json.get("startPrice").getAsDouble();
                String endTimeStr = json.get("endTime").getAsString();

                Product p = new Product(id, name, "Đang diễn ra",
                        price, price, "", LocalDateTime.now(),
                        LocalDateTime.parse(endTimeStr));

                Platform.runLater(() -> {
                    data.add(p);
                    System.out.println("✅ Auction mới xuất hiện: " + name);
                });
            }
        });
    }

    private void loadAuctionsFromServer() {
        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendRaw("{\"type\":\"GET_AUCTIONS\"}");
                String response = NetworkClient.getInstance().readResponse();
                System.out.println("GET_AUCTIONS phản hồi: " + response);

                if (response == null || response.equals("[]")) return;

                Gson gson = new Gson();
                JsonObject[] auctions = gson.fromJson(response, JsonObject[].class);

                Platform.runLater(() -> {
                    data.clear();
                    for (JsonObject a : auctions) {
                        String id        = a.get("itemId").getAsString();
                        String name      = a.get("itemName").getAsString();
                        double price     = a.get("startPrice").getAsDouble();
                        String endTimeStr = a.get("endTime").getAsString();

                        Product p = new Product(id, name, "Đang diễn ra",
                                price, price, "", LocalDateTime.now(),
                                LocalDateTime.parse(endTimeStr));
                        data.add(p);
                    }
                    System.out.println("Đã load " + data.size() + " phiên đấu giá");
                });

            } catch (Exception e) {
                System.err.println("Lỗi load danh sách auction: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    protected void handleSwitchToSeller() {
        NetworkClient.getInstance().clearPushListener();
        SceneManager.switchScene("/view/SellerDashboard.fxml", "Quản lý sản phẩm");
    }

    @FXML
    protected void handleLogout() {
        NetworkClient.getInstance().clearPushListener();
        SessionManager.logout();
        SceneManager.switchScene("/view/Login.fxml", "Đăng nhập");
    }
}