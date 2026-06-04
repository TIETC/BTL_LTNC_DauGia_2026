package vn.edu.uet.daugia.server.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import vn.edu.uet.daugia.server.dao.DatabaseConnection;
import vn.edu.uet.daugia.shared.exception.AuctionClosedException;
import vn.edu.uet.daugia.shared.exception.InvalidBidException;
import vn.edu.uet.daugia.shared.model.user.Bidder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String jsonRequest;

            while ((jsonRequest = in.readLine()) != null) {
                System.out.println("[Yêu cầu từ Client]: " + jsonRequest);

                try {
                    JsonObject requestObj = gson.fromJson(jsonRequest, JsonObject.class);
                    String type = requestObj.has("type") ? requestObj.get("type").getAsString() : "";
                    String response = "";

                    switch (type) {
                        case "LOGIN":
                            String user = requestObj.get("username").getAsString();
                            String pass = requestObj.get("password").getAsString();
                            String role = DatabaseConnection.checkLogin(user, pass);
                            if (role != null) {
                                response = "{\"status\":\"SUCCESS\",\"role\":\"" + role + "\"}";
                            } else {
                                response = "{\"status\":\"FAIL\",\"message\":\"Sai tài khoản hoặc mật khẩu\"}";
                            }
                            break;

                        case "REGISTER":
                            String regUser = requestObj.get("username").getAsString();
                            String regPass = requestObj.get("password").getAsString();
                            String regRole = requestObj.get("role").getAsString();

                            boolean isRegistered = DatabaseConnection.registerUser(regUser, regPass, regRole);
                            if (isRegistered) {
                                response = "{\"status\":\"SUCCESS\",\"message\":\"Đăng ký thành công\"}";
                            } else {
                                response = "{\"status\":\"FAIL\",\"message\":\"Tài khoản đã tồn tại hoặc có lỗi xảy ra\"}";
                            }
                            break;

                        case "ADD_ITEM":
                            try {
                                String itemId = requestObj.get("item_id").getAsString();
                                String name = requestObj.get("name").getAsString();
                                double price = requestObj.get("starting_price").getAsDouble();
                                String startTime = requestObj.get("start_time").getAsString();
                                String endTime = requestObj.get("end_time").getAsString();
                                String imagePath = requestObj.get("image_path").getAsString();
                                String sellerName = requestObj.has("seller_name") ? requestObj.get("seller_name").getAsString() : "Ẩn danh";
                                String description = requestObj.has("description") ? requestObj.get("description").getAsString() : "";

                                boolean isAdded = DatabaseConnection.insertItem(itemId, name, price, startTime, endTime, imagePath, sellerName, description);
                                if (isAdded) {
                                    response = "{\"status\":\"SUCCESS\",\"message\":\"Đã thêm sản phẩm thành công\"}";
                                } else {
                                    response = "{\"status\":\"ERROR\",\"message\":\"Lỗi lưu vào CSDL\"}";
                                }
                            } catch (Exception e) {
                                response = "{\"status\":\"ERROR\",\"message\":\"Lỗi dữ liệu JSON gửi lên bị thiếu trường\"}";
                            }
                            break;

                        case "GET_ALL_ITEMS":
                            response = DatabaseConnection.getAllItemsAsJson();
                            break;

                        case "GET_ITEM":
                            String queryId = requestObj.get("item_id").getAsString();
                            response = DatabaseConnection.getItemAsJson(queryId);
                            break;

                        case "BID":
                            try {
                                String auctionId = requestObj.get("auctionId").getAsString();
                                String bidderUsername = requestObj.get("bidderId").getAsString();
                                double bidAmount = requestObj.get("price").getAsDouble();

                                // [OOP INTEGRATION] Khởi tạo đối tượng Bidder theo chuẩn
                                Bidder currentBidder = new Bidder(bidderUsername, bidderUsername + "@mail.com", "---", 50000000);

                                // Gọi xuống DB và lấy kết quả
                                response = DatabaseConnection.placeBid(auctionId, currentBidder.getUsername(), bidAmount);

                            } catch (InvalidBidException e) {
                                // [BẮT LỖI OOP]: Khi giá thấp hơn giá hiện tại
                                response = "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}";
                            } catch (AuctionClosedException e) {
                                // [BẮT LỖI OOP]: Khi phiên đấu giá đã kết thúc
                                response = "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}";
                            } catch (Exception e) {
                                response = "{\"status\":\"ERROR\",\"message\":\"Dữ liệu đặt giá không hợp lệ\"}";
                            }
                            break;

                        default:
                            response = "{\"status\":\"ERROR\",\"message\":\"Cú pháp JSON không hợp lệ\"}";
                            break;
                    }

                    out.println(response);

                } catch (Exception parseException) {
                    System.err.println("Lỗi phân tích JSON từ Client: " + parseException.getMessage());
                    out.println("{\"status\":\"ERROR\",\"message\":\"Định dạng JSON gửi lên bị hỏng\"}");
                }
            }
        } catch (Exception e) {
            System.out.println("Một Client đã ngắt kết nối: " + socket.getInetAddress().getHostAddress());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) { socket.close(); }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}