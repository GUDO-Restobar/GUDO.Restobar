package ui.kiosk;

import java.awt.*;
import javax.swing.*;

public class OrderOption extends JPanel {
    public OrderOption() {
        setLayout(new BorderLayout());
        setBackground(new Color(246, 246, 246));
        
        Font interLight;
        Font montserratBold;

        try {
            montserratBold = FontLoader.loadFont("public/assets/fonts/Inter/static/Montserrat-Bold.ttf");
            montserratBold = montserratBold.deriveFont(Font.PLAIN, 32f);

            interLight = FontLoader.loadFont("public/assets/fonts/Inter/static/Inter_18pt-Light.ttf");
            interLight = interLight.deriveFont(Font.PLAIN, 16f);
        } catch (java.io.IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load the fonts.", e);
        }

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon LogoImage = new ImageIcon("public/assets/logo/logo.png");
        Image logoImage = LogoImage.getImage().getScaledInstance(202, 202, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(logoImage));

        add(logoLabel, BorderLayout.NORTH);
        
    }
}