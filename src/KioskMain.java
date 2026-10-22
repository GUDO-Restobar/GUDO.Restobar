import ui.kiosk.LandingPage;

import javax.swing.*;

public class KioskMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LandingPage landing = new LandingPage();
            landing.setVisible(true);
        });
    }
}

