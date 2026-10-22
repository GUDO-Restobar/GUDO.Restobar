package ui.kiosk;

import javax.swing.*;
import java.awt.*;

public class LandingPage extends JFrame {
    public LandingPage() {
        setTitle("Gudo Restobar");
        setSize(580, 1040);
        getContentPane().setBackground(new Color(246, 246, 246));
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        ImageIcon BannerImage1 = new ImageIcon("public/assets/banners/landing-page-1.png");
        Image bannerImage1 = BannerImage1.getImage().getScaledInstance(580, 676, Image.SCALE_SMOOTH);
        JLabel bannerLabel1 = new JLabel(new ImageIcon(bannerImage1));

        ImageIcon LogoImage = new ImageIcon("public/assets/logo/logo-horizontal-transparent.png");
        Image logoImage = LogoImage.getImage().getScaledInstance(411, 100, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(logoImage));

        JButton startBtn = new JButton("Start Order");
        startBtn.setFont(new Font("Arial", Font.PLAIN, 28));
        startBtn.addActionListener(e -> {
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            dispose();
        });

        add(bannerLabel1, BorderLayout.NORTH);
        add(logoLabel, BorderLayout.CENTER);
        add(startBtn, BorderLayout.SOUTH);
    }
}
