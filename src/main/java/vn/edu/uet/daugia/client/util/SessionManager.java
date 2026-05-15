package vn.edu.uet.daugia.client.util;

public class SessionManager {
    private static String role;
    private static String username;

    public static void setUser(String user, String r) {
        username = user;
        role = r;
    }

    public static String getRole() {
        return (role != null) ? role : "GUEST";
    }

    public static String getUsername() {
        return (username != null) ? username : "unknown";
    }

    // THÊM MỚI: dùng trong Controller để ẩn/hiện nút Admin
    public static boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public static void logout() {
        role = null;
        username = null;
        System.out.println("Đã xóa phiên đăng nhập.");
    }
}