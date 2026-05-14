package vn.edu.uet.daugia.client.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.google.gson.Gson;
// Nhớ import đúng đường dẫn model BidMessage của nhóm bạn nhé
import vn.edu.uet.daugia.shared.model.BidMessage;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;

    private NetworkClient() {
        gson = new Gson();
    }

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public void connect(String serverAddress, int port) {
        try {
            System.out.println("Đang tìm kiếm Máy chủ ở cổng " + port + "...");
            socket = new Socket(serverAddress, port);

            // Khởi tạo luồng Đọc/Ghi dữ liệu
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Đã kết nối thành công tới Máy chủ Đấu giá!");
        } catch (Exception e) {
            System.err.println("Không thể kết nối! Cửa Server đang đóng hoặc Server chưa bật.");
        }
    }

    // Hàm mới: Dùng để gửi tin nhắn JSON lên Server
    public void sendBidMessage(BidMessage bid) {
        if (out != null) {
            String json = gson.toJson(bid);
            out.println(json);
            System.out.println("Đã gửi JSON lên Server: " + json);
        } else {
            System.err.println("Lỗi: Chưa kết nối được tới Server!");
        }
    }

    public Socket getSocket() {
        return socket;
    }
}