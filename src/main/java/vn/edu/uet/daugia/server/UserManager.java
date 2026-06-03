package vn.edu.uet.daugia.server;

import vn.edu.uet.daugia.database.DatabaseConnection;
import vn.edu.uet.daugia.shared.model.user.Bidder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserManager {

    public static Bidder findBidder(String bidderId) {
        if (bidderId == null || bidderId.trim().isEmpty()) return null;

        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn == null) return fallback(bidderId);

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT username, email, password FROM users WHERE username = ? AND role = 'BIDDER'");
            ps.setString(1, bidderId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String username = rs.getString("username");
                String email    = rs.getString("email") != null ? rs.getString("email") : username + "@local";
                String password = rs.getString("password") != null ? rs.getString("password") : "";

                double balance = 1_000_000;
                try { balance = rs.getDouble("balance"); } catch (Exception ignored) {}

                return new Bidder(username, email, password, balance);
            }
            return null;

        } catch (Exception e) {
            System.err.println("[UserManager] Lỗi query bidder: " + e.getMessage());
            return fallback(bidderId);
        }
    }

    private static Bidder fallback(String bidderId) {
        System.err.println("[UserManager] Fallback cho bidder: " + bidderId);
        return new Bidder(bidderId, bidderId + "@local", "", 1_000_000);
    }
}