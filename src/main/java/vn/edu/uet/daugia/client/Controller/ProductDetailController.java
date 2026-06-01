package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class ProductDetailController {

    @FXML private Label lblName, lblId, lblSession, lblStartPrice;
    @FXML private TextArea txtDescription;
    private Product product;

    public void setProductData(Product product) {
        this.product = product;
        lblName.setText(product.getName());
        lblId.setText(product.getId());
        lblSession.setText(product.getSession());
        lblStartPrice.setText(String.format("%,.0f VNĐ", product.getStartPrice()));
        txtDescription.setText(product.getDescription());
    }

    @FXML
    private void handleJoinAuction() {
        // Sang Phòng đấu giá
        BiddingRoomController controller = SceneManager.switchSceneAndGetController(
                "/view/BiddingRoom.fxml", "Phòng đấu giá: " + product.getName()
        );
        if (controller != null) {
            controller.setProductData(product);
        }
    }

    @FXML
    private void handleBack() {
        // FIX QUAN TRỌNG: clearPushListener trước khi rời màn hình.
        //
        // Tại sao cần làm vậy?
        // Khi từ AuctionList → ProductDetail (qua handleDetail ở ProductCardController),
        // push listener của AuctionListController vẫn còn đăng ký.
        // Nếu không clear ở đây, khi AuctionList khởi tạo lại (initialize()),
        // nó sẽ gọi registerPushListener() → đăng ký listener MỚI.
        // Nhưng vì NetworkClient.setPushListener() chỉ lưu 1 listener,
        // listener cũ bị đè → không gây vấn đề về sản phẩm biến mất.
        //
        // Vấn đề thực sự: nếu trước đó người dùng đã vào BiddingRoom rồi back,
        // BiddingRoom đã clearPushListener() → AuctionList.initialize() chạy lại
        // và loadAuctionsFromServer() KHÔNG chạy vì productCache != empty.
        // Đây là flow đúng.
        //
        // Tuy nhiên nếu có race condition: clear ở đây đảm bảo listener sạch
        // trước khi AuctionListController.initialize() đăng ký lại.
        NetworkClient.getInstance().clearPushListener();

        SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
    }
}
