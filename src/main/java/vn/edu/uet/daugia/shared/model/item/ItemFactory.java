package vn.edu.uet.daugia.shared.model.item;

import java.time.LocalDateTime;

/**
 * Factory Method Pattern – interface Creator.
 *
 * Mỗi loại sản phẩm (Electronics, Art, Vehicle) sẽ có một
 * ConcreteFactory tương ứng implement interface này.
 *
 * Lợi ích:
 *  - Tách logic khởi tạo ra khỏi nơi sử dụng (ClientHandler, AuctionService…)
 *  - Dễ dàng thêm loại Item mới mà không sửa code cũ (Open/Closed Principle)
 */
public interface ItemFactory {

    /**
     * Factory Method – tạo một Item từ các tham số chung.
     *
     * @param id           Mã định danh sản phẩm
     * @param name         Tên sản phẩm
     * @param description  Mô tả sản phẩm
     * @param startingPrice Giá khởi điểm
     * @param startTime    Thời điểm bắt đầu phiên
     * @param endTime      Thời điểm kết thúc phiên
     * @param extra        Thông tin đặc thù của từng loại (brand, artist, biển số…)
     *                     Truyền null nếu không có.
     * @return             Item tương ứng đã được khởi tạo
     */
    Item createItem(String id, String name, String description,
                    double startingPrice,
                    LocalDateTime startTime, LocalDateTime endTime,
                    String extra);
}
