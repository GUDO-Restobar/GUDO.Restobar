package pages.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import javax.swing.*;
import util.feat.*;
import util.ui.*;

/**
 * The cashless payment page: scan the QR code, or tap the card on the reader.
 *
 * "Pay Card" completes the payment (KioskMain then shows the receipt);
 * "Discard Payment" goes back to the payment options. A random QR code is
 * picked every time the page is shown, and the page fades in element by element
 * (see {@link FadeInPage}).
 */
public class CashlessPayment extends FadeInPage {

    // ============================================================
    // FRONTEND - logo, QR code, card instructions and the buttons
    // ============================================================

    private static final int BUTTON_WIDTH = 323;
    private static final int BUTTON_HEIGHT = 51;   // button face plus its shadow

    private final Font montserratBold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
    private final Font interLight = UiKit.font("Inter/static/Inter_18pt-Light.ttf");
    private final Font interBold = UiKit.font("Inter/static/Inter_18pt-Bold.ttf");

    /** Runs when the customer pays with the card. */
    private final Runnable onPaid;
    /** Runs when the customer gives up and returns to the payment options. */
    private final Runnable onDiscard;

    private final RandomQrCode qrCode = new RandomQrCode();

    public CashlessPayment(Runnable onPaid, Runnable onDiscard) {
        this.onPaid = onPaid != null ? onPaid : () -> {};
        this.onDiscard = onDiscard != null ? onDiscard : () -> {};

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // The order below is the order of the fade-in.
        add(gap(36));
        add(fade(new JLabel(UiKit.icon("public/assets/logo/logo-horizontal-transparent.png", 415, 101))));
        add(gap(28));
        add(fade(buildSection("Scan to Pay",
            "Scan the QR code using your", "banking or e-wallet app.")));
        add(gap(14));
        add(fade(qrCode));
        add(gap(30));
        add(fade(buildLabel("OR", montserratBold.deriveFont(Font.PLAIN, 28f), UiColors.TEXT_DARK)));
        add(gap(24));
        add(fade(buildSection("Tap Your Card to Pay",
            "Use the card reader below the kiosk to complete", "your payment")));
        add(gap(30));
        add(fade(buildButton("Pay Card", UiColors.ORANGE, UiColors.ORANGE_HOVER,
            null, UiColors.WHITE, this::payCard)));
        add(gap(12));
        add(fade(buildButton("Discard Payment", UiColors.SURFACE, UiColors.HOVER_GREY,
            UiColors.BROWN, UiColors.BROWN, this::discard)));
        add(Box.createVerticalGlue());
        finishEntrance();
    }

    /** A different random QR code every time the page opens. */
    @Override
    protected void onShown() {
        qrCode.pickRandom();
    }

    /** An orange heading with a few centred lines of text under it. */
    private JPanel buildSection(String title, String... lines) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.add(buildLabel(title, montserratBold.deriveFont(Font.PLAIN, 30f), UiColors.ORANGE));
        section.add(gap(2));
        for (String line : lines) {
            section.add(buildLabel(line, interLight.deriveFont(Font.PLAIN, 15f), UiColors.TEXT_DARK));
        }
        return section;
    }

    private JLabel buildLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private JButton buildButton(String text, Color base, Color hover,
                                Color outline, Color textColor, Runnable onClick) {
        JButton button = new RoundedButton(text, null, base, hover, outline);
        button.setFont(interBold.deriveFont(Font.PLAIN, 16f));
        button.setForeground(textColor);
        button.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        button.addActionListener(e -> onClick.run());
        return button;
    }

    private Component gap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }

    // ============================================================
    // BACKEND - hand over to KioskMain
    // ============================================================

    /** The card was tapped: the payment is done. */
    private void payCard() {
        onPaid.run();
    }

    /** Back to the payment options. */
    private void discard() {
        onDiscard.run();
    }
}