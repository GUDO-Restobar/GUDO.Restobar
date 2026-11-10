package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import javax.swing.*;
import util.UiColors;
import util.UiKit;

/**
 * The final screen: the order number the customer has to wait for, plus how
 * the order was taken and paid for.
 *
 * KioskMain fills the number in through {@link #setOrderInfo} once the order
 * has been saved to the database, and resets the kiosk after a short delay.
 */
public class OrderReceipt extends JPanel {

    // ============================================================
    // FRONTEND - headline, the orange order-number card and the footer
    // ============================================================

    private JLabel orderNumberLabel;
    private JLabel orderTypeLabel;

    public OrderReceipt() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiColors.BACKGROUND);

        Font montserratBold = UiKit.font("Inter/static/Montserrat-Bold.ttf")
            .deriveFont(Font.PLAIN, 32f);
        Font interLight = UiKit.font("Inter/static/Inter_18pt-Light.ttf")
            .deriveFont(Font.PLAIN, 16f);

        add(Box.createRigidArea(new Dimension(0, 30)));
        add(buildHeading(montserratBold, interLight));
        add(Box.createRigidArea(new Dimension(0, 30)));
        add(buildOrderCard(montserratBold, interLight));
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(buildFooter(interLight));
    }

    /** The word-mark, "Your Feast Awaits!" and the two instruction lines. */
    private JPanel buildHeading(Font montserratBold, Font interLight) {
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        heading.add(Box.createRigidArea(new Dimension(0, 20)));
        heading.add(buildLogo());
        heading.add(Box.createRigidArea(new Dimension(0, 100)));

        JLabel headline = new JLabel("Your Feast Awaits!");
        headline.setFont(montserratBold);
        headline.setAlignmentX(Component.CENTER_ALIGNMENT);
        headline.setForeground(UiColors.ORANGE);
        heading.add(headline);

        heading.add(Box.createRigidArea(new Dimension(0, 8)));
        heading.add(buildLine("Please collect your receipt and wait for your order ", interLight));
        heading.add(buildLine("number to be called.", interLight));
        return heading;
    }

    /** The big orange rounded card showing the order number. */
    private JPanel buildOrderCard(Font montserratBold, Font interLight) {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UiColors.ORANGE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(428, 264));
        card.setPreferredSize(new Dimension(428, 264));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Order Number:");
        title.setFont(interLight.deriveFont(Font.PLAIN, 24f));
        title.setForeground(UiColors.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        orderNumberLabel = new JLabel();
        orderNumberLabel.setFont(montserratBold.deriveFont(Font.BOLD, 96f));
        orderNumberLabel.setForeground(UiColors.WHITE);
        orderNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        orderTypeLabel = new JLabel("Take-out • Cash");
        orderTypeLabel.setFont(interLight.deriveFont(Font.PLAIN, 16f));
        orderTypeLabel.setForeground(UiColors.WHITE);
        orderTypeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createRigidArea(new Dimension(0, 24)));
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 16)));
        card.add(orderNumberLabel);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(orderTypeLabel);
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        center.add(card);
        return center;
    }

    /** The thank-you message under the card. */
    private JPanel buildFooter(Font interLight) {
        JPanel footer = new JPanel();
        footer.setBackground(UiColors.BACKGROUND);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.add(buildLine("Thank you for your faith in Gudo’s Restobar. Enjoy", interLight));
        footer.add(buildLine("your blessed meal!", interLight));
        return footer;
    }

    // ------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------

    private JLabel buildLogo() {
        return new JLabel(UiKit.icon(
            "public/assets/logo/logo-horizontal-transparent.png", 426, 104));
    }

    private static JLabel buildLine(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    // ============================================================
    // BACKEND - showing the order that was actually saved
    // ============================================================

    /** Shows the real order number from the database, plus how it was ordered and paid. */
    public void setOrderInfo(int orderID, String orderType, String paymentMethod) {
        String type = "Takeout".equalsIgnoreCase(orderType) ? "Take-out" : orderType;
        orderNumberLabel.setText("#" + orderID);
        orderTypeLabel.setText(type + " • " + paymentMethod);
    }
}
