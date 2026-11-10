package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import javax.swing.*;
import util.ConfirmationBackground;
import util.UiColors;
import util.UiKit;

/**
 * "Are you sure?" prompt shown when the customer wants to throw the whole
 * order away.
 *
 * Yes clears the cart and returns to the landing page; No simply goes back
 * to whichever page the prompt was opened from (KioskMain decides).
 */
public class StartOver extends ConfirmationBackground {

    // ============================================================
    // FRONTEND - logo, message and the Yes / No buttons
    // ============================================================

    /** Yes: throw the order away and start again. */
    private final Runnable onStartOver;
    /** No: return to the previous page with the order untouched. */
    private final Runnable onGoBack;

    public StartOver(Runnable onStartOver, Runnable onGoBack) {
        this.onStartOver = onStartOver != null ? onStartOver : () -> {};
        this.onGoBack = onGoBack != null ? onGoBack : () -> {};

        setLayout(new BorderLayout());
        setBackground(UiColors.BACKGROUND);

        Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
        Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(115, 26, 90, 26));

        JLabel logo = new JLabel(UiKit.icon("public/assets/logo/logo.png", 150, 150));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("<html><center>Every Craving Deserves<br>a Second Chance</center></html>");
        heading.setFont(bold.deriveFont(30f));
        heading.setForeground(UiColors.ORANGE);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitle = new JLabel("Change your mind? Start your order anew");
        subtitle.setFont(regular.deriveFont(15f));
        subtitle.setForeground(UiColors.BROWN);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel bag = new JLabel(UiKit.icon("public/assets/icons/restart-bag.png", 190, 190));
        bag.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Yes
        JButton start = new JButton("Start Over");
        start.setFont(bold.deriveFont(16f));
        start.setForeground(UiColors.WHITE);
        start.setBackground(UiColors.ORANGE);
        start.setBorderPainted(false);
        start.setFocusPainted(false);
        start.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        start.setAlignmentX(Component.CENTER_ALIGNMENT);
        start.addActionListener(e -> confirmStartOver());

        // No
        JButton back = new JButton("Go Back");
        back.setFont(regular.deriveFont(15f));
        back.setForeground(UiColors.BROWN);
        back.setBackground(UiColors.BACKGROUND);
        back.setBorder(BorderFactory.createLineBorder(UiColors.BORDER));
        back.setFocusPainted(false);
        back.setAlignmentX(Component.CENTER_ALIGNMENT);
        back.addActionListener(e -> goBack());

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

    // ============================================================
    // BACKEND - clearing the order or backing out
    // ============================================================

    /** Yes: hand control to KioskMain, which clears the cart and shows the landing page. */
    private void confirmStartOver() {
        onStartOver.run();
    }

    /** No: return to the page this prompt was opened from, order untouched. */
    private void goBack() {
        onGoBack.run();
    }
}
