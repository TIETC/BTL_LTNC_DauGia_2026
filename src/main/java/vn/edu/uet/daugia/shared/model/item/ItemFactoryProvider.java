package vn.edu.uet.daugia.shared.model.item;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry tập trung: map ItemType → ItemFactory.
 *
 * Cách dùng ở bất kỳ đâu trong code:
 * <pre>
 *   Item item = ItemFactoryProvider.create(
 *       ItemType.ELECTRONICS, id, name, desc, price, start, end, "Asus|24");
 * </pre>
 *
 * Để thêm loại mới (ví dụ JEWELRY) chỉ cần:
 *   1. Thêm giá trị vào {@link ItemType}
 *   2. Tạo class JewelryFactory implements ItemFactory
 *   3. Đăng ký: REGISTRY.put(ItemType.JEWELRY, new JewelryFactory());
 *   – Không cần sửa bất kỳ code nào khác.
 */
public class ItemFactoryProvider {

    private static final Map<ItemType, ItemFactory> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put(ItemType.ELECTRONICS, new ElectronicsFactory());
        REGISTRY.put(ItemType.ART,         new ArtFactory());
        REGISTRY.put(ItemType.VEHICLE,     new VehicleFactory());
    }

    /**
     * Tạo Item bằng factory tương ứng với loại.
     *
     * @throws IllegalArgumentException nếu loại chưa được đăng ký
     */
    public static Item create(ItemType type,
                              String id, String name, String description,
                              double startingPrice,
                              LocalDateTime startTime, LocalDateTime endTime,
                              String extra) {
        ItemFactory factory = REGISTRY.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Chưa có factory cho loại item: " + type);
        }
        return factory.createItem(id, name, description, startingPrice, startTime, endTime, extra);
    }

    /**
     * Tiện ích: tạo Item từ chuỗi tên loại (ví dụ lấy từ DB/JSON).
     * Mặc định là ELECTRONICS nếu không nhận ra.
     */
    public static Item createFromString(String typeName,
                                        String id, String name, String description,
                                        double startingPrice,
                                        LocalDateTime startTime, LocalDateTime endTime,
                                        String extra) {
        ItemType type;
        try {
            type = ItemType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ItemType.ELECTRONICS; // fallback mặc định
        }
        return create(type, id, name, description, startingPrice, startTime, endTime, extra);
    }

    /** Kiểm tra loại có được hỗ trợ không. */
    public static boolean isSupported(ItemType type) {
        return REGISTRY.containsKey(type);
    }
}
