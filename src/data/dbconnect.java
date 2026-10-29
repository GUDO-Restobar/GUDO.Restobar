package data;

import java.sql.*;

public class dbconnect {
    private static final String URL =
            "jdbc:mysql://localhost:3306/gudorestobardb";

    private static final String USER = "root";
    private static final String PASSWORD = "JJthegreat_123"; //change password here if you have different password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
