package ui.kiosk;

import java.awt.*;
import javax.swing.*;

import util.*;

public class OrderReceipt extends JPanel {
    private static final String LANDING_SCREEN = "landing";
    private static final String MENU_SCREEN = "menu_page";

    public OrderReceipt() {
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

        ImageIcon LogoImage = new ImageIcon("public/assets/logo/logo-horizontal-transparent.png");
        Image logoImage = LogoImage.getImage().getScaledInstance(426, 104, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(logoImage));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel headingLabel = new JLabel("Your Feast Awaits!");
        headingLabel.setFont(montserratBold);
        headingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headingLabel.setForeground(Color.decode("#EC5E02"));

        JLabel subHeadingLabel = new JLabel("Please collect your receipt and wait for your order ");
        subHeadingLabel.setFont(interLight);
        subHeadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subHeadingLabel2 = new JLabel("number to be called.");
        subHeadingLabel2.setFont(interLight);
        subHeadingLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);

        heading.add(Box.createRigidArea(new Dimension(0, 20)));
        heading.add(logoLabel);
        heading.add(Box.createRigidArea(new Dimension(0, 100)));
        heading.add(headingLabel);
        heading.add(Box.createRigidArea(new Dimension(0, 8)));
        heading.add(subHeadingLabel);
        heading.add(subHeadingLabel2);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createRigidArea(new Dimension(0, 20)));

       
        JPanel orderNum = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                
                g2d.setColor(Color.decode("#EC5E02"));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                
                g2d.dispose();
            }
        };
        
        orderNum.setLayout(new BoxLayout(orderNum, BoxLayout.Y_AXIS));
        orderNum.setOpaque(false);
        orderNum.setMaximumSize(new Dimension(428, 264));
        orderNum.setPreferredSize(new Dimension(428, 264));
        orderNum.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Labels inside the card
        JLabel lblTitle = new JLabel("Order Number:");
        lblTitle.setFont(interLight.deriveFont(Font.PLAIN, 24f));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblNumber = new JLabel("#67"); 
        Font montserratExtraBold;
        try {
            montserratExtraBold = FontLoader.loadFont("public/assets/fonts/Inter/static/Montserrat-Bold.ttf").deriveFont(Font.BOLD, 96f);
        } catch (Exception e) {
            montserratExtraBold = montserratBold.deriveFont(Font.BOLD, 96f);
        }
        lblNumber.setFont(montserratExtraBold);
        lblNumber.setForeground(Color.WHITE);
        lblNumber.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblType = new JLabel("Take-out • Cash");
        lblType.setFont(interLight.deriveFont(Font.PLAIN, 16f));
        lblType.setForeground(Color.WHITE);
        lblType.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Assemble the card contents with spacing
        orderNum.add(Box.createRigidArea(new Dimension(0, 24)));
        orderNum.add(lblTitle);
        orderNum.add(Box.createRigidArea(new Dimension(0, 16)));
        orderNum.add(lblNumber);
        orderNum.add(Box.createRigidArea(new Dimension(0, 20)));
        orderNum.add(lblType);
        orderNum.add(Box.createRigidArea(new Dimension(0, 24)));

        center.add(orderNum);

        JPanel btmPanel = new JPanel();
        btmPanel.setBackground(new Color(246, 246, 246));
        btmPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));
        btmPanel.setLayout(new BoxLayout(btmPanel, BoxLayout.Y_AXIS));

        JLabel subTitleLabel = new JLabel("Thank you for your faith in Gudo’s Restobar. Enjoy");
        subTitleLabel.setFont(interLight);
        subTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subTitleLabel2 = new JLabel("your blessed meal!");
        subTitleLabel2.setFont(interLight);
        subTitleLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);
        btmPanel.add(subTitleLabel);
        btmPanel.add(subTitleLabel2);

        add(Box.createRigidArea(new Dimension(0, 30)));
        add(heading);
        add(Box.createRigidArea(new Dimension(0, 30)));
        add(center);
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(btmPanel);
    }
}