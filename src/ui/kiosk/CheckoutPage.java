package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.List;
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
    /** Room above the first card so the scroll viewport never clips its border. */
    private static final int TOP_PADDING = 4;

    private final Cart cart;
    /** Called with the order line whose "Edit Meal" button was pressed. */
    private final Consumer<OrderLine> onEditItem;
    /** Called when the customer wants to go back to the menu. */
    private final Runnable onOrderMore;
    /** Called when the customer wants to throw the order away. */
    private final Runnable onStartOver;
    /** Called when the customer is ready to pay. */
    private final Runnable onComplete;

    private final Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
    private final Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");
    private final Font inter = UiKit.font("Inter/static/Inter_18pt-Regular.ttf");

    private final CardLayout centerLayout = new CardLayout();
    private final JPanel centerPanel = new JPanel(centerLayout);
    private final JPanel itemsPanel = new ScrollablePanel();
    private final FadeLabel totalAmount = new FadeLabel("\u20B10.00", null, SwingConstants.LEFT);
    /** Greyed out and unclickable while the order is empty. */
    private JButton completeButton;

    // Entrance animation: logo and title, the order cards (or the empty box),
    // then the total. The bottom buttons are always there.
    private static final int FADE_MILLIS = 250;
    private static final int STAGGER_MILLIS = 100;
    private final FadeInSequence entrance = new FadeInSequence(this, FADE_MILLIS, STAGGER_MILLIS);
    private final PlaceholderPanel placeholder = new PlaceholderPanel();
    private FadeLabel logo;
    private FadeLabel title;
    private FadeLabel totalCaption;

    public CheckoutPage(Cart cart, Runnable onOrderMore, Runnable onStartOver,
                        Runnable onComplete, Consumer<OrderLine> onEditItem) {
        this.cart = cart != null ? cart : new Cart();
        this.onOrderMore = onOrderMore != null ? onOrderMore : () -> {};
        this.onStartOver = onStartOver != null ? onStartOver : () -> {};
        this.onComplete = onComplete != null ? onComplete : () -> {};
        this.onEditItem = onEditItem != null ? onEditItem : line -> {};

        setLayout(new BorderLayout());
        setBackground(UiColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 30, 28, 30));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        // Rebuild the list and replay the entrance every time the page is shown;
        // hiding it puts everything back to invisible for the next visit.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                showCartContents();
                entrance.start();
            } else {
                entrance.reset();
            }
        });
    }

    /** Logo plus the page title. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 33, 0));

        logo = new FadeLabel(UiKit.icon("public/assets/logo/logo-circle.png", 96, 96));
        header.add(logo);

        title = new FadeLabel("Your Order", null, SwingConstants.LEFT);
        title.setFont(bold.deriveFont(30.5f));
        title.setForeground(UiColors.ORANGE);
        title.setBorder(BorderFactory.createEmptyBorder(0, 17, 0, 0));
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
        itemsPanel.setBorder(BorderFactory.createEmptyBorder(TOP_PADDING, 0, 0, 0)); // keeps the first card's top border visible

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
        SmoothScroller.install(scrollPane);
        return scrollPane;
    }

    /** Total on the left with Order More / Start Over, amount and Pay on the right. */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(11, 0));
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
        totalCaption = new FadeLabel("Total:", null, SwingConstants.LEFT);
        totalCaption.setFont(bold.deriveFont(20.5f));
        totalCaption.setForeground(UiColors.ORANGE);
        totalCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalCaption.setPreferredSize(new Dimension(198, 32));
        totalCaption.setMaximumSize(new Dimension(198, 32));
        return totalCaption;
    }

    private JLabel buildAmountLabel() {
        totalAmount.setFont(bold.deriveFont(20.5f));
        totalAmount.setForeground(UiColors.ORANGE);
        totalAmount.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalAmount.setPreferredSize(new Dimension(296, 32));
        totalAmount.setMaximumSize(new Dimension(296, 32));
        return totalAmount;
    }

    /** "Order More" on top of "Start Over", both full width of the left column. */
    private JPanel buildSmallButtons() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton orderMoreButton = new RoundedButton("Order More", null,
            UiColors.SURFACE, UiColors.HOVER_GREY, UiColors.BROWN);
        orderMoreButton.setFont(inter.deriveFont(12.5f));
        orderMoreButton.setForeground(UiColors.BROWN);
        orderMoreButton.setPreferredSize(new Dimension(198, 59));
        orderMoreButton.setMaximumSize(new Dimension(198, 59));
        orderMoreButton.addActionListener(e -> orderMore());

        JButton startOverButton = new RoundedButton("Start Over", null,
            UiColors.SURFACE, UiColors.HOVER_GREY, UiColors.BROWN);
        startOverButton.setFont(inter.deriveFont(12.5f));
        startOverButton.setForeground(UiColors.BROWN);
        startOverButton.setPreferredSize(new Dimension(198, 36));
        startOverButton.setMaximumSize(new Dimension(198, 36));
        startOverButton.addActionListener(e -> startOver());

        column.add(orderMoreButton);
        column.add(startOverButton);
        return column;
    }

    /** The big orange "Complete Order" button that leads to payment. */
    private JButton buildCompleteButton() {
        JButton button = new RoundedButton(null, null,
            UiColors.ORANGE, UiColors.ORANGE_HOVER, null);
        completeButton = button;

        // Icon and two-line text as real components: an HTML label would
        // ignore the Montserrat font and its size.
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(buildCompleteLine("Complete"));
        text.add(buildCompleteLine("Order"));

        button.setLayout(new BoxLayout(button, BoxLayout.X_AXIS));
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0)); // centre on the face, above the shadow
        button.add(Box.createHorizontalGlue());
        button.add(new JLabel(UiKit.icon("public/assets/icons/check.png", 47, 47)));
        button.add(Box.createHorizontalStrut(25));
        button.add(text);
        button.add(Box.createHorizontalGlue());

        // Same height as Order More + Start Over, so both columns line up.
        button.setPreferredSize(new Dimension(296, 95));
        button.setMaximumSize(new Dimension(296, 95));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> completeOrder());
        return button;
    }

    private JLabel buildCompleteLine(String line) {
        JLabel label = new JLabel(line);
        label.setFont(bold.deriveFont(18f));
        label.setForeground(UiColors.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    // ------------------------------------------------------------
    // Reusable components used by this page
    // ------------------------------------------------------------

    /** Dashed rounded outline drawn behind the "Your order is empty" message. */
    private static class PlaceholderPanel extends FadeablePanel {
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

    /** Rebuilds the list of order cards from whatever is in the cart right now. */
    private void showCartContents() {
        itemsPanel.removeAll();

        // Entrance order: logo and title, then each card top to bottom.
        entrance.clear();
        entrance.add(alpha -> {
            logo.setFade(alpha);
            title.setFade(alpha);
        });

        List<OrderLine> lines = cart.getLines();
        for (OrderLine line : lines) {
            OrderItemCard card = buildCard(line);
            addOrderItem(card);
            entrance.add(card::setFade);
        }

        // The empty box fades in only when there is nothing in the order;
        // otherwise it must stay fully visible for when the last item is removed.
        placeholder.setFade(1f);
        if (lines.isEmpty()) {
            entrance.add(placeholder::setFade);
        }

        // Last: the total and its price.
        entrance.add(alpha -> {
            totalCaption.setFade(alpha);
            totalAmount.setFade(alpha);
        });

        itemsPanel.revalidate();
        itemsPanel.repaint();
        updateTotal();
        refreshCenter();
    }

    /** One line of the order: picture, quantity controls, edit and remove. */
    private OrderItemCard buildCard(OrderLine line) {
        OrderItemCard card = new OrderItemCard(
            line.getProduct(),
            line.getQuantity(),
            () -> cart.maxQuantity(line),
            line.getExtras(),
            line.getFavorites(),
            newQuantity -> {
                // The + / - circles change the real cart, so the Menu total stays in sync too.
                cart.setQuantity(line, newQuantity);
                updateTotal();
            },
            () -> editItem(line),
            null // wired up below, once the card exists
        );
        // Each card removes only itself, not the whole list.
        card.setOnRemove(() -> removeItem(line, card));
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
        completeButton.setEnabled(!cart.isEmpty()); // nothing to pay for
    }

    // ------------------------------------------------------------
    // Button actions - KioskMain turns these into page changes
    // ------------------------------------------------------------

    /** "Edit Meal" on a card: KioskMain shows the Edit Order page. */
    private void editItem(OrderLine line) {
        onEditItem.accept(line);
    }

    /** The trash icon on a card: drop that line, take it off the list and refresh the total. */
    private void removeItem(OrderLine line, OrderItemCard card) {
        cart.remove(line);
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