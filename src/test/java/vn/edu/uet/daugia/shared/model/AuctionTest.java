package vn.edu.uet.daugia.shared.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import vn.edu.uet.daugia.shared.model.user.*;
import vn.edu.uet.daugia.shared.model.item.*;
import vn.edu.uet.daugia.shared.exception.*;
import java.time.LocalDateTime;

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
}