package vn.edu.uet.daugia.client.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import vn.edu.uet.daugia.shared.model.BidMessage;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;

    private BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private PushListener pushListener;

    public interface PushListener {
        void onPush(String type, JsonObject data);
    }

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
            socket.connect(new java.net.InetSocketAddress(serverAddress, port), 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối thành công tới Máy chủ Đấu giá!");
            startReaderThread();
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("Hết thời gian kết nối!");
            socket = null; out = null; in = null;
        } catch (Exception e) {
            System.err.println("Không thể kết nối! Lỗi: " + e.getMessage());
            socket = null; out = null; in = null;
        }
    }

    private void startReaderThread() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("Nhận từ Server: " + line);

                    boolean isPush = false;
                    try {
                        JsonObject json = gson.fromJson(line, JsonObject.class);
                        if (json.has("type")) {
                            String type = json.get("type").getAsString();
                            // Tất cả các loại push — KHÔNG đưa vào responseQueue
                            if (type.equals("NEW_AUCTION")
                                    || type.equals("NEW_BID")
                                    || type.equals("AUCTION_CLOSED")
                                    || type.equals("AUCTION_DELETED")) {
                                isPush = true;
                                if (pushListener != null) {
                                    pushListener.onPush(type, json);
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    if (!isPush) {
                        responseQueue.put(line);
                    }
                }
            } catch (Exception e) {
                System.err.println("Mất kết nối Server: " + e.getMessage());
            }
        }, "ServerReader").start();
    }

    public void setPushListener(PushListener listener) {
        this.pushListener = listener;
    }

    public void clearPushListener() {
        this.pushListener = null;
    }

    public void clearPendingResponses() {
        responseQueue.clear();
        System.out.println("[NetworkClient] Đã xóa hàng đợi phản hồi cũ.");
    }

    public void sendRaw(String json) {
        if (out != null) {
            out.println(json);
            System.out.println("Đã gửi JSON lên Server: " + json);
        } else {
            System.err.println("Lỗi: Chưa kết nối tới Server!");
        }
    }

    public String readResponse() {
        try {
            return responseQueue.poll(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

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