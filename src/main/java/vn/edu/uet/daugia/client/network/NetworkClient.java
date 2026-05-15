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

            socket = new Socket();
            // Timeout 5 giây khi kết nối — không treo mãi nếu Server không có
            socket.connect(new java.net.InetSocketAddress(serverAddress, port), 5000);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Timeout 10 giây khi đọc — không treo mãi nếu Server không phản hồi
            socket.setSoTimeout(10000);

            System.out.println("Đã kết nối thành công tới Máy chủ Đấu giá!");
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("Hết thời gian kết nối! Server không phản hồi sau 5 giây.");
            socket = null; out = null; in = null;
        } catch (Exception e) {
            System.err.println("Không thể kết nối! Lỗi: " + e.getMessage());
            socket = null; out = null; in = null;
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

    // Gửi JSON thô lên Server
    public void sendRaw(String json) {
        if (out != null) {
            out.println(json);
            System.out.println("Đã gửi JSON lên Server: " + json);
        } else {
            System.err.println("Lỗi: Chưa kết nối tới Server! Gọi connect() trước.");
        }
    }

    // Đọc phản hồi từ Server — tự động báo lỗi sau 10 giây nếu không có phản hồi
    public String readResponse() {
        try {
            if (in != null) {
                return in.readLine();
            } else {
                System.err.println("Lỗi: Chưa kết nối, không thể đọc phản hồi!");
                return null;
            }
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("Hết thời gian chờ phản hồi từ Server (10 giây)!");
            return null;
        } catch (Exception e) {
            System.err.println("Lỗi đọc phản hồi: " + e.getMessage());
            return null;
        }
    }

    public Socket getSocket() {
        return socket;
    }
}