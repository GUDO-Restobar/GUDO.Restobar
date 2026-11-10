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
 * Confirmation shown after an item was added to the order.
 *
 * It shows the new running total and offers the two things the customer can
 * do next: keep ordering or go straight to checkout. If they do nothing it
 * returns to the menu by itself after {@link ConfirmationBackground#AUTO_REDIRECT_MILLIS}.
 */
public class OrderAdded extends ConfirmationBackground {

    // ============================================================
    // FRONTEND - logo, message, total, bag and the two next-step buttons
    // ============================================================

    /** Runs when the customer wants to keep ordering (back to the menu). */
    private final Runnable onBackToMenu;
    /** Runs when the customer wants to pay now. */
    private final Runnable onGoToCheckout;

    /** Stops the automatic redirect if the page is thrown away early. */
    private final Timer redirectTimer;

    public OrderAdded(double total, Runnable onBackToMenu, Runnable onGoToCheckout) {
        this.onBackToMenu = onBackToMenu != null ? onBackToMenu : () -> {};
        this.onGoToCheckout = onGoToCheckout != null ? onGoToCheckout : () -> {};

        // Built first so the buttons below can stop it.
        redirectTimer = new Timer(AUTO_REDIRECT_MILLIS, e -> this.onBackToMenu.run());
        redirectTimer.setRepeats(false);

        setLayout(new BorderLayout());
        setBackground(UiColors.BACKGROUND);

        Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
        Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(75, 24, 55, 24));

        JLabel logo = new JLabel(UiKit.icon("public/assets/logo/logo.png", 150, 150));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("<html><center>Your Cravings has<br>Been Answered</center></html>");
        heading.setFont(bold.deriveFont(31f));
        heading.setForeground(UiColors.ORANGE);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subheading = new JLabel("Your total has been updated");
        subheading.setFont(regular.deriveFont(16f));
        subheading.setForeground(UiColors.BROWN);
        subheading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel totalLabel = new JLabel(UiKit.peso(total));
        totalLabel.setFont(bold.deriveFont(21f));
        totalLabel.setForeground(UiColors.ORANGE);
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel bag = new JLabel(UiKit.icon("public/assets/icons/hd-bag.png", 160, 160));
        bag.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.setPreferredSize(new Dimension(510, 48));
        buttons.setMaximumSize(new Dimension(510, 48));
        
        content.add(logo);
        content.add(Box.createVerticalStrut(12));
        content.add(heading);
        content.add(Box.createVerticalStrut(8));
        content.add(subheading);
        content.add(Box.createVerticalStrut(28));
        content.add(totalLabel);
        content.add(Box.createVerticalStrut(20));
        content.add(bag);
        content.add(Box.createVerticalGlue());
        content.add(buttons);
        add(content, BorderLayout.CENTER);

        redirectTimer.start();
    }

    /** One of the two orange next-step buttons. */
    private static JButton buildButton(String text, Font bold, Font regular) {
        JButton button = new JButton(text);
        button.setFont(bold.deriveFont(15f));
        button.setForeground(UiColors.WHITE);
        button.setBackground(UiColors.ORANGE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ============================================================
    // BACKEND - the automatic redirect back to the menu
    // ============================================================

    @Override
    public void removeNotify() {
        // If this page is discarded early, make sure the timer cannot fire later.
        redirectTimer.stop();
        super.removeNotify();
    }
}
