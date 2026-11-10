package data;

import java.sql.*;

public class dbconnect {
    // DONT CHANGE ANYTHING HERE
    // DO NOT SHARE THESE DETAILS, THESE ARE CONFIDENTIAL AND ONLY FOR THE GROUP ONLY
    // //----- TURN THESE TO COMMENT IF WANT NYO MAGTESTING2 SA LOCAL HOST -----
    private static final String URL =
            "jdbc:mysql://mysql-27a36514-gudorestobar.b.aivencloud.com:24673/defaultdb?sslMode=REQUIRED";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_IcUEw8KPvS4MB30kd-n";
            
    //---- UNCOMMENT THESE IF YOU WANT TO TEST SA LOCAL HOST -----
    // private static final String URL =
    //         "jdbc:mysql://localhost:3306/defaultdb";
    // private static final String USER = "root";
    // private static final String PASSWORD = "root";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
