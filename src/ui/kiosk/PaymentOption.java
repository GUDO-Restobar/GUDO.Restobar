package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;
import util.*;

/**
 * Asks the customer how they want to pay: Cash or Cashless.
 *
 * The choice is stored in {@link OrderSession} and the page reports back to
 * KioskMain, which then shows the receipt. Drawn on {@link ConfirmationBackground},
 * so it carries the same orange corner ribbons as the other full-screen pages.
 *
 * Every time the page is shown, its elements fade in one after the other:
 * top ribbon, logo, heading, sub-heading, payment cards, back button, bottom ribbon.
 */
public class PaymentOption extends ConfirmationBackground {

    // ============================================================
    // FRONTEND - heading, payment cards and the back button
    // ============================================================

    /** How long one element takes to appear, and the delay between elements. */
    private static final int FADE_MILLIS = 125;
    private static final int STAGGER_MILLIS = 25;

    /** Runs once a payment method has been chosen. */
    private final Runnable onPaymentSelected;
    /** Runs when the customer wants to return to the checkout page. */
    private final Runnable onBack;

    /** The entrance animation; elements fade in in the order they are added. */
    private final FadeInSequence entrance = new FadeInSequence(this, FADE_MILLIS, STAGGER_MILLIS);

    public PaymentOption(Runnable onPaymentSelected, Runnable onBack) {
        this.onPaymentSelected = onPaymentSelected != null ? onPaymentSelected : () -> {};
        this.onBack = onBack != null ? onBack : () -> {};

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // The order below is the order of the fade-in.
        entrance.add(alpha -> topLeftRibbonAlpha = alpha);
        add(Box.createRigidArea(new Dimension(0, 30)));
        add(buildHeading());
        add(Box.createRigidArea(new Dimension(0, 30)));
        add(fade(buildPaymentCards()));
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(buildBackBar());
        entrance.add(alpha -> bottomRightRibbonAlpha = alpha);

        // Replay the fade-in on every visit; hiding the page resets it to blank.
        entrance.reset();
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { entrance.start(); }
            @Override public void componentHidden(ComponentEvent e) { entrance.reset(); }
        });
    }

    /** Wraps an element so it fades, and adds it as the next step of the entrance. */
    private FadingPanel fade(Component element) {
        FadingPanel fading = new FadingPanel(element);
        entrance.add(fading::setAlpha);
        return fading;
    }

    /** Logo, the two headline lines and the sub-heading, all centred. */
    private JPanel buildHeading() {
        Font montserratBold = UiKit.font("Inter/static/Montserrat-Bold.ttf")
            .deriveFont(Font.PLAIN, 32f);
        Font interLight = UiKit.font("Inter/static/Inter_18pt-Light.ttf")
            .deriveFont(Font.PLAIN, 16f);

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        heading.add(fade(new JLabel(UiKit.icon("public/assets/logo/logo.png", 202, 202))));

        JPanel title = new JPanel();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.setOpaque(false);
        title.add(buildHeadline("Gudo Makes", montserratBold));
        title.add(buildHeadline("Payments Easy!", montserratBold));
        heading.add(fade(title));

        heading.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel subHeading = new JLabel("Choose how you shall pay for your blessings.");
        subHeading.setFont(interLight);
        heading.add(fade(subHeading));

        return heading;
    }

    /** The two stacked picture cards: Cash and Cashless. */
    private JPanel buildPaymentCards() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        center.add(buildPaymentCard("public/assets/icons/cash.png", "Cash"));
        center.add(Box.createRigidArea(new Dimension(0, 20)));
        center.add(buildPaymentCard("public/assets/icons/cashless.png", "Cashless"));
        return center;
    }

    /** The bar holding the single Back button (transparent so the ribbon shows). */
    private JPanel buildBackBar() {
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));

        ImageButton back = new ImageButton(UiKit.icon("public/assets/icons/button.png", 329, 49));
        back.addActionListener(e -> goBack());
        bar.add(fade(back));
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

    private JLabel buildHeadline(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(UiColors.ORANGE);
        return label;
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