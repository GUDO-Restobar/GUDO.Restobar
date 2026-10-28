package data;
import java.sql.*;

public class testconnection {

    // FOR TESTING THE CONNECTION ONLY, DELETE WHEN FINAL SUBMISSION
    public static void main(String[] args) {
        try (Connection conn = dbconnect.getConnection()) {
            System.out.println("Successfully connected to MySQL!"); //expected output when connected successfuly
            System.out.println("Database: " + conn.getCatalog());

        } catch (SQLException e) {
            System.out.println("Connection failed!"); //output when connection DNE
            e.printStackTrace();
        }
    }
}