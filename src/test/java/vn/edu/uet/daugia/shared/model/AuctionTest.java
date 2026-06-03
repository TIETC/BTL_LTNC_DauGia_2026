package vn.edu.uet.daugia.shared.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import vn.edu.uet.daugia.shared.model.user.*;
import vn.edu.uet.daugia.shared.model.item.*;
import vn.edu.uet.daugia.shared.exception.*;
import java.time.LocalDateTime;
import vn.edu.uet.daugia.server.AuctionService;
import vn.edu.uet.daugia.server.AuctionManager;
import vn.edu.uet.daugia.shared.model.AuctionStatus;

class AuctionTest {
    Auction auction;
    Bidder b1, b2;

    @BeforeEach
    void setup() {
        Seller seller = new Seller("seller", "s@mail.com", "123", "Shop");
        b1 = new Bidder("alice", "a@mail.com", "123", 1_000_000);
        b2 = new Bidder("bob", "b@mail.com", "123", 1_000_000);

        // Sử dụng đối tượng thực tế thay vì null
        Electronics laptop = new Electronics("SP01", "Laptop Gaming", "Mô tả", 500000,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(10), "Asus", 24);

        auction = new Auction(laptop, seller, 500_000,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(10));
        auction.startAuction();
    }

    @Test
    void validBidUpdatesPrice() {
        auction.placeBid(b1, 600_000);
        assertEquals(600_000, auction.getCurrentPrice());
        assertEquals(b1, auction.getCurrentLeader());
    }

    @Test
    void bidBelowCurrentPriceThrows() {
        auction.placeBid(b1, 600_000);
        assertThrows(InvalidBidException.class,
                () -> auction.placeBid(b2, 550_000));
    }

    @Test
    void bidOnClosedAuctionThrows() {
        auction.closeAuction();
        assertThrows(AuctionClosedException.class,
                () -> auction.placeBid(b1, 600_000));
    }

    @Test
    void concurrentBidsOnlyOneWins() throws InterruptedException {
        // Mô phỏng 5 người bấm đặt giá cùng 1 phần nghìn giây
        Thread[] threads = new Thread[5];
        double[] prices = {600_000, 650_000, 700_000, 680_000, 720_000};

        for (int i = 0; i < 5; i++) {
            final double p = prices[i];
            final Bidder b = new Bidder("u"+i, "", "", 1_000_000);
            threads[i] = new Thread(() -> {
                try { auction.placeBid(b, p); }
                catch (Exception ignored) {}
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        //Phải có đúng 1 người thắng và giá là 720.000đ
        assertNotNull(auction.getCurrentLeader());
        assertEquals(720_000, auction.getCurrentPrice(), 0.01);
    }
    // 3 TEST CASE MỚI BỔ SUN

    @Test
    @DisplayName("Đóng phiên đấu giá khi không có lượt bid -> CANCELED")
    void closeAuctionWithZeroBidsIsCanceled() {
        // Trong hàm setup() (@BeforeEach), auction đã được khởi tạo và startAuction()
        // nhưng chưa có bất kỳ lời gọi placeBid() nào.
        auction.closeAuction();

        // Kiểm tra trạng thái phải là CANCELED và không có ai chiến thắng
        assertEquals(AuctionStatus.CANCELED, auction.getStatus(), "Trạng thái phiên phải chuyển thành CANCELED");
        assertNull(auction.getWinner(), "Không được phép có winner khi không có bid nào");
    }

    @Test
    @DisplayName("Kiểm tra getWinner() trả về đúng Bidder sau khi phiên kết thúc thành công")
    void getWinnerReturnsCorrectBidderAfterClosing() {
        // Mô phỏng 2 bidder đặt giá
        auction.placeBid(b1, 600_000);
        auction.placeBid(b2, 650_000); // b2 đặt giá cao hơn

        // Đóng phiên
        auction.closeAuction();

        // Kiểm tra kết quả
        assertEquals(AuctionStatus.FINISHED, auction.getStatus(), "Trạng thái phải chuyển thành FINISHED");
        assertNotNull(auction.getWinner(), "Phải xác định được người chiến thắng");
        assertEquals(b2, auction.getWinner(), "Người chiến thắng phải là Bob (b2) do đặt giá cao nhất");
        assertEquals(650_000, auction.getCurrentPrice(), "Giá chốt phiên phải khớp với mức đặt cao nhất");
    }

    @Test
    @DisplayName("Kiểm tra AuctionService.handleGetAuctions() trả về định dạng JSON đúng")
    void handleGetAuctionsReturnsCorrectJson() {
        // 1. Chuẩn bị dữ liệu: Đưa auction đang chạy vào Manager
        AuctionManager manager = AuctionManager.getInstance();
        manager.addAuction(auction); // auction này đã ở trạng thái RUNNING từ setup()

        // 2. Khởi tạo Service và gọi hàm
        AuctionService service = new AuctionService();
        String jsonResult = service.handleGetAuctions();

        // 3. Kiểm tra tính hợp lệ của mảng JSON
        assertTrue(jsonResult.startsWith("["), "Đầu ra phải bắt đầu bằng ký tự mảng JSON '['");
        assertTrue(jsonResult.endsWith("]"), "Đầu ra phải kết thúc bằng ký tự mảng JSON ']'");

        // 4. Kiểm tra các thông tin cốt lõi có được toJson() xử lý đúng không
        assertTrue(jsonResult.contains(auction.getId()), "JSON phải chứa ID của phiên đấu giá");
        assertTrue(jsonResult.contains("\"itemName\":\"Laptop Gaming\""), "JSON phải chứa tên thiết bị");
        assertTrue(jsonResult.contains("\"status\":\"RUNNING\""), "JSON phải hiển thị đúng trạng thái RUNNING");

        // 5. Dọn dẹp dữ liệu trong Singleton để không làm ảnh hưởng các test khác chạy sau nó
        manager.removeAuction(auction.getId());
    }
}