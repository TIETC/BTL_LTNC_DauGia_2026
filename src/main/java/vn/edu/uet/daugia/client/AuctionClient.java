package vn.edu.uet.daugia.client;

import javafx.application.Application;
import javafx.stage.Stage;

import vn.edu.uet.daugia.client.network.NetworkClient;
import vn.edu.uet.daugia.client.util.SceneManager;
import vn.edu.uet.daugia.shared.model.RegisterMessage;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class AuctionClient extends Application {

    // =========================
    // CONFIG
    // =========================
    // Không cần hardcode IP cố định nữa, hệ thống sẽ tự động quét qua mạng LAN
    public static final int SERVER_PORT = 5000;
    private static final int DISCOVERY_PORT = 8888; // Cổng UDP dùng để dò tìm Server

    // =========================
    // ĐIỂM KHỞI ĐỘNG JAVAFX
    // =========================
    @Override
    public void start(Stage stage) {
        // Bước 1: Đăng ký Stage với SceneManager
        SceneManager.setMainStage(stage);

        // Bước 2: Tự động dò tìm IP của Server trong cùng mạng Wi-Fi/LAN
        String targetIp = discoverServerIP();

        if (targetIp == null) {
            System.out.println("⚠️ Không tìm thấy Server nào trong mạng LAN.");
            System.out.println("🔄 Tự động chuyển hướng kết nối về máy cục bộ '127.0.0.1' (Localhost)...");
            targetIp = "127.0.0.1";
        }

        // Bước 3: Kết nối tới Server bằng IP tìm được (hoặc IP fallback)
        System.out.println("⚙️ Đang kết nối TCP tới Server tại địa chỉ [" + targetIp + ":" + SERVER_PORT + "]...");
        NetworkClient.getInstance().connect(targetIp, SERVER_PORT);

        // Bước 4: Mở màn hình Login
        SceneManager.switchScene("/view/Login.fxml", "Hệ thống đấu giá trực tuyến");

        stage.show();
    }

    // =========================
    // CƠ CHẾ TỰ ĐỘNG QUÉT LAN ĐỂ LẤY IP SERVER
    // =========================
    private static String discoverServerIP() {
        System.out.println("🔍 [LAN Discovery] Đang phát tín hiệu dò tìm Server...");

        // Sử dụng DatagramSocket để gửi gói tin UDP Broadcast
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(2500); // Chờ phản hồi từ Server tối đa 2.5 giây

            // Gửi thông điệp mật mã tới địa chỉ toàn mạng 255.255.255.255 công cổng 8888
            byte[] sendData = "DISCOVER_AUCTION_SERVER".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
            socket.send(sendPacket);

            // Chuẩn bị bộ nhớ đệm để nhận phản hồi từ Server
            byte[] recvBuf = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);

            // Đón nhận gói tin phản hồi
            socket.receive(receivePacket);
            String message = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

            // Nếu Server phản hồi đúng mật mã xác nhận
            if ("AUCTION_SERVER_HERE".equals(message)) {
                String serverIp = receivePacket.getAddress().getHostAddress();
                System.out.println("🎉 [LAN Discovery] Tìm thấy Server thành công! IP của Server là: " + serverIp);
                return serverIp;
            }

        } catch (SocketTimeoutException e) {
            System.out.println("❌ [LAN Discovery] Không nhận được phản hồi từ Server nào (Hết thời gian chờ).");
        } catch (Exception e) {
            System.out.println("💥 [LAN Discovery] Gặp lỗi trong quá trình quét mạng: " + e.getMessage());
        }

        return null; // Trả về null nếu không quét ra Server
    }

    // =========================
    // REGISTER — dùng kết nối chung
    // =========================
    public static void sendRegister(String username, String password, String role) {
        try {
            RegisterMessage register = new RegisterMessage("REGISTER", username, password, role);
            Gson gson = new Gson();
            String json = gson.toJson(register);

            NetworkClient.getInstance().sendRaw(json);
            System.out.println("Đã gửi REGISTER: " + json);

            String response = NetworkClient.getInstance().readResponse();
            System.out.println("Server phản hồi: " + response);

        } catch (Exception e) {
            System.out.println("Lỗi REGISTER!");
            e.printStackTrace();
        }
    }

    // =========================
    // MAIN
    // =========================
    public static void main(String[] args) {
        launch(args); // gọi start() ở trên
    }
}