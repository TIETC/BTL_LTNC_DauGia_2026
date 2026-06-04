package database;

import vn.edu.uet.daugia.server.dao.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class TestInsertBid {
    public static void main(String[] args) {
        String maSanPham = "SP01";
        String nguoiDatGia = "Tuan_Client";
        double giaTien = 600000;
        String sql = "INSERT INTO bids (auction_id, bidder_id, price, bid_time) VALUES (?, ?, ?, NOW())";

        try {
            System.out.println("Đang kết nối để ghi dữ liệu...");
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, maSanPham);    // Dấu ? số 1
            pstmt.setString(2, nguoiDatGia);  // Dấu ? số 2
            pstmt.setDouble(3, giaTien);      // Dấu ? số 3

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("THÀNH CÔNG: Đã cất giao dịch 600.000đ vào két sắt MySQL!");
            }
            pstmt.close();
            conn.close();

        } catch (Exception e) {
            System.out.println("LỖI: Không thể ghi dữ liệu!");
            e.printStackTrace();
        }
    }
}