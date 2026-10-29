package ui.kiosk;

import java.awt.*;
import javax.swing.*;
import util.*;

public class OrderOption extends JPanel {
    private static final String LANDING_SCREEN = "landing";
    private static final String MENU_SCREEN = "menu_page";

    public OrderOption() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
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
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel headingLabel = new JLabel("Gudo Has Answered");
        headingLabel.setFont(montserratBold);
        headingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headingLabel.setForeground(Color.decode("#EC5E02"));

        JLabel headingLabel2 = new JLabel("Your Cravings");
        headingLabel2.setFont(montserratBold);
        headingLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);
        headingLabel2.setForeground(Color.decode("#EC5E02"));

        JLabel subHeadingLabel = new JLabel("Choose how you shall receive your blessings.");
        subHeadingLabel.setFont(interLight);
        subHeadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        heading.add(logoLabel);
        heading.add(headingLabel);
        heading.add(headingLabel2);
        heading.add(Box.createRigidArea(new Dimension(0, 8)));
        heading.add(subHeadingLabel);

        JPanel center = new JPanel();
        center.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 15));
        center.setOpaque(false);
        center.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton importButton = createCardButton("public/assets/icons/import_dinein.png", MENU_SCREEN, "Dine-in");
        JButton exportButton = createCardButton("public/assets/icons/export_takeout.png", MENU_SCREEN, "Takeout");
        center.add(importButton);
        center.add(exportButton);

        JPanel handsPanel = new JPanel();
        handsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0)); 
        handsPanel.setOpaque(false);
        handsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon leftIcon = new ImageIcon("public/assets/icons/left_hand.png");
        Image leftImg = leftIcon.getImage().getScaledInstance(270, 240, Image.SCALE_SMOOTH);
        JLabel leftHandLabel = new JLabel(new ImageIcon(leftImg));

        ImageIcon rightIcon = new ImageIcon("public/assets/icons/right_hand.png");
        Image rightImg = rightIcon.getImage().getScaledInstance(270, 240, Image.SCALE_SMOOTH);
        JLabel rightHandLabel = new JLabel(new ImageIcon(rightImg));

        handsPanel.add(leftHandLabel);
        handsPanel.add(rightHandLabel);

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(new Color(246, 246, 246));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));
        JButton backButton = createBackButton(LANDING_SCREEN);
        btnPanel.add(backButton);

        add(Box.createRigidArea(new Dimension(0, 30)));
        add(heading);
        add(Box.createRigidArea(new Dimension(0, 30)));
        add(center);
        add(Box.createRigidArea(new Dimension(0, -150)));
        add(handsPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(btnPanel);
    }

    private JButton createCardButton(String imagePath, String targetScreen, String orderType) {
        ImageIcon icon = new ImageIcon(imagePath);
        Image scaledImg = icon.getImage().getScaledInstance(208, 269, Image.SCALE_SMOOTH);
        
        JButton button = new JButton(new ImageIcon(scaledImg));
        button.setPreferredSize(new Dimension(208, 269));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> {
            OrderSession.setOrderType(orderType);
            System.out.println("Selected Order Type: " + orderType);

            // Navigate to screen via CardLayout
            Component ancestor = button;
            while ((ancestor = ancestor.getParent()) != null) {
                if (ancestor instanceof Container) {
                    LayoutManager layout = ((Container) ancestor).getLayout();
                    if (layout instanceof CardLayout) {
                        CardLayout cl = (CardLayout) layout;
                        cl.show((Container) ancestor, targetScreen);
                        break;
                    }
                }
            }
        });

        return button;
    }

    private JButton createBackButton(String targetScreen) {
        ImageIcon icon = new ImageIcon("public/assets/icons/button.png");
        Image scaledImg = icon.getImage().getScaledInstance(329, 49, Image.SCALE_SMOOTH);
        
        JButton button = new JButton(new ImageIcon(scaledImg));
        button.setPreferredSize(new Dimension(329, 49));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> {
            Component ancestor = button;
            while ((ancestor = ancestor.getParent()) != null) {
                if (ancestor instanceof Container) {
                    LayoutManager layout = ((Container) ancestor).getLayout();
                    if (layout instanceof CardLayout) {
                        CardLayout cl = (CardLayout) layout;
                        cl.show((Container) ancestor, targetScreen);
                        break;
                    }
                }
            }
        });

        return button;
    }
}