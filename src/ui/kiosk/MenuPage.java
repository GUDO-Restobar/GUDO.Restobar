package ui.kiosk;

import java.awt.*;
import javax.swing.*;

public class MenuPage extends JPanel {
    public MenuPage() {
        setLayout(new BorderLayout());
        setBackground(new Color(246, 246, 246));

        ImageIcon LogoImage = new ImageIcon("public/assets/logo/logo-horizontal-transparent.png");
        Image logoImage = LogoImage.getImage().getScaledInstance(500, 121, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(logoImage));

        add(logoLabel, BorderLayout.NORTH);
    }
}
