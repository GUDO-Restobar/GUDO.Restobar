package ui.kiosk;

import java.awt.*;
import javax.swing.*;
import util.*;


public class LandingPage extends JFrame {
    private static final String LANDING_SCREEN = "landing";
    private static final String ORDER_OPTION_SCREEN = "order-option";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screenPanel = new JPanel(cardLayout);

    public LandingPage() {

        setTitle("Gudo Restobar");
        setSize(580, 1040);
        getContentPane().setBackground(new Color(246, 246, 246));
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel landingPanel = new JPanel(new BorderLayout());
        landingPanel.setBackground(new Color(246, 246, 246));

        JLabel bannerLabel = new FadingBanner(
            new String[] {
                "public/assets/banners/landing-page-1.png",
                "public/assets/banners/landing-page-2.png",
                "public/assets/banners/landing-page-3.png",
            }, 580, 676);

        ImageIcon LogoImage = new ImageIcon("public/assets/logo/logo-horizontal-transparent.png");
        Image logoImage = LogoImage.getImage().getScaledInstance(500, 121, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(logoImage));

        Font interExtraBold;
        Font interLight;

        try {
            interExtraBold = FontLoader.loadFont(
            "public/assets/fonts/Inter/static/Inter_28pt-ExtraBold.ttf"
            );
            interLight = FontLoader.loadFont(
            "public/assets/fonts/Inter/static/Inter_18pt-Light.ttf"
            );
        } catch (java.io.IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load the button fonts.", e);
        }

        ImageIcon buttonIcon = new ImageIcon("public/assets/icons/touch.png");
        Image scaledIcon = buttonIcon.getImage()
            .getScaledInstance(96, 96, Image.SCALE_SMOOTH);

        JButton startBtn = new JButton(
            "<html><center>"
                + "<font face='" + interExtraBold.getFontName() +"' size='7'>Start Order</font><br>"
                + "<font face='" + interLight.getFontName() +"' size='5'>To unlock Gudo's Flavor</font>"
                + "</center></html>",
            new ImageIcon(scaledIcon)
        ) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                int shadowSize = 8;
                int cornerRadius = 10;
                int buttonHeight = getHeight() - shadowSize;

                // Layered translucent shapes create a soft-shadow effect.
                for (int offset = shadowSize; offset > 0; offset--) {
                    int alpha = 4 + (shadowSize - offset) * 3;
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.fillRoundRect(
                        0, offset, getWidth(), buttonHeight,
                        cornerRadius, cornerRadius
                    );
                }

                g2.setColor(getBackground());
                g2.fillRoundRect(
                    0, 0, getWidth(), buttonHeight,
                    cornerRadius, cornerRadius
                );

                g2.dispose();
                super.paintComponent(g);
            }
        };

        Color buttonColor = new Color(236, 94, 2);
        Color hoverColor = new Color(255, 120, 35);

        startBtn.setBackground(buttonColor);
        startBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                startBtn.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                startBtn.setBackground(buttonColor);
            }
        });

        startBtn.setForeground(new Color(246, 246, 246));

        startBtn.setPreferredSize(new Dimension(500, 140));
        startBtn.setIconTextGap(12);
        startBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        startBtn.setMargin(new Insets(0, 0, 0, 30));
        startBtn.setOpaque(false);
        startBtn.setContentAreaFilled(false);
        startBtn.setBorderPainted(false);
        startBtn.setFocusPainted(false);
        startBtn.addActionListener(e -> cardLayout.show(screenPanel, ORDER_OPTION_SCREEN));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setBackground(new Color(246, 246, 246));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));
        buttonPanel.add(startBtn);

        landingPanel.add(bannerLabel, BorderLayout.NORTH);
        landingPanel.add(logoLabel, BorderLayout.CENTER);
        landingPanel.add(buttonPanel, BorderLayout.SOUTH);

        screenPanel.add(landingPanel, LANDING_SCREEN);
        screenPanel.add(new OrderOption(), ORDER_OPTION_SCREEN);
<<<<<<< HEAD
        screenPanel.add(new MenuPage(null, null, null), "menu_page");
=======
>>>>>>> ae9e80ad287502062ef844ef3e8f403cb88642b4
        setContentPane(screenPanel);
    }
}
