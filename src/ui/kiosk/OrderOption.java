package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import javax.swing.*;
import util.*;

/**
 * Asks the customer how they want to receive the order: Dine-in or Takeout.
 *
 * The choice is stored in {@link OrderSession} and the page reports back to
 * KioskMain, which then shows the menu. Drawn on
 * {@link util.ConfirmationBackground}, so it carries the same orange corner
 * ribbons as the confirmation pages.
 */
public class OrderOption extends ConfirmationBackground {

    // ============================================================
    // FRONTEND - heading, order-type cards and the back button
    // ============================================================

    /** Runs once an order type has been chosen. */
    private final Runnable onOrderTypeSelected;
    /** Runs when the customer wants to return to the landing page. */
    private final Runnable onBack;

    public OrderOption(Runnable onOrderTypeSelected, Runnable onBack) {
        this.onOrderTypeSelected = onOrderTypeSelected != null ? onOrderTypeSelected : () -> {};
        this.onBack = onBack != null ? onBack : () -> {};

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiColors.BACKGROUND);

        add(Box.createRigidArea(new Dimension(0, 30)));
        add(buildHeading());
        add(Box.createRigidArea(new Dimension(0, 30)));
        add(buildOptionCards());
        add(Box.createRigidArea(new Dimension(0, -150)));
        add(buildHands());
        add(Box.createRigidArea(new Dimension(0, 10)));
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

        heading.add(buildLogo(202));
        heading.add(buildHeadline("Gudo Has Answered", montserratBold));
        heading.add(buildHeadline("Your Cravings", montserratBold));
        heading.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel subHeading = new JLabel("Choose how you shall receive your blessings.");
        subHeading.setFont(interLight);
        subHeading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.add(subHeading);

        return heading;
    }

    /** The two big picture cards: Dine-in and Takeout. */
    private JPanel buildOptionCards() {
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 15));
        center.setOpaque(false);
        center.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(buildOrderCard("public/assets/icons/import_dinein.png", "Dine-in"));
        center.add(buildOrderCard("public/assets/icons/export_takeout.png", "Takeout"));
        return center;
    }

    /** The pair of pointing hands sitting under the cards. */
    private JPanel buildHands() {
        JPanel handsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        handsPanel.setOpaque(false);
        handsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        handsPanel.add(buildScaledLabel("public/assets/icons/left_hand.png", 270, 280));
        handsPanel.add(buildScaledLabel("public/assets/icons/right_hand.png", 270, 280));
        return handsPanel;
    }

    /** The bar holding the single Back button (transparent so the corner ribbon shows). */
    private JPanel buildBackBar() {
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));
        bar.add(buildBackButton());
        return bar;
    }

    // ------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------

    /** One large picture card that also carries the order type it selects. */
    private JButton buildOrderCard(String imagePath, String orderType) {
        JButton button = new JButton(UiKit.icon(imagePath, 208, 269));
        button.setPreferredSize(new Dimension(208, 269));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> chooseOrderType(orderType));
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

    private JLabel buildLogo(int size) {
        JLabel logo = new JLabel(UiKit.icon("public/assets/logo/logo.png", size, size));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        return logo;
    }

    private static JLabel buildScaledLabel(String path, int width, int height) {
        return new JLabel(UiKit.icon(path, width, height));
    }

    // ============================================================
    // BACKEND - remember the choice, then hand over to KioskMain
    // ============================================================

    /** Stores Dine-in / Takeout for the whole order, then shows the menu. */
    private void chooseOrderType(String orderType) {
        OrderSession.getInstance().setOrderType(orderType);
        onOrderTypeSelected.run();
    }

    /** Returns to the landing page. */
    private void goBack() {
        onBack.run();
    }
}
