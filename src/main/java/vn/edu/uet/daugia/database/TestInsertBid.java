package vn.edu.uet.daugia.database;



import java.sql.Connection;
import java.sql.PreparedStatement;

public class TestInsertBid {

    public static void main(String[] args) {

        try {

            Connection connection =
                    DatabaseConnection.getConnection();

            String sql =
                    "INSERT INTO bids(username, price) VALUES (?, ?)";

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            statement.setString(1, "Quan");

            statement.setInt(2, 5000);

            statement.executeUpdate();

            System.out.println(
                    "Đã lưu bid vào database!"
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}