package database;

import vn.edu.uet.daugia.server.dao.DatabaseConnection;

import java.sql.Connection;

public class TestDatabase {
    public static void main(String[] args) {
        System.out.println("Đang thử kết nối tới MySQL...");
        try {
            Connection conn = DatabaseConnection.getConnection();
            System.out.println("THÀNH CÔNG: Đã thông đường ống tới kho chứa he_thong_dau_gia!");
            conn.close(); // Test xong thì đóng cửa kho lại cho an toàn
        } catch (Exception e) {
            System.out.println("THẤT BẠI: Không thể kết nối. Máy tính báo lỗi:");
            e.printStackTrace();
        }
    }
}