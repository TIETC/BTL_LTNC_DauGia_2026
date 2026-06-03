package vn.edu.uet.daugia.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Kết nối tới Server (Chạy trên máy cá nhân nên dùng localhost)
    public boolean connect(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối thành công tới Server!");
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi kết nối Server: " + e.getMessage());
            return false;
        }
    }

    // Hàm ném chuỗi JSON sang Server và chờ nhận kết quả về
    public String sendRequest(String jsonRequest) {
        try {
            out.println(jsonRequest); // Gửi đi
            return in.readLine();     // Đứng chờ Server trả lời rồi return
        } catch (Exception e) {
            return "{\"status\":\"ERROR\",\"message\":\"Mất kết nối tới Server\"}";
        }
    }

    // Đóng kết nối khi tắt app
    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}