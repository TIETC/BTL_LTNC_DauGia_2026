package vn.edu.uet.daugia.server.controller;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ServerDiscovery implements Runnable {
    private static final int DISCOVERY_PORT = 8888; // Port dùng để tìm nhau (khác port TCP 5000)

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
            System.out.println("📡 Mạng lưới Discovery đang chạy, lắng nghe ở cổng " + DISCOVERY_PORT + "...");
            byte[] recvBuf = new byte[1024];

            while (true) {
                // Nhận gói tin từ Client
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength()).trim();

                // Nếu đúng ám hiệu tìm Server
                if (message.equals("DISCOVER_AUCTION_SERVER")) {
                    byte[] sendData = "AUCTION_SERVER_HERE".getBytes();

                    // Gửi ngược lại câu trả lời tới IP và Port của Client vừa gọi
                    DatagramPacket sendPacket = new DatagramPacket(
                            sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);

                    System.out.println("✅ Đã phản hồi IP cho Client tại: " + packet.getAddress().getHostAddress());
                }
            }
        } catch (Exception ex) {
            System.err.println("Lỗi Server Discovery: " + ex.getMessage());
        }
    }
}