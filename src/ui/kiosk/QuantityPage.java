package ui.kiosk;

import java.awt.*;
import javax.swing.*;
import util.Cart;
import util.OrderLine;
import util.Inventory;
import util.Product;
import util.UiColors;
import util.UiKit;

/**
 * Base class for the two pages that show one product with "- / quantity / +"
 * controls: {@link AddOrder} (add a meal to the order) and {@link EditOrder}
 * (change a meal that is already in the order). (Inheritance)
 *
 * The layout, the product card and the quantity rules live here once; the
 * subclasses only supply what differs between the two pages: the heading,
 * the quantity limits, the save button label and what "save" actually does
 * (Polymorphism).
 */
abstract class QuantityPage extends JPanel {

    // ============================================================
    // FRONTEND - header, product card, quantity controls and buttons
    // ============================================================

    /** The pieces of the pattern the subclasses need to see. */
    protected final Product product;
    protected final Cart cart;
    /** The order line being edited; null when a new line is being added. */
    protected final OrderLine line;
    /** Runs after the real cart has been updated. */
    protected final Runnable onSaved;
    /** Runs when the customer backs out without changing anything. */
    private final Runnable onCancel;

    private final Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
    private final Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");

    /** The quantity shown between the minus and plus buttons. */
    protected int quantity = 1;
    /** The read-out between the minus and plus buttons. */
    protected JLabel quantityLabel;
    protected JButton saveButton;
    protected JButton plusButton;

    /**
     * The most of this product the customer may have on this page.
     * AddOrder looks at what is left after what is already in the order,
     * EditOrder simply allows everything that is in stock.
     */
    protected abstract int maxQuantity();

    /** The start of the save button label ("Add to Order", "Save Changes to Order"). */
    protected abstract String saveButtonPrefix();

    /** Writes the chosen quantity into the cart however this page wants. */
    protected abstract void commitOrder();

    /** The quantity the page starts at (a new item starts at 1, an edited one at its current amount). */
    protected int startingQuantity() {
        return 1;
    }

    /** What goes after the two placeholder sections (the layout glue, or more controls). */
    protected abstract void addDetailsFooter(JPanel details);

    /** The bottom row of buttons (Cancel / Save). */
    protected abstract JPanel buildButtons();

    /** The heading shown next to the logo. */
    protected abstract String pageTitle();

    protected QuantityPage(Product product, OrderLine line, Cart cart, Runnable onSaved, Runnable onCancel) {
        this.product = product;
        this.line = line;
        this.cart = cart;
        this.onSaved = onSaved != null ? onSaved : () -> {};
        this.onCancel = onCancel != null ? onCancel : () -> {};

        quantity = Math.max(1, Math.min(startingQuantity(), maxQuantity()));

        setLayout(new BorderLayout(0, 14));
        setBackground(UiColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildDetails(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        updateSaveButton();
        updatePlusButton();
    }

    /** Logo plus the page title. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.add(new JLabel(UiKit.icon("public/assets/logo/logo.png", 86, 86)), BorderLayout.WEST);

        JLabel title = new JLabel(pageTitle());
        title.setFont(bold.deriveFont(29f));
        title.setForeground(UiColors.ORANGE);
        header.add(title, BorderLayout.CENTER);

        header.setPreferredSize(new Dimension(0, 94));
        return header;
    }

    /** The product card followed by the two (still empty) suggestion sections. */
    private JPanel buildDetails() {
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(buildProductCard());
        details.add(Box.createVerticalStrut(16));
        details.add(buildPlaceholderSection("Extras", 82));
        details.add(Box.createVerticalStrut(14));
        details.add(buildPlaceholderSection("Add Gudo’s Favorites with your Cravings", 112));
        addDetailsFooter(details);
        return details;
    }

    /** Picture, name, description, price and the - / quantity / + controls. */
    private JPanel buildProductCard() {
        JPanel card = new JPanel(new BorderLayout(12, 8));
        card.setBackground(UiColors.BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiColors.BORDER),
            BorderFactory.createEmptyBorder(10, 10, 10, 12)));
        card.setPreferredSize(new Dimension(0, 198));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 198));

        JLabel image = new JLabel(UiKit.icon(product.getImagePath(), 150, 150));
        image.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(image, BorderLayout.WEST);

        JPanel info = new JPanel(new BorderLayout(0, 6));
        info.setOpaque(false);

        JTextArea name = new JTextArea(product.getName());
        name.setFont(bold.deriveFont(20f));
        name.setForeground(UiColors.BROWN);
        name.setOpaque(false);
        name.setEditable(false);
        name.setFocusable(false);
        name.setLineWrap(true);
        name.setWrapStyleWord(true);
        name.setBorder(null);
        name.setMargin(new Insets(0, 0, 0, 0));
        name.setRows(2);

        JTextArea description = new JTextArea(
            "A delicious Gudo’s Restobar favorite, freshly prepared for your cravings.");
        description.setFont(regular.deriveFont(13f));
        description.setForeground(UiColors.BROWN);
        description.setOpaque(false);
        description.setEditable(false);
        description.setFocusable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(null);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);

        JLabel price = new JLabel(UiKit.peso(product.getPrice()));
        price.setFont(bold.deriveFont(19f));
        price.setForeground(UiColors.ORANGE);
        bottom.add(price, BorderLayout.WEST);
        bottom.add(buildQuantityControls(), BorderLayout.EAST);

        info.add(name, BorderLayout.NORTH);
        info.add(description, BorderLayout.CENTER);
        info.add(bottom, BorderLayout.SOUTH);
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    /** The minus button, the quantity read-out and the plus button. */
    private JPanel buildQuantityControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        controls.setOpaque(false);

        JButton minus = quantityButton("−");
        plusButton = quantityButton("+");

        quantityLabel = new JLabel(String.valueOf(quantity), SwingConstants.CENTER);
        quantityLabel.setFont(bold.deriveFont(18f));
        quantityLabel.setForeground(UiColors.ORANGE);
        quantityLabel.setPreferredSize(new Dimension(26, 28));

        minus.addActionListener(e -> changeQuantity(-1));
        plusButton.addActionListener(e -> changeQuantity(1));

        controls.add(minus);
        controls.add(quantityLabel);
        controls.add(plusButton);
        return controls;
    }

    /** A titled, still-empty box kept from the original design. */
    private JPanel buildPlaceholderSection(String title, int blankHeight) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setOpaque(false);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, blankHeight + 35));

        JLabel heading = new JLabel(title);
        heading.setFont(bold.deriveFont(19f));
        heading.setForeground(UiColors.BROWN);

        JPanel blank = new JPanel();
        blank.setOpaque(false);
        blank.setPreferredSize(new Dimension(0, blankHeight));
        blank.setBorder(BorderFactory.createLineBorder(UiColors.BORDER));

        section.add(heading, BorderLayout.NORTH);
        section.add(blank, BorderLayout.CENTER);
        return section;
    }

    /** A small orange square holding a white - or +. */
    protected final JButton quantityButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        button.setForeground(UiColors.WHITE);
        button.setBackground(UiColors.ORANGE);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setPreferredSize(new Dimension(32, 32));
        button.setMinimumSize(new Dimension(32, 32));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** The plain bordered button that backs out of the page. */
    protected final JButton createCancelButton(String text) {
        JButton button = new JButton(text);
        button.setFont(regular.deriveFont(14f));
        button.setForeground(UiColors.BROWN);
        button.setBackground(UiColors.BACKGROUND);
        button.setBorder(BorderFactory.createLineBorder(UiColors.BORDER));
        button.setFocusPainted(false);
        return button;
    }

    /** The borderless orange save button; the label updates with the quantity. */
    protected final JButton createSaveButton() {
        JButton button = new JButton();
        button.setFont(bold.deriveFont(14f));
        button.setForeground(UiColors.WHITE);
        button.setBackground(UiColors.ORANGE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        UiKit.hover(button, UiColors.ORANGE, UiColors.ORANGE_HOVER);
        button.addActionListener(e -> saveOrder());
        return button;
    }

    // ============================================================
    // BACKEND - quantity rules, saving and leaving the page
    // ============================================================

    /** Moves the quantity by -1 / +1, keeping it inside 1 .. max. */
    protected void changeQuantity(int change) {
        quantity = Math.min(maxQuantity(), Math.max(1, quantity + change));
        quantityLabel.setText(String.valueOf(quantity));
        updateSaveButton();
        updatePlusButton();
    }

    /** Greys and un-disables the + button once the quantity has reached the limit. */
    protected void updatePlusButton() {
        if (plusButton == null) {
            return;
        }
        boolean canAdd = quantity < maxQuantity();
        plusButton.setEnabled(canAdd);
        plusButton.setBackground(canAdd ? UiColors.ORANGE : UiColors.DISABLED);
        plusButton.setCursor(Cursor.getPredefinedCursor(
            canAdd ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /** Keeps the "• ₱..." label in step with the quantity. */
    protected void updateSaveButton() {
        if (saveButton != null) {
            saveButton.setText(saveButtonPrefix() + " • " + UiKit.peso(product.getPrice() * quantity));
        }
    }

    /** Commits the choice to the real cart, then hands control back. */
    protected final void saveOrder() {
        commitOrder();
        onSaved.run();
    }

    /** Back out: discard the choice and let KioskMain pick the next page. */
    protected final void cancel() {
        onCancel.run();
    }
}