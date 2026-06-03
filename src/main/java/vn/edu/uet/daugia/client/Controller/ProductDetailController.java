package vn.edu.uet.daugia.client.Controller;

import vn.edu.uet.daugia.client.model.Product;
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
        // LUỒNG ĐI TIẾP: Sang Trang 3 (Phòng đấu giá)
        BiddingRoomController controller = SceneManager.switchSceneAndGetController(
                "/view/BiddingRoom.fxml", "Trang 3: Đấu giá: " + product.getName()
        );
        if (controller != null) {
            controller.setProductData(product);
        }
    }

    @FXML
    private void handleBack() {
        // LUỒNG QUAY LẠI: Về Trang 1 (Danh sách sản phẩm)
        SceneManager.switchScene("/view/AuctionList.fxml", "Danh sách sản phẩm");
    }
}