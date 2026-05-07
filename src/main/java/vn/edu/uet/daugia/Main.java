package vn.edu.uet.daugia;
import vn.edu.uet.daugia.model.user.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== KHỞI TẠO HỆ THỐNG ===");

        Bidder bidder = new Bidder("trung_pro", "trung@gmail.com", "123456", 5000000);
        Seller seller = new Seller("quan_store", "quan@gmail.com", "123456", "Cửa hàng Đồ Cổ Quân");
        Admin admin = new Admin("admin_tuantran", "admin@gmail.com", "admin_vip_pass");

        System.out.println("\n=== THÔNG TIN NGƯỜI DÙNG ===");
        User[] danhSachNguoiDung = {bidder, seller, admin};
        for (User u : danhSachNguoiDung) {
            System.out.println(u.getInfo());
        }

        System.out.println("\n=== TEST LOGIC ===");
        System.out.println("Test 1: Bidder đăng nhập đúng pass: " + bidder.checkPassword("123456"));
        System.out.println("Test 2: Bidder đăng nhập sai pass: " + bidder.checkPassword("khong_biet"));

        System.out.println("Test 3: Bidder mua món hàng giá 2 triệu...");
        if (bidder.hasSufficientBalance(2000000)) {
            bidder.deductBalance(2000000);
            System.out.println("Mua thành công! Số dư còn lại: " + bidder.getBalance() + " VND");
        }

        System.out.println("Test 4: Bidder mua món hàng giá 10 triệu...");
        if (!bidder.hasSufficientBalance(10000000)) {
            System.out.println("Thất bại: Không đủ tiền!");
        }
    }
}