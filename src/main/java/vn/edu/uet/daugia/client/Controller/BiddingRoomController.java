package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.BidHistoryRow;
import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.AlertUtil;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.client.util.SessionManager;
import vn.edu.uet.daugia.shared.model.BidMessage;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BiddingRoomController {

    @FXML private Label lblName, lblId, lblSession, lblDescription, lblCurrentPrice, lblTimer;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private ImageView productImageView;
    @FXML private Label lblStartPrice, lblMaxPrice;

    @FXML private TableView<BidHistoryRow> tblBidHistory;
    @FXML private TableColumn<BidHistoryRow, String> colBidder;
    @FXML private TableColumn<BidHistoryRow, String> colBidTime;
    @FXML private TableColumn<BidHistoryRow, String> colBidPrice;

    private XYChart.Series<String, Number> priceSeries;
    private Product currentProduct;
    private Timeline timeline;
    private final ObservableList<BidHistoryRow> bidHistoryRows = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_CHART = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_TABLE = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    @FXML
    public void initialize() {
        colBidder.setCellValueFactory(c -> c.getValue().bidderProperty());
        colBidTime.setCellValueFactory(c -> c.getValue().bidTimeProperty());
        colBidPrice.setCellValueFactory(c -> c.getValue().priceTextProperty());
        tblBidHistory.setItems(bidHistoryRows);
        tblBidHistory.setPlaceholder(new Label("Chưa có lượt đặt giá"));
    }

    public void setAuctionData(Product product) {
        this.currentProduct = product;
        setupUI();
    }

    public void setProductData(Product product) {
        this.currentProduct = product;
        setupUI();
    }

    private void setupUI() {
        if (currentProduct == null) {
            System.err.println("❌ Lỗi: Dữ liệu sản phẩm truyền vào phòng bị NULL!");
            return;
        }

        lblName.setText(currentProduct.getName());
        lblId.setText("Mã SP: " + currentProduct.getId());
        lblSession.setText("Phiên: " + currentProduct.getSession());
        lblDescription.setText("Mô tả: " + currentProduct.getDescription());
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentProduct.getCurrentPrice()));
        lblStartPrice.setText(String.format("%,.0f VNĐ", currentProduct.getStartPrice()));

        if (currentProduct.getMaxPrice() > 0) {
            lblMaxPrice.setText(String.format("%,.0f VNĐ", currentProduct.getMaxPrice()));
        } else {
            lblMaxPrice.setText("Không giới hạn");
        }

        loadProductImage(currentProduct.getImageUrl());

        resetChartAndTable();
        if (timeline != null) timeline.stop();
        startCountdown();

        sendSubscribe();
        registerSocketListener();
        syncRoomFromServer();
    }

    /** Gọi server tuần tự — tránh lẫn phản hồi trong hàng đợi socket. */
    private void syncRoomFromServer() {
        if (currentProduct == null) return;
        new Thread(() -> {
            fetchAuctionStateFromServerBlocking();
            fetchBidHistoryFromServerBlocking();
        }, "SyncBiddingRoom").start();
    }

    private void resetChartAndTable() {
        bidHistoryRows.clear();
        priceChart.getData().clear();
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Diễn biến giá");
        priceChart.getData().add(priceSeries);
    }

    private void loadProductImage(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return;
        try {
            if (rawUrl.contains("drive.google.com/file/d/")) {
                String id = rawUrl.replaceAll(".*drive\\.google\\.com/file/d/([^/]+).*", "$1");
                rawUrl = "https://drive.google.com/uc?export=download&id=" + id;
            }
            Image image = new Image(rawUrl, true);
            productImageView.setImage(image);
        } catch (Exception e) {
            System.err.println("Không load được ảnh sản phẩm: " + e.getMessage());
        }
    }

    private double getEffectiveCurrentPrice() {
        if (currentProduct == null) return 0;
        return Math.max(currentProduct.getStartPrice(), currentProduct.getCurrentPrice());
    }

    /** Nạp toàn bộ lịch sử từ DB — biểu đồ vẽ lại từ đầu phiên. */
    private void rebuildFromHistory(double startPrice, List<BidHistoryRow> rows) {
        resetChartAndTable();

        String startLabel = "Khởi điểm";
        if (currentProduct.getStartTime() != null) {
            startLabel = currentProduct.getStartTime().format(TIME_CHART);
        }
        priceSeries.getData().add(new XYChart.Data<>(startLabel, startPrice));

        double latest = startPrice;
        for (BidHistoryRow row : rows) {
            String chartTime = toChartTimeLabel(row.bidTimeProperty().get());
            priceSeries.getData().add(new XYChart.Data<>(chartTime, row.getPrice()));
            latest = row.getPrice();
        }

        List<BidHistoryRow> tableOrder = new ArrayList<>(rows);
        Collections.reverse(tableOrder);
        bidHistoryRows.addAll(tableOrder);

        currentProduct.setCurrentPrice(latest);
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", latest));
    }

    private void appendBidEntry(String bidder, String bidTimeIso, double price) {
        if (!bidHistoryRows.isEmpty()) {
            BidHistoryRow latest = bidHistoryRows.get(0);
            if (latest.getPrice() == price
                    && latest.bidderProperty().get().equals(bidder)) {
                currentProduct.setCurrentPrice(price);
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", price));
                return;
            }
        }

        String tableTime = formatTableTime(bidTimeIso);
        String chartTime = toChartTimeLabel(tableTime);

        BidHistoryRow row = new BidHistoryRow(bidder, tableTime, price);
        bidHistoryRows.add(0, row);

        boolean existsOnChart = priceSeries.getData().stream()
                .anyMatch(d -> d.getYValue().doubleValue() == price
                        && chartTime.equals(d.getXValue()));
        if (!existsOnChart) {
            priceSeries.getData().add(new XYChart.Data<>(chartTime, price));
        }

        currentProduct.setCurrentPrice(price);
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", price));
    }

    private void applyPriceUpdate(double newPrice, String leader, String bidTimeIso) {
        if (currentProduct == null) return;
        String bidder = (leader != null && !leader.isEmpty()) ? leader : "?";
        String time = (bidTimeIso != null && !bidTimeIso.isEmpty())
                ? bidTimeIso
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        appendBidEntry(bidder, time, newPrice);
    }

    private void fetchAuctionStateFromServerBlocking() {
        try {
            String req = String.format(
                    "{\"type\":\"GET_AUCTION_STATE\",\"auctionId\":\"%s\"}",
                    currentProduct.getId());
            NetworkClient.getInstance().sendRaw(req);
            String response = NetworkClient.getInstance().readResponse();
            if (response == null) return;

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (!"OK".equals(json.get("status").getAsString())) return;

            double currentPrice = json.get("currentPrice").getAsDouble();
            Platform.runLater(() -> {
                currentProduct.setCurrentPrice(currentPrice);
                lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentPrice));
            });
        } catch (Exception e) {
            System.err.println("Lỗi đồng bộ giá phiên: " + e.getMessage());
        }
    }

    private void fetchBidHistoryFromServerBlocking() {
        try {
            String req = String.format(
                    "{\"type\":\"GET_BID_HISTORY\",\"auctionId\":\"%s\"}",
                    currentProduct.getId());
            NetworkClient.getInstance().sendRaw(req);
            String response = NetworkClient.getInstance().readResponse();
            if (response == null) return;

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (!"OK".equals(json.get("status").getAsString())) return;

            double startPrice = json.has("startPrice")
                    ? json.get("startPrice").getAsDouble()
                    : currentProduct.getStartPrice();

            List<BidHistoryRow> rows = new ArrayList<>();
            if (json.has("history") && json.get("history").isJsonArray()) {
                JsonArray arr = json.getAsJsonArray("history");
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject item = arr.get(i).getAsJsonObject();
                    String bidder = item.get("bidderId").getAsString();
                    double price = item.get("price").getAsDouble();
                    String bidTime = item.get("bidTime").getAsString();
                    rows.add(new BidHistoryRow(bidder, formatTableTime(bidTime), price));
                }
            }

            Platform.runLater(() -> rebuildFromHistory(startPrice, rows));
        } catch (Exception e) {
            System.err.println("Lỗi tải lịch sử đặt giá: " + e.getMessage());
        }
    }

    private void registerSocketListener() {
        NetworkClient.getInstance().setPushListener((type, json) -> {
            if ("NEW_BID".equals(type)) {
                try {
                    JsonObject data = json.getAsJsonObject("data");
                    String itemId = data.has("itemId")
                            ? data.get("itemId").getAsString()
                            : data.get("id").getAsString();

                    if (currentProduct == null || !itemId.equals(currentProduct.getId())) return;

                    double newPrice = data.get("currentPrice").getAsDouble();
                    String leader = data.has("lastBidder")
                            ? data.get("lastBidder").getAsString()
                            : data.has("currentLeader")
                            ? data.get("currentLeader").getAsString()
                            : data.get("leader").getAsString();
                    String bidTime = data.has("bidTime") ? data.get("bidTime").getAsString() : "";

                    Platform.runLater(() -> applyPriceUpdate(newPrice, leader, bidTime));
                } catch (Exception e) {
                    System.err.println("Lỗi xử lý NEW_BID: " + e.getMessage());
                }
            }
        });
    }

    private void startCountdown() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (currentProduct == null || currentProduct.getEndTime() == null) return;

            java.time.Duration diff = java.time.Duration.between(LocalDateTime.now(), currentProduct.getEndTime());

            if (diff.isNegative() || diff.isZero()) {
                lblTimer.setText("KẾT THÚC");
                btnBid.setDisable(true);
                txtBidAmount.setDisable(true);
                timeline.stop();
                AlertUtil.showSuccess("Kết thúc", "Phiên đấu giá đã chính thức khép lại!");
            } else {
                long hours = diff.toHours();
                int minutes = diff.toMinutesPart();
                int seconds = diff.toSecondsPart();
                lblTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handleBid() {
        if (currentProduct == null) return;

        try {
            String input = txtBidAmount.getText().trim();
            if (input.isEmpty()) return;

            double bidPrice = Double.parseDouble(input);
            double minPrice = getEffectiveCurrentPrice();
            if (bidPrice <= minPrice) {
                AlertUtil.showError("Lỗi",
                        String.format("Giá đặt phải cao hơn giá hiện tại (%,.0f VNĐ)!", minPrice));
                return;
            }

            if (currentProduct.getMaxPrice() > 0 && bidPrice > currentProduct.getMaxPrice()) {
                AlertUtil.showError("Lỗi",
                        String.format("Giá đặt không được vượt giá mua đứt (%,.0f VNĐ)!",
                                currentProduct.getMaxPrice()));
                return;
            }

            String bidderId = SessionManager.getUsername();
            final double finalBidPrice = bidPrice;

            new Thread(() -> {
                try {
                    BidMessage bidMsg = new BidMessage("BID", currentProduct.getId(), bidderId, finalBidPrice);
                    NetworkClient.getInstance().sendBidMessage(bidMsg);
                    String response = NetworkClient.getInstance().readResponse();

                    Platform.runLater(() -> {
                        if (response == null) {
                            AlertUtil.showError("Lỗi", "Không nhận được phản hồi từ máy chủ!");
                            return;
                        }
                        try {
                            JsonObject resp = JsonParser.parseString(response).getAsJsonObject();
                            if ("OK".equals(resp.get("status").getAsString())) {
                                double newPrice = resp.get("currentPrice").getAsDouble();
                                String leader = resp.has("leader") ? resp.get("leader").getAsString() : bidderId;
                                String bidTime = LocalDateTime.now()
                                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                applyPriceUpdate(newPrice, leader, bidTime);
                                txtBidAmount.clear();
                            } else {
                                String msg = resp.has("message")
                                        ? resp.get("message").getAsString()
                                        : "Đặt giá không thành công!";
                                AlertUtil.showError("Không hợp lệ", msg);
                            }
                        } catch (Exception ex) {
                            AlertUtil.showError("Lỗi", "Phản hồi máy chủ không hợp lệ!");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() ->
                            AlertUtil.showError("Lỗi hệ thống", "Không thể gửi dữ liệu đấu giá tới máy chủ!"));
                }
            }).start();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Vui lòng nhập một số tiền hợp lệ!");
        }
    }

    @FXML
    private void handleBackToList() {
        if (timeline != null) timeline.stop();
        sendUnsubscribe();
        NetworkClient.getInstance().clearPushListener();
        SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
    }

    @FXML
    private void handleBackToDetail() {
        if (timeline != null) timeline.stop();
        sendUnsubscribe();
        NetworkClient.getInstance().clearPushListener();
        ProductDetailController controller = SceneManager.switchSceneAndGetController(
                "/view/ProductDetail.fxml", "Chi tiết sản phẩm");
        if (controller != null) controller.setProductData(this.currentProduct);
    }

    private void sendSubscribe() {
        if (currentProduct == null) return;
        new Thread(() -> {
            try {
                String json = String.format(
                        "{\"type\":\"SUBSCRIBE_AUCTION\",\"roomId\":\"%s\",\"username\":\"%s\"}",
                        currentProduct.getId(), SessionManager.getUsername());
                NetworkClient.getInstance().sendRaw(json);
            } catch (Exception e) {
                System.err.println("Lỗi gửi lệnh SUBSCRIBE: " + e.getMessage());
            }
        }).start();
    }

    private void sendUnsubscribe() {
        if (currentProduct == null) return;
        new Thread(() -> {
            try {
                String json = String.format(
                        "{\"type\":\"UNSUBSCRIBE_AUCTION\",\"roomId\":\"%s\",\"username\":\"%s\"}",
                        currentProduct.getId(), SessionManager.getUsername());
                NetworkClient.getInstance().sendRaw(json);
            } catch (Exception e) {
                System.err.println("Lỗi gửi lệnh UNSUBSCRIBE: " + e.getMessage());
            }
        }).start();
    }

    private String formatTableTime(String isoOrRaw) {
        if (isoOrRaw == null || isoOrRaw.isEmpty()) {
            return LocalDateTime.now().format(TIME_TABLE);
        }
        try {
            return LocalDateTime.parse(isoOrRaw).format(TIME_TABLE);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(isoOrRaw.replace(' ', 'T')).format(TIME_TABLE);
            } catch (Exception ex) {
                return isoOrRaw;
            }
        }
    }

    private String toChartTimeLabel(String tableTime) {
        if (tableTime == null) return LocalDateTime.now().format(TIME_CHART);
        if (tableTime.length() >= 8) {
            return tableTime.substring(tableTime.length() - 8);
        }
        return tableTime;
    }
}
