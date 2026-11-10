package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import javax.swing.*;
import util.OrderSession;
import util.UiColors;
import util.UiKit;

/**
 * Asks the customer how they want to pay: Cash or Cashless.
 *
 * The choice is stored in {@link OrderSession} and the page reports back to
 * KioskMain, which then shows the receipt.
 */
public class PaymentOption extends JPanel {

    // ============================================================
    // FRONTEND - heading, payment cards and the back button
    // ============================================================

    /** Runs once a payment method has been chosen. */
    private final Runnable onPaymentSelected;
    /** Runs when the customer wants to return to the checkout page. */
    private final Runnable onBack;

    public PaymentOption(Runnable onPaymentSelected, Runnable onBack) {
        this.onPaymentSelected = onPaymentSelected != null ? onPaymentSelected : () -> {};
        this.onBack = onBack != null ? onBack : () -> {};

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiColors.BACKGROUND);

        add(Box.createRigidArea(new Dimension(0, 30)));
        add(buildHeading());
        add(Box.createRigidArea(new Dimension(0, 30)));
        add(buildPaymentCards());
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(buildBackBar());
    }

    /** Logo, the two headline lines and the sub-heading. */
    private JPanel buildHeading() {
        Font montserratBold = UiKit.font("Inter/static/Montserrat-Bold.ttf")
            .deriveFont(Font.PLAIN, 32f);
        Font interLight = UiKit.font("Inter/static/Inter_18pt-Light.ttf")
            .deriveFont(Font.PLAIN, 16f);

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        heading.add(buildLogo());
        heading.add(buildHeadline("Gudo Makes", montserratBold));
        heading.add(buildHeadline("Payments Easy!", montserratBold));
        heading.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel subHeading = new JLabel("Choose how you shall pay for your blessings.");
        subHeading.setFont(interLight);
        subHeading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.add(subHeading);

        return heading;
    }

    /** The two stacked picture cards: Cash and Cashless. */
    private JPanel buildPaymentCards() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(buildPaymentCard("public/assets/icons/cash.png", "Cash"));
        center.add(Box.createRigidArea(new Dimension(0, 20)));
        center.add(buildPaymentCard("public/assets/icons/cashless.png", "Cashless"));
        return center;
    }

    /** The bar holding the single Back button. */
    private JPanel buildBackBar() {
        JPanel bar = new JPanel();
        bar.setBackground(UiColors.BACKGROUND);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));
        bar.add(buildBackButton());
        return bar;
    }

    // ------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------

    /** One large picture card that also carries the payment method it selects. */
    private JButton buildPaymentCard(String imagePath, String paymentMethod) {
        JButton button = new JButton(UiKit.icon(imagePath, 382, 189));
        button.setPreferredSize(new Dimension(382, 189));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> choosePaymentMethod(paymentMethod));
        return button;
    }

    /** The image-only Back button used at the bottom of the page. */
    private JButton buildBackButton() {
        JButton button = new JButton(UiKit.icon("public/assets/icons/button.png", 329, 49));
        button.setPreferredSize(new Dimension(329, 49));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> goBack());
        return button;
    }

    private JLabel buildHeadline(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(UiColors.ORANGE);
        return label;
    }

    private JLabel buildLogo() {
        return new JLabel(UiKit.icon("public/assets/logo/logo.png", 202, 202));
    }

    // ============================================================
    // BACKEND - remember the choice, then hand over to KioskMain
    // ============================================================

    /** Stores Cash / Cashless for the whole order, then shows the receipt. */
    private void choosePaymentMethod(String paymentMethod) {
        // The payment method is kept apart from the order type (Dine-in / Takeout).
        OrderSession.getInstance().setPaymentMethod(paymentMethod);
        onPaymentSelected.run();
    }

    /** Returns to the checkout page. */
    private void goBack() {
        onBack.run();
    }
}
