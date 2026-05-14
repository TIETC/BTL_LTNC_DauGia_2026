package server;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {

    public static void main(String[] args) {

        try {

            int port = 5000;

            // Mở cổng server
            ServerSocket serverSocket =
                    new ServerSocket(port);

            System.out.println(
                    "Server đang chạy ở cổng "
                            + port
            );

            // Tạo thread pool
            ExecutorService pool =
                    Executors.newFixedThreadPool(10);

            // Server chạy liên tục
            while (true) {

                System.out.println(
                        "Đang chờ client kết nối..."
                );

                // Chờ client
                Socket clientSocket =
                        serverSocket.accept();

                System.out.println(
                        "Client đã kết nối!"
                );

                // Giao client cho thread xử lý
                pool.execute(
                        new ClientHandler(clientSocket)
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Lỗi server!"
            );

            e.printStackTrace();
        }
    }
}