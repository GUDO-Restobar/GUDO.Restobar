package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import util.*;

/**
 * The "Your Order" screen: every line of the cart, the running total and the
 * buttons that drive the rest of the flow - Order More, Start Over and
 * Complete Order (which goes to payment).
 *
 * Each line can be edited through its "Edit Meal" button; KioskMain decides
 * where that leads.
 */
public class CheckoutPage extends JPanel {

    // ============================================================
    // FRONTEND - header, item list, empty state and the footer buttons
    // ============================================================

    private static final String SCROLL_THUMB = "public/assets/scrollbar/scroll-thumb.png";
    private static final String SCROLL_TRACK = "public/assets/scrollbar/scroll-track.png";

    /** The two states the middle of the page can be in. */
    private static final String EMPTY_CARD = "empty";
    private static final String LIST_CARD = "list";

    private final Cart cart;
    /** Called with the product whose "Edit Meal" button was pressed. */
    private final Consumer<Product> onEditItem;
    /** Called when the customer wants to go back to the menu. */
    private final Runnable onOrderMore;
    /** Called when the customer wants to throw the order away. */
    private final Runnable onStartOver;
    /** Called when the customer is ready to pay. */
    private final Runnable onComplete;

    private final Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
    private final Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");

    private final CardLayout centerLayout = new CardLayout();
    private final JPanel centerPanel = new JPanel(centerLayout);
    private final JPanel itemsPanel = new ScrollablePanel();
    private final JLabel totalAmount = new JLabel("\u20B10.00");

    public CheckoutPage(Cart cart, Runnable onOrderMore, Runnable onStartOver,
                        Runnable onComplete, Consumer<Product> onEditItem) {
        this.cart = cart != null ? cart : new Cart();
        this.onOrderMore = onOrderMore != null ? onOrderMore : () -> {};
        this.onStartOver = onStartOver != null ? onStartOver : () -> {};
        this.onComplete = onComplete != null ? onComplete : () -> {};
        this.onEditItem = onEditItem != null ? onEditItem : product -> {};

        setLayout(new BorderLayout());
        setBackground(UiColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        showCartContents();
    }

    /** Logo plus the page title. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        header.add(new JLabel(UiKit.icon("public/assets/logo/logo-circle.png", 92, 92)));

        JLabel title = new JLabel("Your Order");
        title.setFont(bold.deriveFont(34f));
        title.setForeground(UiColors.ORANGE);
        header.add(title);

        return header;
    }

    /** Wraps the empty-state message and the scrollable list in a CardLayout. */
    private JComponent buildCenter() {
        centerPanel.setOpaque(false);
        centerPanel.add(buildEmptyState(), EMPTY_CARD);
        centerPanel.add(buildListArea(), LIST_CARD);
        return centerPanel;
    }

    /** Shown instead of the list while the cart is empty. */
    private JPanel buildEmptyState() {
        JPanel placeholder = new PlaceholderPanel();
        placeholder.setLayout(new GridBagLayout());
        placeholder.setOpaque(false);
        placeholder.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel message = new JLabel("Your order is empty", SwingConstants.CENTER);
        message.setFont(regular.deriveFont(16f));
        message.setForeground(UiColors.TEXT_HINT);
        placeholder.add(message);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 8, 24, 8));
        wrapper.add(placeholder, BorderLayout.CENTER);
        return wrapper;
    }

    /** The scrolling column of order cards. */
    private JScrollPane buildListArea() {
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Same scroll bar design as the Menu page.
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        bar.setUnitIncrement(24);
        bar.setUI(new KioskScrollBarUI(SCROLL_THUMB, SCROLL_TRACK));
        bar.setPreferredSize(new Dimension(26, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 8));
        bar.setBackground(UiColors.BACKGROUND);
        return scrollPane;
    }

    /** Total on the left with Order More / Start Over, amount and Pay on the right. */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setOpaque(false);

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.add(buildTotalLabel());
        leftColumn.add(Box.createVerticalStrut(14));
        leftColumn.add(buildSmallButtons());

        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.add(buildAmountLabel());
        rightColumn.add(Box.createVerticalStrut(14));
        rightColumn.add(buildCompleteButton());

        footer.add(leftColumn, BorderLayout.WEST);
        footer.add(rightColumn, BorderLayout.CENTER);
        return footer;
    }

    private JLabel buildTotalLabel() {
        JLabel label = new JLabel("Total:");
        label.setFont(bold.deriveFont(22f));
        label.setForeground(UiColors.ORANGE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setPreferredSize(new Dimension(207, 32));
        label.setMaximumSize(new Dimension(207, 32));
        return label;
    }

    private JLabel buildAmountLabel() {
        totalAmount.setFont(bold.deriveFont(22f));
        totalAmount.setForeground(UiColors.ORANGE);
        totalAmount.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalAmount.setPreferredSize(new Dimension(310, 32));
        totalAmount.setMaximumSize(new Dimension(310, 32));
        return totalAmount;
    }

    /** "Order More" on top of "Start Over", both full width of the left column. */
    private JPanel buildSmallButtons() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton orderMoreButton = new RoundedButton("Order More", null,
            UiColors.SURFACE, UiColors.HOVER_GREY, UiColors.BORDER);
        orderMoreButton.setFont(regular.deriveFont(13f));
        orderMoreButton.setForeground(UiColors.BROWN);
        orderMoreButton.setPreferredSize(new Dimension(207, 57));
        orderMoreButton.setMaximumSize(new Dimension(207, 57));
        orderMoreButton.addActionListener(e -> orderMore());

        JButton startOverButton = new RoundedButton("Start Over", null,
            UiColors.SURFACE, UiColors.HOVER_GREY, UiColors.BORDER);
        startOverButton.setFont(regular.deriveFont(13f));
        startOverButton.setForeground(UiColors.BROWN);
        startOverButton.setPreferredSize(new Dimension(207, 33));
        startOverButton.setMaximumSize(new Dimension(207, 33));
        startOverButton.addActionListener(e -> startOver());

        column.add(orderMoreButton);
        column.add(Box.createVerticalStrut(8));
        column.add(startOverButton);
        return column;
    }

    /** The big orange "Complete Order" button that leads to payment. */
    private JButton buildCompleteButton() {
        JButton button = new RoundedButton(
            "<html><center>"
                + "<font face='" + bold.getFontName() + "' size='6'>Complete</font><br>"
                + "<font face='" + bold.getFontName() + "' size='6'>Order</font>"
                + "</center></html>",
            UiKit.icon("public/assets/icons/check.png", 49, 49),
            UiColors.ORANGE, UiColors.ORANGE_HOVER, null);
        button.setFont(bold.deriveFont(17f));
        button.setForeground(UiColors.WHITE);
        button.setIconTextGap(14);
        // Same height as Order More + gap + Start Over, so both columns line up.
        button.setPreferredSize(new Dimension(310, 98));
        button.setMaximumSize(new Dimension(310, 98));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> completeOrder());
        return button;
    }

    // ------------------------------------------------------------
    // Reusable components used by this page
    // ------------------------------------------------------------

    /** Rounded button with a soft drop shadow, an optional outline and a hover colour. */
    private static class RoundedButton extends JButton {
        private static final int SHADOW = 4;
        private static final int ARC = 8;

        private final Color base;
        private final Color hover;
        private final Color outline;

        RoundedButton(String text, Icon icon, Color base, Color hover, Color outline) {
            super(text, icon);
            this.base = base;
            this.hover = hover;
            this.outline = outline;
            setBackground(base);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(hover); }
                @Override public void mouseExited(MouseEvent e) { setBackground(base); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int faceHeight = getHeight() - SHADOW;

            for (int offset = SHADOW; offset > 0; offset--) {
                g2.setColor(UiColors.shadow(5 + (SHADOW - offset) * 5));
                g2.fillRoundRect(0, offset, getWidth(), faceHeight, ARC, ARC);
            }

            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), faceHeight, ARC, ARC);

            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, faceHeight - 1, ARC, ARC);
            }
            g2.dispose();

            Graphics textGraphics = g.create();
            textGraphics.translate(0, -SHADOW / 2);
            super.paintComponent(textGraphics);
            textGraphics.dispose();
        }
    }

    /** Dashed rounded outline drawn behind the "Your order is empty" message. */
    private static class PlaceholderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UiColors.BORDER);
            g2.setStroke(new BasicStroke(
                1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                10f, new float[]{8f, 6f}, 0f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            g2.dispose();
        }
    }

    /** A panel that always stretches to the width of the scroll viewport. */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return visibleRect.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ============================================================
    // BACKEND - keeping the list and total in step with the cart
    // ============================================================

    /** The cart may have changed on the Menu or Edit Order pages. */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            showCartContents();
        }
    }

    /** Rebuilds the list of order cards from whatever is in the cart right now. */
    private void showCartContents() {
        itemsPanel.removeAll();

        List<Map.Entry<Product, Integer>> entries = new ArrayList<>(cart.getItems().entrySet());
        for (Map.Entry<Product, Integer> entry : entries) {
            addOrderItem(buildCard(entry.getKey(), entry.getValue()));
        }

        itemsPanel.revalidate();
        itemsPanel.repaint();
        updateTotal();
        refreshCenter();
    }

    /** One line of the order: picture, quantity controls, edit and remove. */
    private OrderItemCard buildCard(Product product, int quantity) {
        OrderItemCard card = new OrderItemCard(
            product,
            quantity,
            newQuantity -> {
                // The + / - circles change the real cart, so the Menu total stays in sync too.
                cart.setQuantity(product, newQuantity);
                updateTotal();
            },
            () -> editItem(product),
            null // wired up below, once the card exists
        );
        // Each card removes only itself, not the whole list.
        card.setOnRemove(() -> removeItem(product, card));
        return card;
    }

    /** Puts one order card (plus its spacer) into the scrolling list. */
    private void addOrderItem(JComponent item) {
        itemsPanel.add(item);
        itemsPanel.add(Box.createVerticalStrut(12));
        itemsPanel.revalidate();
        itemsPanel.repaint();
        refreshCenter();
    }

    /** Drops one order card, plus the spacer that follows it. */
    private void removeOrderItem(JComponent item) {
        Component[] children = itemsPanel.getComponents();
        for (int i = 0; i < children.length; i++) {
            if (children[i] == item) {
                itemsPanel.remove(i);
                if (i < itemsPanel.getComponentCount()
                        && itemsPanel.getComponent(i) instanceof Box.Filler) {
                    itemsPanel.remove(i);
                }
                break;
            }
        }
        itemsPanel.revalidate();
        itemsPanel.repaint();
        refreshCenter();
    }

    /** Switches between the empty state and the list. */
    private void refreshCenter() {
        centerLayout.show(centerPanel, itemsPanel.getComponentCount() == 0 ? EMPTY_CARD : LIST_CARD);
    }

    private void updateTotal() {
        totalAmount.setText(UiKit.peso(cart.getTotal()));
    }

    // ------------------------------------------------------------
    // Button actions - KioskMain turns these into page changes
    // ------------------------------------------------------------

    /** "Edit Meal" on a card: KioskMain shows the Edit Order page. */
    private void editItem(Product product) {
        onEditItem.accept(product);
    }

    /** The trash icon on a card: drop that line, take it off the list and refresh the total. */
    private void removeItem(Product product, OrderItemCard card) {
        cart.setQuantity(product, 0);
        removeOrderItem(card);
        updateTotal();
    }

    /** "Order More": back to the menu. */
    private void orderMore() {
        onOrderMore.run();
    }

    /** "Start Over": KioskMain shows the confirmation prompt. */
    private void startOver() {
        onStartOver.run();
    }

    /** "Complete Order": KioskMain shows the payment page (only if there is something to pay for). */
    private void completeOrder() {
        onComplete.run();
    }
}
