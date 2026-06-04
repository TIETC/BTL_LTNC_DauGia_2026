package vn.edu.uet.daugia.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/auction_db";

    private static final String USER =
            "root";

    private static final String PASSWORD =
            "123456";

    public static Connection getConnection() {

        try {

            Connection connection =
                    DriverManager.getConnection(
                            URL,
                            USER,
                            PASSWORD
                    );

            System.out.println(
                    "Kết nối MySQL thành công!"
            );

            return connection;

        } catch (Exception e) {

            System.out.println(
                    "Kết nối MySQL thất bại!"
            );

            e.printStackTrace();

            return null;
        }
    }
}