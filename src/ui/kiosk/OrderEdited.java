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
 * Confirmation shown after "Save Changes to Order". It shows the new running
 * total and hands control back to KioskMain after the same delay as
 * {@link OrderAdded}, so both confirmations feel identical.
 */
public class OrderEdited extends ConfirmationBackground {

    // ============================================================
    // FRONTEND - logo, message, total and the bag illustration
    // ============================================================

    /** Runs when it is time to go back to the checkout page. */
    private final Runnable onFinished;

    /** Stops the automatic redirect if the page is thrown away early. */
    private final Timer redirectTimer;

    public OrderEdited(double total, Runnable onFinished) {
        this.onFinished = onFinished != null ? onFinished : () -> {};

        // Built first so removeNotify() below can stop it.
        redirectTimer = new Timer(AUTO_REDIRECT_MILLIS, e -> this.onFinished.run());
        redirectTimer.setRepeats(false);

        setLayout(new BorderLayout());
        setBackground(UiColors.BACKGROUND);

        Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
        Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(95, 24, 70, 24));

        JLabel logo = new JLabel(UiKit.icon("public/assets/logo/logo.png", 160, 160));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("<html><center>Your Cravings has<br>Been Modified</center></html>");
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

        JLabel bag = new JLabel(UiKit.icon("public/assets/icons/hd-bag.png", 180, 180));
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

        redirectTimer.start();
    }

    // ============================================================
    // BACKEND - the automatic redirect back to checkout
    // ============================================================

    @Override
    public void removeNotify() {
        // If this page is discarded early, make sure the timer cannot fire later.
        redirectTimer.stop();
        super.removeNotify();
    }
}
