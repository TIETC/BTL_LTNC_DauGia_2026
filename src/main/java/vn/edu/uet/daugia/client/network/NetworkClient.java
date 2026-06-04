package vn.edu.uet.daugia.client.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import vn.edu.uet.daugia.shared.ServerConfig; // Import file cấu hình chung

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Hàm kết nối tự động lấy IP và Port từ cấu hình dùng chung
    public boolean connect() {
        try {
            socket = new Socket(ServerConfig.SERVER_IP, ServerConfig.SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối thành công tới Server tại IP: " + ServerConfig.SERVER_IP);
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi kết nối Server (" + ServerConfig.SERVER_IP + "): " + e.getMessage());
            return false;
        }
    }

    // Hàm ném chuỗi JSON sang Server và chờ nhận kết quả về
    public String sendRequest(String jsonRequest) {
        try {
            if (out != null && in != null) {
                out.println(jsonRequest);
                return in.readLine();
            }
            return "{\"status\":\"ERROR\",\"message\":\"Chưa thiết lập kết nối mạng\"}";
        } catch (Exception e) {
            return "{\"status\":\"ERROR\",\"message\":\"Mất kết nối tới Server\"}";
        }
    }

    // Đóng kết nối khi tắt app
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}