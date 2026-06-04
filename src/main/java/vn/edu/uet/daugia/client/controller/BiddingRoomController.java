package vn.edu.uet.daugia.client.controller;

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

    // =========================
    // FXML FIELDS
    // =========================

    @FXML private Label lblName, lblId, lblSession, lblDescription, lblCurrentPrice, lblTimer;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private ImageView productImageView;
    @FXML private Label lblStartPrice, lblMaxPrice;

    @FXML private TableView<BidHistoryRow>           tblBidHistory;
    @FXML private TableColumn<BidHistoryRow, String> colBidder;
    @FXML private TableColumn<BidHistoryRow, String> colBidTime;
    @FXML private TableColumn<BidHistoryRow, String> colBidPrice;

    // =========================
    // STATE
    // =========================

    private XYChart.Series<String, Number>       priceSeries;
    private Product                              currentProduct;
    private Timeline                             timeline;
    private final ObservableList<BidHistoryRow>  bidHistoryRows = FXCollections.observableArrayList();
    private volatile boolean                     auctionEnded   = false;

    // =========================
    // FORMATTERS
    // =========================

    private static final DateTimeFormatter TIME_CHART = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_TABLE = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    // =========================
    // INITIALIZE
    // =========================

    @FXML
    public void initialize() {
        colBidder.setCellValueFactory(c -> c.getValue().bidderProperty());
        colBidTime.setCellValueFactory(c -> c.getValue().bidTimeProperty());
        colBidPrice.setCellValueFactory(c -> c.getValue().priceTextProperty());
        tblBidHistory.setItems(bidHistoryRows);
        tblBidHistory.setPlaceholder(new Label("Chưa có lượt đặt giá"));
    }

    // =========================
    // DATA ENTRY POINTS
    // =========================

    public void setAuctionData(Product product) {
        this.currentProduct = product;
        this.auctionEnded   = false;
        setupUI();
    }

    public void setProductData(Product product) {
        this.currentProduct = product;
        this.auctionEnded   = false;
        setupUI();
    }

    // =========================
    // UI SETUP
    // =========================

    private void setupUI() {
        if (currentProduct == null) {
            System.err.println("Lỗi: Dữ liệu sản phẩm NULL!");
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

        registerSocketListener();
        sendSubscribe();
        syncRoomFromServer();
    }

    // =========================
    // CHART & TABLE
    // =========================

    private void resetChartAndTable() {
        bidHistoryRows.clear();
        priceChart.getData().clear();
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Diễn biến giá");
        priceChart.getData().add(priceSeries);
    }

    private void rebuildFromHistory(double startPrice, List<BidHistoryRow> rows) {
        resetChartAndTable();

        String startLabel = (currentProduct.getStartTime() != null)
                ? currentProduct.getStartTime().format(TIME_CHART) : "Khởi điểm";
        priceSeries.getData().add(new XYChart.Data<>(startLabel, startPrice));

        double latest = startPrice;
        for (BidHistoryRow row : rows) {
            addChartPoint(toChartTimeLabel(row.bidTimeProperty().get()), row.getPrice());
            latest = row.getPrice();
        }

        List<BidHistoryRow> tableOrder = new ArrayList<>(rows);
        Collections.reverse(tableOrder);
        bidHistoryRows.addAll(tableOrder);

        currentProduct.setCurrentPrice(latest);
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", latest));
    }

    private void addChartPoint(String chartTime, double price) {
        boolean exists = priceSeries.getData().stream()
                .anyMatch(d -> chartTime.equals(d.getXValue()));
        if (!exists) {
            priceSeries.getData().add(new XYChart.Data<>(chartTime, price));
        }
    }

    private void appendBidEntry(String bidder, String bidTimeIso, double price) {
        String tableTime = formatTableTime(bidTimeIso);
        String chartTime = toChartTimeLabel(tableTime);

        if (!bidHistoryRows.isEmpty()) {
            BidHistoryRow top = bidHistoryRows.get(0);
            if (top.getPrice() == price
                    && top.bidderProperty().get().equals(bidder)
                    && top.bidTimeProperty().get().equals(tableTime)) {
                return;
            }
        }

        bidHistoryRows.add(0, new BidHistoryRow(bidder, tableTime, price));
        addChartPoint(chartTime, price);
        currentProduct.setCurrentPrice(price);
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", price));
    }

    // =========================
    // IMAGE LOADING (chỉ link online)
    // =========================

    private void loadProductImage(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return;
        new Thread(() -> {
            try {
                String url = convertImageUrl(rawUrl.trim());
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.connect();
                try (java.io.InputStream is = conn.getInputStream()) {
                    Image img = new Image(is);
                    if (!img.isError()) {
                        Platform.runLater(() -> productImageView.setImage(img));
                    }
                }
            } catch (Exception e) {
                System.err.println("Không load được ảnh: " + e.getMessage());
            }
        }).start();
    }

    static String convertImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return rawUrl;
        rawUrl = rawUrl.trim();
        if (rawUrl.contains("i.imgur.com")) return rawUrl;
        if (rawUrl.matches("https?://imgur\\.com/[a-zA-Z0-9]+")) {
            String id = rawUrl.replaceAll(".*/([a-zA-Z0-9]+)$", "$1");
            return "https://i.imgur.com/" + id + ".jpg";
        }
        if (rawUrl.contains("drive.google.com/file/d/")) {
            String id = rawUrl.replaceAll(".*drive\\.google\\.com/file/d/([^/?&]+).*", "$1");
            if (!id.equals(rawUrl))
                return "https://drive.google.com/thumbnail?id=" + id + "&sz=w400";
        }
        if (rawUrl.contains("drive.google.com") && rawUrl.contains("id=")) {
            String id = rawUrl.replaceAll(".*[?&]id=([^&]+).*", "$1");
            if (!id.equals(rawUrl))
                return "https://drive.google.com/thumbnail?id=" + id + "&sz=w400";
        }
        return rawUrl;
    }

    // =========================
    // SERVER SYNC
    // =========================

    private void syncRoomFromServer() {
        if (currentProduct == null) return;
        new Thread(() -> {
            fetchAuctionStateFromServerBlocking();
            fetchBidHistoryFromServerBlocking();
        }, "SyncBiddingRoom").start();
    }

    private void fetchAuctionStateFromServerBlocking() {
        try {
            NetworkClient.getInstance().sendRaw(String.format(
                    "{\"type\":\"GET_AUCTION_STATE\",\"auctionId\":\"%s\"}",
                    currentProduct.getId()));
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
            System.err.println("Lỗi đồng bộ giá: " + e.getMessage());
        }
    }

    private void fetchBidHistoryFromServerBlocking() {
        try {
            NetworkClient.getInstance().sendRaw(String.format(
                    "{\"type\":\"GET_BID_HISTORY\",\"auctionId\":\"%s\"}",
                    currentProduct.getId()));
            String response = NetworkClient.getInstance().readResponse();
            if (response == null) return;

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (!"OK".equals(json.get("status").getAsString())) return;

            double startPrice = json.has("startPrice")
                    ? json.get("startPrice").getAsDouble() : currentProduct.getStartPrice();

            List<BidHistoryRow> rows = new ArrayList<>();
            if (json.has("history") && json.get("history").isJsonArray()) {
                JsonArray arr = json.getAsJsonArray("history");
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject item = arr.get(i).getAsJsonObject();
                    rows.add(new BidHistoryRow(
                            item.get("bidderId").getAsString(),
                            formatTableTime(item.get("bidTime").getAsString()),
                            item.get("price").getAsDouble()));
                }
            }
            Platform.runLater(() -> rebuildFromHistory(startPrice, rows));
        } catch (Exception e) {
            System.err.println("Lỗi tải lịch sử: " + e.getMessage());
        }
    }

    // =========================
    // SOCKET LISTENER
    // =========================

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
                    String leader   = data.has("lastBidder")    ? data.get("lastBidder").getAsString()
                            : data.has("currentLeader") ? data.get("currentLeader").getAsString()
                            : data.get("leader").getAsString();
                    String bidTime  = data.has("bidTime") ? data.get("bidTime").getAsString() : "";

                    Platform.runLater(() -> appendBidEntry(leader, bidTime, newPrice));

                } catch (Exception e) {
                    System.err.println("Lỗi xử lý NEW_BID: " + e.getMessage());
                }

            } else if ("AUCTION_CLOSED".equals(type)) {
                try {
                    String auctionId = json.has("auctionId") ? json.get("auctionId").getAsString() : "";
                    if (currentProduct == null || !auctionId.equals(currentProduct.getId())) return;

                    String status     = json.has("status")     ? json.get("status").getAsString()     : "FINISHED";
                    String winner     = json.has("winner")     ? json.get("winner").getAsString()     : "";
                    double finalPrice = json.has("finalPrice") ? json.get("finalPrice").getAsDouble() : 0;

                    Platform.runLater(() -> handleAuctionEnded(status, winner, finalPrice, null));

                } catch (Exception e) {
                    System.err.println("Lỗi xử lý AUCTION_CLOSED: " + e.getMessage());
                }

            } else if ("AUCTION_EXTENDED".equals(type)) {
                // ===== ANTI-SNIPING: cập nhật đồng hồ đếm ngược =====
                try {
                    String auctionId = json.has("auctionId") ? json.get("auctionId").getAsString() : "";
                    if (currentProduct == null || !auctionId.equals(currentProduct.getId())) return;

                    String newEndTimeStr = json.get("newEndTime").getAsString();
                    LocalDateTime newEndTime = LocalDateTime.parse(newEndTimeStr);

                    Platform.runLater(() -> {
                        // Cập nhật endTime của product → đồng hồ tự đọc lại mỗi giây
                        currentProduct = new Product(
                                currentProduct.getId(),
                                currentProduct.getName(),
                                currentProduct.getStatus(),
                                currentProduct.getStartPrice(),
                                currentProduct.getCurrentPrice(),
                                currentProduct.getMaxPrice(),
                                currentProduct.getDescription(),
                                currentProduct.getImageUrl(),
                                currentProduct.getStartTime(),
                                newEndTime);
                        currentProduct.setLeader(currentProduct.getLeader());
                        AlertUtil.showSuccess("⏱ Phiên được gia hạn!",
                                "Có bid mới trong 60 giây cuối.\nPhiên được gia hạn thêm 60 giây!");
                    });

                } catch (Exception e) {
                    System.err.println("Lỗi xử lý AUCTION_EXTENDED: " + e.getMessage());
                }
            }
        });
    }

    // =========================
    // COUNTDOWN
    // =========================

    private void startCountdown() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (currentProduct == null || currentProduct.getEndTime() == null) return;

            java.time.Duration diff = java.time.Duration.between(
                    LocalDateTime.now(), currentProduct.getEndTime());

            if (diff.isNegative() || diff.isZero()) {
                lblTimer.setText("KẾT THÚC");
                btnBid.setDisable(true);
                txtBidAmount.setDisable(true);
                timeline.stop();
            } else {
                lblTimer.setText(String.format("%02d:%02d:%02d",
                        diff.toHours(), diff.toMinutesPart(), diff.toSecondsPart()));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // =========================
    // AUCTION ENDED
    // =========================

    private void handleAuctionEnded(String status, String winner, double finalPrice, String customMessage) {
        if (auctionEnded) return;
        auctionEnded = true;

        if (timeline != null) timeline.stop();
        lblTimer.setText("KẾT THÚC");
        btnBid.setDisable(true);
        txtBidAmount.setDisable(true);

        NetworkClient.getInstance().clearPushListener();

        currentProduct.setStatus(status);
        if (winner != null && !winner.isEmpty()) currentProduct.setLeader(winner);

        String  myUsername = SessionManager.getUsername();
        boolean iWon       = winner != null && winner.equals(myUsername);

        if ("FINISHED".equals(status)) {
            if (customMessage != null && !customMessage.isEmpty()) {
                AlertUtil.showSuccess("Chúc mừng! 🎉", customMessage);
            } else if (iWon) {
                AlertUtil.showSuccess("Chúc mừng! 🎉",
                        String.format("Bạn đã thắng phiên đấu giá với giá %,.0f VNĐ!", finalPrice));
            } else {
                String msg = (winner != null && !winner.isEmpty())
                        ? String.format("Phiên đã kết thúc.\nNgười thắng: %s (%,.0f VNĐ)", winner, finalPrice)
                        : "Phiên đấu giá đã kết thúc.";
                AlertUtil.showSuccess("Phiên đấu giá kết thúc", msg);
            }
        } else {
            AlertUtil.showSuccess("Phiên đấu giá kết thúc", "Phiên đã bị hủy (không có người đặt giá).");
        }
    }

    // =========================
    // HANDLE BID
    // =========================

    @FXML
    private void handleBid() {
        if (currentProduct == null || auctionEnded) {
            AlertUtil.showError("Không thể đặt giá", "Phiên đấu giá đã kết thúc!");
            return;
        }

        try {
            String input = txtBidAmount.getText().trim();
            if (input.isEmpty()) return;

            double bidPrice = Double.parseDouble(input.replace(",", ""));
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

            String       bidderId      = SessionManager.getUsername();
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
                            JsonObject resp       = JsonParser.parseString(response).getAsJsonObject();
                            String     respStatus = resp.get("status").getAsString();

                            if ("OK".equals(respStatus)) {
                                txtBidAmount.clear();

                            } else if ("BUYOUT".equals(respStatus)) {
                                txtBidAmount.clear();
                                double newPrice = resp.get("currentPrice").getAsDouble();
                                String msg = resp.has("message")
                                        ? resp.get("message").getAsString()
                                        : String.format("Chúc mừng! Bạn đã mua đứt với giá %,.0f VNĐ!", newPrice);
                                appendBidEntry(bidderId,
                                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                        newPrice);
                                handleAuctionEnded("FINISHED", bidderId, newPrice, msg);

                            } else {
                                String msg = resp.has("message")
                                        ? formatServerMessage(resp.get("message").getAsString())
                                        : "Đặt giá không thành công!";
                                AlertUtil.showError("Không hợp lệ", msg);
                            }

                        } catch (Exception ex) {
                            AlertUtil.showError("Lỗi", "Phản hồi máy chủ không hợp lệ: " + ex.getMessage());
                        }
                    });

                } catch (Exception e) {
                    Platform.runLater(() ->
                            AlertUtil.showError("Lỗi hệ thống", "Không thể gửi dữ liệu đấu giá!"));
                }
            }).start();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Vui lòng nhập một số tiền hợp lệ!");
        }
    }

    private String formatServerMessage(String msg) {
        if (msg == null) return "";
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "[0-9]+\\.?[0-9]*(?:[Ee][+\\-]?[0-9]+)?");
            java.util.regex.Matcher matcher = p.matcher(msg);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                try {
                    double val = Double.parseDouble(matcher.group());
                    matcher.appendReplacement(sb, String.format("%,.0f", val));
                } catch (Exception e) {
                    matcher.appendReplacement(sb, matcher.group());
                }
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            return msg;
        }
    }

    // =========================
    // NAVIGATION
    // =========================

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

    // =========================
    // SUBSCRIBE / UNSUBSCRIBE
    // =========================

    private void sendSubscribe() {
        if (currentProduct == null) return;
        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendRaw(String.format(
                        "{\"type\":\"SUBSCRIBE_AUCTION\",\"roomId\":\"%s\",\"username\":\"%s\"}",
                        currentProduct.getId(), SessionManager.getUsername()));
            } catch (Exception e) {
                System.err.println("Lỗi SUBSCRIBE: " + e.getMessage());
            }
        }).start();
    }

    private void sendUnsubscribe() {
        if (currentProduct == null) return;
        new Thread(() -> {
            try {
                NetworkClient.getInstance().sendRaw(String.format(
                        "{\"type\":\"UNSUBSCRIBE_AUCTION\",\"roomId\":\"%s\",\"username\":\"%s\"}",
                        currentProduct.getId(), SessionManager.getUsername()));
            } catch (Exception e) {
                System.err.println("Lỗi UNSUBSCRIBE: " + e.getMessage());
            }
        }).start();
    }

    // =========================
    // HELPERS
    // =========================

    private double getEffectiveCurrentPrice() {
        if (currentProduct == null) return 0;
        return Math.max(currentProduct.getStartPrice(), currentProduct.getCurrentPrice());
    }

    private String formatTableTime(String isoOrRaw) {
        if (isoOrRaw == null || isoOrRaw.isEmpty())
            return LocalDateTime.now().format(TIME_TABLE);
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
        return tableTime.length() >= 8 ? tableTime.substring(tableTime.length() - 8) : tableTime;
    }
}