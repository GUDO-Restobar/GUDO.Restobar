package ui.kiosk;

import java.awt.*;
import javax.swing.*;
import util.*;

public class StartOver extends ConfirmationBackground {
    private static final Color ORANGE = new Color(236, 94, 2);
    private static final Color BROWN = new Color(74, 45, 35);
    private static final Color OFF_WHITE = new Color(246, 246, 246);
    private static final Color LINE = new Color(200, 200, 200);

    public StartOver(Runnable onStartOver, Runnable onGoBack) {
        setLayout(new BorderLayout());
        setBackground(OFF_WHITE);

        Font bold = loadFont("Montserrat/static/Montserrat-Bold.ttf");
        Font regular = loadFont("Montserrat/static/Montserrat-Regular.ttf");

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(115, 26, 90, 26));

        JLabel logo = new JLabel(scaledIcon("public/assets/logo/logo.png", 150, 150));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("<html><center>Every Craving Deserves<br>a Second Chance</center></html>");
        heading.setFont(bold.deriveFont(30f));
        heading.setForeground(ORANGE);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitle = new JLabel("Change your mind? Start your order anew");
        subtitle.setFont(regular.deriveFont(15f));
        subtitle.setForeground(BROWN);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel bag = new JLabel(scaledIcon("public/assets/icons/restart-bag.png", 190, 190));
        bag.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton start = new JButton("Start Over");
        start.setFont(bold.deriveFont(16f));
        start.setForeground(Color.WHITE);
        start.setBackground(ORANGE);
        start.setBorderPainted(false);
        start.setFocusPainted(false);
        start.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        start.setAlignmentX(Component.CENTER_ALIGNMENT);
        start.addActionListener(e -> {
            if (onStartOver != null) onStartOver.run();
        });

        JButton back = new JButton("Go Back");
        back.setFont(regular.deriveFont(15f));
        back.setForeground(BROWN);
        back.setBackground(OFF_WHITE);
        back.setBorder(BorderFactory.createLineBorder(LINE));
        back.setFocusPainted(false);
        back.setAlignmentX(Component.CENTER_ALIGNMENT);
        back.addActionListener(e -> {
            if (onGoBack != null) onGoBack.run();
        });

        Dimension buttonSize = new Dimension(330, 48);
        start.setMaximumSize(buttonSize);
        start.setPreferredSize(buttonSize);
        back.setMaximumSize(buttonSize);
        back.setPreferredSize(buttonSize);

        content.add(logo);
        content.add(Box.createVerticalStrut(14));
        content.add(heading);
        content.add(Box.createVerticalStrut(8));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(24));
        content.add(bag);
        content.add(Box.createVerticalGlue());
        content.add(start);
        content.add(Box.createVerticalStrut(10));
        content.add(back);
        add(content, BorderLayout.CENTER);
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
