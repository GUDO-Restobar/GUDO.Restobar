package ui.kiosk;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import util.*;

public class CheckoutPage extends JPanel {

    private static final Color ORANGE = new Color(236, 94, 2);
    private static final Color HOVER = new Color(255, 120, 35);
    private static final Color DARK_ORANGE = new Color(204, 80, 0);
    private static final Color BROWN = new Color(74, 45, 35);
    private static final Color OFF_WHITE = new Color(246, 246, 246);
    private static final Color LINE = new Color(200, 200, 200);

    private final Cart cart;
    private final Consumer<Product> onEditItem;
    private final Font bold = loadFont("Montserrat-Bold.ttf");
    private final Font regular = loadFont("Montserrat-Regular.ttf");

    private static final String EMPTY_CARD = "empty";
    private static final String LIST_CARD = "list";

    private final CardLayout centerLayout = new CardLayout();
    private final JPanel centerPanel = new JPanel(centerLayout);
    private final JPanel itemsPanel = new ScrollablePanel();
    private final JLabel totalAmount = new JLabel("\u20B10.00");

    public CheckoutPage(Cart cart) {
        this(cart, null, null, null, null);
    }

    public CheckoutPage(Cart cart, Runnable onOrderMore, Runnable onStartOver, Runnable onComplete) {
        this(cart, onOrderMore, onStartOver, onComplete, null);
    }

    /**
     * @param onEditItem called with the product whose "Edit Meal" button was pressed
     */
    public CheckoutPage(Cart cart, Runnable onOrderMore, Runnable onStartOver,
                        Runnable onComplete, Consumer<Product> onEditItem) {
        this.cart = cart != null ? cart : new Cart();
        this.onEditItem = onEditItem != null ? onEditItem : product -> {};

        Runnable orderMore = onOrderMore != null ? onOrderMore : () -> {};
        Runnable startOver = onStartOver != null ? onStartOver : () -> {};
        Runnable complete = onComplete != null ? onComplete : () -> {};

        setLayout(new BorderLayout());
        setBackground(OFF_WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(orderMore, startOver, complete), BorderLayout.SOUTH);

        showCartContents();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            // The cart may have changed on the Menu or Edit Order pages.
            showCartContents();
        }
    }

    private void updateTotal() {
        totalAmount.setText("\u20B1" + String.format("%,.2f", cart.getTotal()));
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

    private OrderItemCard buildCard(Product product, int quantity) {
        OrderItemCard card = new OrderItemCard(
            product,
            quantity,
            newQuantity -> {
                // The + / - circles change the real cart, so the Menu total stays in sync too.
                cart.setQuantity(product, newQuantity);
                updateTotal();
            },
            () -> onEditItem.accept(product),
            null
        );
        // Each card removes only itself, not the whole list.
        card.setOnRemove(() -> {
            cart.setQuantity(product, 0);
            removeOrderItem(card);
            updateTotal();
        });
        return card;
    }

    public void addOrderItem(JComponent item) {
        itemsPanel.add(item);
        itemsPanel.add(Box.createVerticalStrut(12));
        itemsPanel.revalidate();
        itemsPanel.repaint();
        refreshCenter();
    }

    /** Removes one order card, plus the spacer that follows it. */
    public void removeOrderItem(JComponent item) {
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

    public void clearOrderItems() {
        itemsPanel.removeAll();
        itemsPanel.revalidate();
        itemsPanel.repaint();
        refreshCenter();
    }

    private void refreshCenter() {
        centerLayout.show(centerPanel, itemsPanel.getComponentCount() == 0 ? EMPTY_CARD : LIST_CARD);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel logo = new JLabel(scaledIcon("public/assets/logo/logo-circle.png", 92, 92));

        JLabel title = new JLabel("Your Order");
        title.setFont(bold.deriveFont(34f));
        title.setForeground(ORANGE);

        header.add(logo);
        header.add(title);
        return header;
    }

    private JComponent buildCenter() {
        centerPanel.setOpaque(false);
        centerPanel.add(buildEmptyState(), EMPTY_CARD);
        centerPanel.add(buildListArea(), LIST_CARD);
        return centerPanel;
    }

    private JPanel buildEmptyState() {
        JPanel placeholder = new PlaceholderPanel();
        placeholder.setLayout(new GridBagLayout());
        placeholder.setOpaque(false);
        placeholder.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel message = new JLabel("Your order is empty", SwingConstants.CENTER);
        message.setFont(regular.deriveFont(16f));
        message.setForeground(new Color(150, 150, 150));
        placeholder.add(message);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 8, 24, 8));
        wrapper.add(placeholder, BorderLayout.CENTER);
        return wrapper;
    }

    private JScrollPane buildListArea() {
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    private JPanel buildFooter(Runnable orderMore, Runnable startOver, Runnable complete) {
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setOpaque(false);

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.add(buildTotalLabel());
        leftColumn.add(Box.createVerticalStrut(14));
        leftColumn.add(buildSmallButtons(orderMore, startOver));

        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.add(buildAmountLabel());
        rightColumn.add(Box.createVerticalStrut(14));
        rightColumn.add(buildCompleteButton(complete));

        footer.add(leftColumn, BorderLayout.WEST);
        footer.add(rightColumn, BorderLayout.CENTER);
        return footer;
    }

    private JLabel buildTotalLabel() {
        JLabel label = new JLabel("Total:");
        label.setFont(bold.deriveFont(22f));
        label.setForeground(ORANGE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setPreferredSize(new Dimension(207, 32));
        label.setMaximumSize(new Dimension(207, 32));
        return label;
    }

    private JLabel buildAmountLabel() {
        totalAmount.setFont(bold.deriveFont(22f));
        totalAmount.setForeground(ORANGE);
        totalAmount.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalAmount.setPreferredSize(new Dimension(310, 32));
        totalAmount.setMaximumSize(new Dimension(310, 32));
        return totalAmount;
    }

    private JPanel buildSmallButtons(Runnable orderMore, Runnable startOver) {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton orderMoreButton = new RoundedButton(
            "Order More", null, Color.WHITE, new Color(238, 238, 238), LINE);
        orderMoreButton.setFont(regular.deriveFont(13f));
        orderMoreButton.setForeground(BROWN);
        orderMoreButton.setPreferredSize(new Dimension(207, 57));
        orderMoreButton.setMaximumSize(new Dimension(207, 57));
        orderMoreButton.addActionListener(e -> orderMore.run());

        JButton startOverButton = new RoundedButton(
            "Start Over", null, Color.WHITE, new Color(238, 238, 238), LINE);
        startOverButton.setFont(regular.deriveFont(13f));
        startOverButton.setForeground(BROWN);
        startOverButton.setPreferredSize(new Dimension(207, 33));
        startOverButton.setMaximumSize(new Dimension(207, 33));
        startOverButton.addActionListener(e -> startOver.run());

        column.add(orderMoreButton);
        column.add(Box.createVerticalStrut(8));
        column.add(startOverButton);
        return column;
    }

    private JButton buildCompleteButton(Runnable complete) {
        ImageIcon checkIcon = scaledIcon("public/assets/icons/check.png", 49, 49);

        JButton button = new RoundedButton(
            "<html><center>"
                + "<font face='" + bold.getFontName() +"' size='6'>Complete</font><br>"
                + "<font face='" + bold.getFontName() +"' size='6'>Order</font>"
                + "</center></html>",
            checkIcon,
            ORANGE, HOVER, null);
        button.setFont(bold.deriveFont(17f));
        button.setForeground(Color.WHITE);
        button.setIconTextGap(14);
        // Same height as Order More + gap + Start Over, so both columns line up.
        button.setPreferredSize(new Dimension(310, 98));
        button.setMaximumSize(new Dimension(310, 98));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> complete.run());
        return button;
    }

    private static Font loadFont(String file) {
        try {
            return FontLoader.loadFont("public/assets/fonts/Montserrat/static/" + file);
        } catch (java.io.IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load font: " + file, e);
        }
    }

    private static ImageIcon scaledIcon(String path, int width, int height) {
        Image image = new ImageIcon(path).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

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
                int alpha = 5 + (SHADOW - offset) * 5;
                g2.setColor(new Color(0, 0, 0, alpha));
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

    private static class PlaceholderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(200, 200, 200));
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
}
