package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import javax.swing.*;
import util.*;

/**
 * The first screen of the kiosk: the fading banner, the logo and the
 * Start Order button. Navigation is handled by the window (KioskMain)
 * through the onStartOrder callback.
 */
public class LandingPage extends JPanel {

    // ============================================================
    // FRONTEND - logo, Start Order button and the page layout
    // ============================================================

    /** Called when the customer presses Start Order. */
    private final Runnable onStartOrder;

    public LandingPage(Runnable onStartOrder) {
        this.onStartOrder = onStartOrder;
        setLayout(new BorderLayout());
        setBackground(UiColors.BACKGROUND);

        add(buildFadingBanner(), BorderLayout.NORTH);
        add(buildLogo(), BorderLayout.CENTER);
        add(buildStartArea(), BorderLayout.SOUTH);
    }

    /** The GUDO word-mark, scaled to fit the middle of the page. */
    private JLabel buildLogo() {
        return new JLabel(UiKit.icon("public/assets/logo/logo-horizontal-transparent.png", 500, 121));
    }

    /** The big "Start Order" button, with its soft drop shadow. */
    private JPanel buildStartArea() {
        Font extraBold = UiKit.font("Inter/static/Inter_28pt-ExtraBold.ttf");
        Font light = UiKit.font("Inter/static/Inter_18pt-Light.ttf");

        Image touchIcon = UiKit.icon("public/assets/icons/touch.png", 96, 96).getImage();

        JButton startBtn = new JButton(
            "<html><center>"
                + "<font face='" + extraBold.getFontName() + "' size='7'>Start Order</font><br>"
                + "<font face='" + light.getFontName() + "' size='5'>To unlock Gudo's Flavor</font>"
                + "</center></html>",
            new ImageIcon(touchIcon)
        ) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int shadowSize = 8;
                int cornerRadius = 10;
                int buttonHeight = getHeight() - shadowSize;

                // Layered translucent shapes create a soft-shadow effect.
                for (int offset = shadowSize; offset > 0; offset--) {
                    g2.setColor(UiColors.shadow(4 + (shadowSize - offset) * 3));
                    g2.fillRoundRect(0, offset, getWidth(), buttonHeight, cornerRadius, cornerRadius);
                }

                // The face keeps the button's own colour so the hover state shows.
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), buttonHeight, cornerRadius, cornerRadius);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        startBtn.setBackground(UiColors.ORANGE);
        startBtn.setForeground(UiColors.BACKGROUND);
        startBtn.setPreferredSize(new Dimension(500, 140));
        startBtn.setIconTextGap(12);
        startBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        startBtn.setMargin(new Insets(0, 0, 0, 30));
        startBtn.setOpaque(false);
        startBtn.setContentAreaFilled(false);
        startBtn.setBorderPainted(false);
        startBtn.setFocusPainted(false);
        startBtn.addActionListener(e -> startOrder());

        // Hover feedback.
        UiKit.hover(startBtn, UiColors.ORANGE, UiColors.ORANGE_HOVER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setBackground(UiColors.BACKGROUND);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));
        buttonPanel.add(startBtn);
        return buttonPanel;
    }

    // ============================================================
    // FADING BANNER - the slideshow across the top of the page
    // ============================================================

    /** Cycles the three landing-page banners with a cross-fade between them. */
    private JLabel buildFadingBanner() {
        return new FadingBanner(
            new String[] {
                "public/assets/banners/landing-page-1.png",
                "public/assets/banners/landing-page-2.png",
                "public/assets/banners/landing-page-3.png",
            }, 580, 676);
    }

    // ============================================================
    // BACKEND - Start Order hands over to KioskMain
    // ============================================================

    /** Runs when the customer presses Start Order; KioskMain shows OrderOption. */
    private void startOrder() {
        onStartOrder.run();
    }
}
