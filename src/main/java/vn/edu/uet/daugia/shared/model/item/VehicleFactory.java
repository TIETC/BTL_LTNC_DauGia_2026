package vn.edu.uet.daugia.shared.model.item;

import java.time.LocalDateTime;

/**
 * ConcreteFactory cho sản phẩm Vehicle.
 *
 * Tham số {@code extra}: biển số xe (licensePlate).
 * Nếu extra null/rỗng, biển số sẽ là "Không rõ".
 */
public class VehicleFactory implements ItemFactory {

    @Override
    public Item createItem(String id, String name, String description,
                           double startingPrice,
                           LocalDateTime startTime, LocalDateTime endTime,
                           String extra) {

        String licensePlate = (extra != null && !extra.isBlank()) ? extra.trim() : "Không rõ";

        return new Vehicle(id, name, description, startingPrice,
                startTime, endTime, licensePlate);
    }
}
