import java.sql.SQLException;
import javax.swing.*;
import ui.kiosk.LandingPage;
import util.Inventory;

public class KioskMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // The menu and the stock come from MySQL, so check the connection first.
            try {
                Inventory.refresh();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "The kiosk could not connect to the database.\n"
                        + "Make sure MySQL is running and the login in data/dbconnect.java is correct.\n\n"
                        + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            LandingPage landing = new LandingPage();
            landing.setVisible(true);
        });
    }
}
