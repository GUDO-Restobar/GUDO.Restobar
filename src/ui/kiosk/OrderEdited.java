package ui.kiosk;

import java.awt.*;
import javax.swing.*;
import util.*;

/**
 * Confirmation shown after "Save Changes to Order". It stays on screen for
 * 5 seconds, then hands control back so the kiosk can return to Checkout.
 */
public class OrderEdited extends ConfirmationBackground {
    private static final Color ORANGE = new Color(236, 94, 2);
    private static final Color BROWN = new Color(74, 45, 35);
    private static final Color OFF_WHITE = new Color(246, 246, 246);
    private static final int DISPLAY_MILLIS = 5000;

    private final Timer redirectTimer;

    public OrderEdited(double total, Runnable onFinished) {
        setLayout(new BorderLayout());
        setBackground(OFF_WHITE);

        Font bold = loadFont("Montserrat/static/Montserrat-Bold.ttf");
        Font regular = loadFont("Montserrat/static/Montserrat-Regular.ttf");

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(95, 24, 70, 24));

        JLabel logo = new JLabel(scaledIcon("public/assets/logo/logo.png", 160, 160));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("<html><center>Your Cravings has<br>Been Modified</center></html>");
        heading.setFont(bold.deriveFont(31f));
        heading.setForeground(ORANGE);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subheading = new JLabel("Your total has been updated");
        subheading.setFont(regular.deriveFont(16f));
        subheading.setForeground(BROWN);
        subheading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel totalLabel = new JLabel("\u20B1" + String.format("%,.2f", total));
        totalLabel.setFont(bold.deriveFont(21f));
        totalLabel.setForeground(ORANGE);
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel bag = new JLabel(scaledIcon("public/assets/icons/hd-bag.png", 180, 180));
        bag.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(logo);
        content.add(Box.createVerticalStrut(14));
        content.add(heading);
        content.add(Box.createVerticalStrut(8));
        content.add(subheading);
        content.add(Box.createVerticalStrut(34));
        content.add(totalLabel);
        content.add(Box.createVerticalStrut(28));
        content.add(bag);
        content.add(Box.createVerticalGlue());
        add(content, BorderLayout.CENTER);

        redirectTimer = new Timer(DISPLAY_MILLIS, e -> {
            if (onFinished != null) onFinished.run();
        });
        redirectTimer.setRepeats(false);
        redirectTimer.start();
    }

    @Override
    public void removeNotify() {
        // If this page is discarded early, make sure the timer cannot fire later.
        redirectTimer.stop();
        super.removeNotify();
    }

    private static ImageIcon scaledIcon(String path, int width, int height) {
        Image image = new ImageIcon(path).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private static Font loadFont(String file) {
        try {
            return FontLoader.loadFont("public/assets/fonts/" + file);
        } catch (java.io.IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load font: " + file, e);
        }
    }
}
