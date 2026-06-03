package vn.edu.uet.daugia.client.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Parse thời gian từ server/DB (ISO hoặc MySQL datetime). */
public final class DateTimeParseUtil {

    private static final DateTimeFormatter MYSQL =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeParseUtil() {}

    public static LocalDateTime parseFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(raw.replace(' ', 'T'));
            } catch (DateTimeParseException ignored2) {
                return LocalDateTime.parse(raw, MYSQL);
            }
        }
    }
}
