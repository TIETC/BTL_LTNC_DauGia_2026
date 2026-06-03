package vn.edu.uet.daugia.server;

import vn.edu.uet.daugia.shared.model.Auction;

public interface AuctionObserver {
    // Hàm sẽ tự động gọi khi có một giá mới đặt thành công
    void onNewBid(Auction auction);
}