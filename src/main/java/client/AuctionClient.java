package client;

import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.google.gson.Gson;

import vn.edu.uet.daugia.model.BidMessage;

public class AuctionClient {

    public static void main(String[] args) {

        try {

            String serverAddress = "127.0.0.1";
            int port = 5000;

            System.out.println(
                    "Đang tìm kiếm Máy chủ ở cổng "
                            + port + "..."
            );

            // Kết nối server
            Socket socket =
                    new Socket(serverAddress, port);

            System.out.println(
                    "Đã kết nối thành công tới Máy chủ Đấu giá!"
            );

            // Gửi dữ liệu tới server
            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            // Nhận dữ liệu từ server
            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            // =========================
            // TEST BID
            // =========================

            BidMessage bid =
                    new BidMessage(
                            "BID",
                            "Quan",
                            1200
                    );

            Gson gson = new Gson();

            String json =
                    gson.toJson(bid);

            // Gửi JSON
            out.println(json);

            System.out.println(
                    "Đã gửi JSON:"
            );

            System.out.println(json);

            // Đọc phản hồi server
            String response =
                    in.readLine();

            System.out.println(
                    "Server phản hồi:"
            );

            System.out.println(response);

            socket.close();

        } catch (Exception e) {

            System.out.println(
                    "Không thể kết nối tới server!"
            );

            e.printStackTrace();
        }
    }
}