package vn.edu.uet.daugia.client;

import java.net.Socket;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.google.gson.Gson;

import vn.edu.uet.daugia.shared.model.BidMessage;
import vn.edu.uet.daugia.shared.model.RegisterMessage;

public class AuctionClient {

    // =========================
    // CONFIG
    // =========================

    private static final String SERVER_IP =
            "10.11.71.231";

    private static final int SERVER_PORT =
            5000;

    // =========================
    // REGISTER
    // =========================

    public static void sendRegister(
            String username,
            String password
    ) {

        try {

            Socket socket =
                    new Socket(
                            SERVER_IP,
                            SERVER_PORT
                    );

            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            RegisterMessage register =
                    new RegisterMessage(
                            "REGISTER",
                            username,
                            password
                    );

            Gson gson =
                    new Gson();

            String json =
                    gson.toJson(register);

            out.println(json);

            System.out.println(
                    "Đã gửi REGISTER:"
            );

            System.out.println(json);

            String response =
                    in.readLine();

            System.out.println(
                    "Server phản hồi:"
            );

            System.out.println(response);

            socket.close();

        } catch (Exception e) {

            System.out.println(
                    "Lỗi REGISTER!"
            );

            e.printStackTrace();
        }
    }

    // =========================
    // TEST MAIN
    // =========================

    public static void main(String[] args) {

        try {

            System.out.println(
                    "Đang tìm kiếm Máy chủ ở cổng "
                            + SERVER_PORT + "..."
            );

            Socket socket =
                    new Socket(
                            SERVER_IP,
                            SERVER_PORT
                    );

            System.out.println(
                    "Đã kết nối thành công tới Máy chủ Đấu giá!"
            );

            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            BidMessage bid =
                    new BidMessage(
                            "BID",
                            "SP01",
                            "Trung khim",
                            1200.0
                    );

            Gson gson =
                    new Gson();

            String json =
                    gson.toJson(bid);

            out.println(json);

            System.out.println(
                    "Đã gửi JSON:"
            );

            System.out.println(json);

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