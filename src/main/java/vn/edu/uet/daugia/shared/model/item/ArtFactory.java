package vn.edu.uet.daugia.shared.model.item;

import java.time.LocalDateTime;

/**
 * ConcreteFactory cho sản phẩm Art.
 *
 * Tham số {@code extra}: tên tác giả và năm sáng tác.
 * Định dạng extra:
 *   "Picasso"        → artist = "Picasso",  creationYear = 0
 *   "Picasso|1937"   → artist = "Picasso",  creationYear = 1937
 */
public class ArtFactory implements ItemFactory {

    @Override
    public Item createItem(String id, String name, String description,
                           double startingPrice,
                           LocalDateTime startTime, LocalDateTime endTime,
                           String extra) {

        String artist = "Không rõ";
        int creationYear = 0;

        if (extra != null && !extra.isBlank()) {
            String[] parts = extra.split("\\|", 2);
            artist = parts[0].trim();
            if (parts.length == 2) {
                try {
                    creationYear = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) { }
            }
        }

        return new Art(id, name, description, startingPrice,
                startTime, endTime, artist, creationYear);
    }
}
