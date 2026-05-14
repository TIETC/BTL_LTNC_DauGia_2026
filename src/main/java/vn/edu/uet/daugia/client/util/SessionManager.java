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

    // --- BỔ SUNG HÀM NÀY ĐỂ HẾT LỖI ĐỎ ---
    public static void logout() {
        role = null;
        username = null;
        System.out.println("Đã xóa phiên đăng nhập.");
    }
}