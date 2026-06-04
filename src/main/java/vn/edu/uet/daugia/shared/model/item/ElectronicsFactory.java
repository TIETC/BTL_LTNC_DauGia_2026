package vn.edu.uet.daugia.shared.model.item;

import java.time.LocalDateTime;

/**
 * ConcreteFactory cho sản phẩm Electronics.
 *
 * Tham số {@code extra}: tên hãng sản xuất (brand).
 * Warranty mặc định là 12 tháng nếu không tách được từ extra.
 *
 * Định dạng extra linh hoạt:
 *   "Asus"           → brand = "Asus",  warranty = 12
 *   "Asus|24"        → brand = "Asus",  warranty = 24
 */
public class ElectronicsFactory implements ItemFactory {

    private static final int DEFAULT_WARRANTY_MONTHS = 12;

    @Override
    public Item createItem(String id, String name, String description,
                           double startingPrice,
                           LocalDateTime startTime, LocalDateTime endTime,
                           String extra) {

        String brand = "Unknown";
        int warrantyMonths = DEFAULT_WARRANTY_MONTHS;

        if (extra != null && !extra.isBlank()) {
            String[] parts = extra.split("\\|", 2);
            brand = parts[0].trim();
            if (parts.length == 2) {
                try {
                    warrantyMonths = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) { }
            }
        }

        return new Electronics(id, name, description, startingPrice,
                startTime, endTime, brand, warrantyMonths);
    }
}
