package vn.edu.uet.daugia.shared;

public class ServerConfig {
    // THAY ĐỔI ĐỊA CHỈ IP Ở ĐÂY KHI CHẠY THỰC TẾ
    // Nếu chạy 1 mình trên 1 máy: Để "localhost"
    // Nếu chạy qua mạng LAN/Wi-fi chung: Điền IPv4 của máy chủ (VD: "192.168.1.45")
    // Nếu chạy qua Radmin VPN: Điền IP của Radmin cấp (VD: "26.115.89.12")
    public static final String SERVER_IP = "localhost";

    // Cổng giao tiếp mặc định (Tránh việc Server mở 5000 mà Client lại gọi 8080)
    public static final int SERVER_PORT = 5000;
}