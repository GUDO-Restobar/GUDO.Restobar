package data;

import java.sql.*;
import io.github.cdimascio.dotenv.Dotenv;


public class dbconnect {
    // DONT CHANGE ANYTHING HERE
    // DO NOT SHARE THESE DETAILS, THESE ARE CONFIDENTIAL AND ONLY FOR THE GROUP ONLY
    // //----- TURN THESE TO COMMENT IF WANT NYO MAGTESTING2 SA LOCAL HOST -----
    private static final Dotenv dotenv = Dotenv.load();
    
    private static final String URL = dotenv.get("DB_URL");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");
            
    //---- UNCOMMENT THESE IF YOU WANT TO TEST SA LOCAL HOST -----
    // private static final String URL =
    //         "jdbc:mysql://localhost:3306/defaultdb";
    // private static final String USER = "root";
    // private static final String PASSWORD = "root";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
