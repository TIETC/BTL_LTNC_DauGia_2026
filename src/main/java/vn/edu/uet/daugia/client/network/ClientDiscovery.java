package vn.edu.uet.daugia.client.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class ClientDiscovery {
    private static final int DISCOVERY_PORT = 8888;

    public static String discoverServerIP() {
        try (DatagramSocket c = new DatagramSocket()) {
            c.setBroadcast(true);
            c.setSoTimeout(3000); // Chờ phản hồi tối đa 3 giây

            // Gửi gói tin "hét" lên toàn mạng LAN qua địa chỉ Broadcast 255.255.255.255
            byte[] sendData = "DISCOVER_AUCTION_SERVER".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData, sendData.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
            c.send(sendPacket);
            System.out.println("🔍 Đang tìm kiếm Server Đấu Giá trong mạng LAN...");

            // Chờ phản hồi từ Server
            byte[] recvBuf = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);

            try {
                c.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

                if (message.equals("AUCTION_SERVER_HERE")) {
                    String serverIP = receivePacket.getAddress().getHostAddress();
                    System.out.println("🎉 Đã tìm thấy Server tự động tại IP: " + serverIP);
                    return serverIP; // Trả về IP để đi kết nối
                }
            } catch (SocketTimeoutException e) {
                System.out.println("❌ Không tìm thấy Server nào trong mạng (Hết thời gian chờ).");
            }
        } catch (Exception ex) {
            System.err.println("Lỗi khi dò tìm Server: " + ex.getMessage());
        }
        return null; // Trả về null nếu không tìm thấy
    }
}