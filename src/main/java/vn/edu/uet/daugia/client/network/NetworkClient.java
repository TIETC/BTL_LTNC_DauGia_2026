package vn.edu.uet.daugia.client.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.google.gson.Gson;
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

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Đã kết nối thành công tới Máy chủ Đấu giá!");
        } catch (Exception e) {
            System.err.println("Không thể kết nối! Cửa Server đang đóng hoặc Server chưa bật.");
        }
    }

    // Hàm cũ: gửi BID
    public void sendBidMessage(BidMessage bid) {
        if (out != null) {
            String json = gson.toJson(bid);
            out.println(json);
            System.out.println("Đã gửi JSON lên Server: " + json);
        } else {
            System.err.println("Lỗi: Chưa kết nối được tới Server!");
        }
    }

    // THÊM MỚI: gửi bất kỳ JSON thô nào lên Server
    // Dùng cho CREATE_AUCTION, GET_AUCTIONS, và các lệnh khác sau này
    // Khác sendBidMessage ở chỗ: hàm này nhận thẳng chuỗi JSON, không cần tạo object
    public void sendRaw(String json) {
        if (out != null) {
            out.println(json);
            System.out.println("Đã gửi JSON lên Server: " + json);
        } else {
            System.err.println("Lỗi: Chưa kết nối tới Server! Gọi connect() trước.");
        }
    }

    // THÊM MỚI: đọc phản hồi từ Server sau khi gửi lệnh
    // Dùng khi cần biết Server xử lý thành công hay thất bại
    // Ví dụ: sau CREATE_AUCTION, đọc về {"status":"OK"} hay {"status":"ERROR"}
    public String readResponse() {
        try {
            if (in != null) {
                return in.readLine(); // đọc 1 dòng phản hồi từ Server
            }
        } catch (Exception e) {
            System.err.println("Lỗi đọc phản hồi từ Server: " + e.getMessage());
        }
        return null;
    }

    public Socket getSocket() {
        return socket;
    }
}