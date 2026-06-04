package vn.edu.uet.daugia.shared.model.item;

/**
 * Enum các loại sản phẩm được hỗ trợ trong hệ thống đấu giá.
 * Dùng cùng với {@link ItemFactoryProvider} để tạo Item theo Factory Method.
 */
public enum ItemType {
    ELECTRONICS,   // Điện tử (laptop, điện thoại…)
    ART,           // Tác phẩm nghệ thuật
    VEHICLE        // Phương tiện giao thông
}
