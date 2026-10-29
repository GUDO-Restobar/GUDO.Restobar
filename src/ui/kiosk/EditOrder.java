package ui.kiosk;

import java.awt.*;
import java.io.IOException;
import javax.swing.*;
import util.Cart;
import util.FontLoader;
import util.Product;

/**
 * Lets the customer change the quantity of a meal that is already in the order.
 *
 * Uses the Prototype pattern: the screen works on a copy of the cart
 * (cart.copy()), so nothing in the real order changes until the customer
 * presses "Save Changes to Order". "Go Back to Order" simply throws the copy away.
 */
public class EditOrder extends JPanel {
    private static final Color ORANGE = new Color(236, 94, 2);
    private static final Color HOVER = new Color(255, 120, 35);
    private static final Color BROWN = new Color(74, 45, 35);
    private static final Color OFF_WHITE = new Color(246, 246, 246);
    private static final Color LINE = new Color(200, 200, 200);

    private final Product product;
    private final Cart cart;
    private final Cart draft;
    private final Runnable onSaved;
    private final Runnable onCancel;
    private final Font bold = loadFont("Montserrat/static/Montserrat-Bold.ttf");
    private final Font regular = loadFont("Montserrat/static/Montserrat-Regular.ttf");
    private int quantity;
    private JLabel quantityLabel;
    private JButton saveButton;

    public EditOrder(Product product, Cart cart, Runnable onSaved, Runnable onCancel) {
        this.product = product;
        this.cart = cart;
        this.draft = cart.copy();
        this.onSaved = onSaved != null ? onSaved : () -> {};
        this.onCancel = onCancel != null ? onCancel : () -> {};

        Integer currentQuantity = draft.getItems().get(product);
        quantity = currentQuantity != null ? currentQuantity : 1;

        setLayout(new BorderLayout(0, 14));
        setBackground(OFF_WHITE);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildDetails(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        updateSaveButton();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JLabel logo = new JLabel(scaledIcon("public/assets/logo/logo.png", 86, 86));
        JLabel title = new JLabel("<html>Tweak Your Order<br>Your Way</html>");
        title.setFont(bold.deriveFont(29f));
        title.setForeground(ORANGE);
        header.add(logo, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.setPreferredSize(new Dimension(0, 94));
        return header;
    }

    private JPanel buildDetails() {
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(buildProductCard());
        details.add(Box.createVerticalStrut(16));
        details.add(buildPlaceholderSection("Extras", 82));
        details.add(Box.createVerticalStrut(14));
        details.add(buildPlaceholderSection("Add Gudo’s Favorites with your Cravings", 112));
        details.add(Box.createVerticalGlue());
        return details;
    }

    private JPanel buildProductCard() {
        JPanel card = new JPanel(new BorderLayout(12, 8));
        card.setBackground(OFF_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE),
            BorderFactory.createEmptyBorder(10, 10, 10, 12)));
        card.setPreferredSize(new Dimension(0, 198));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 198));

        JLabel image = new JLabel(scaledIcon(product.getImagePath(), 150, 150));
        image.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(image, BorderLayout.WEST);

        JPanel info = new JPanel(new BorderLayout(0, 6));
        info.setOpaque(false);

        JTextArea name = new JTextArea(product.getName());
        name.setFont(bold.deriveFont(20f));
        name.setForeground(BROWN);
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
        description.setForeground(BROWN);
        description.setOpaque(false);
        description.setEditable(false);
        description.setFocusable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(null);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        JLabel price = new JLabel(formatPrice(product.getPrice()));
        price.setFont(bold.deriveFont(19f));
        price.setForeground(ORANGE);
        bottom.add(price, BorderLayout.WEST);
        bottom.add(buildQuantityControls(), BorderLayout.EAST);

        info.add(name, BorderLayout.NORTH);
        info.add(description, BorderLayout.CENTER);
        info.add(bottom, BorderLayout.SOUTH);
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildQuantityControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        controls.setOpaque(false);
        JButton minus = quantityButton("−");
        JButton plus = quantityButton("+");
        quantityLabel = new JLabel(String.valueOf(quantity), SwingConstants.CENTER);
        quantityLabel.setFont(bold.deriveFont(18f));
        quantityLabel.setForeground(ORANGE);
        quantityLabel.setPreferredSize(new Dimension(26, 28));

        minus.addActionListener(e -> changeQuantity(-1));
        plus.addActionListener(e -> changeQuantity(1));
        controls.add(minus);
        controls.add(quantityLabel);
        controls.add(plus);
        return controls;
    }

    private JPanel buildPlaceholderSection(String title, int blankHeight) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setOpaque(false);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, blankHeight + 35));
        JLabel heading = new JLabel(title);
        heading.setFont(bold.deriveFont(19f));
        heading.setForeground(BROWN);
        JPanel blank = new JPanel();
        blank.setOpaque(false);
        blank.setPreferredSize(new Dimension(0, blankHeight));
        blank.setBorder(BorderFactory.createLineBorder(LINE));
        section.add(heading, BorderLayout.NORTH);
        section.add(blank, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildButtons() {
        // "Go Back" is narrower than "Save Changes", like the design (about 40% / 60%),
        // so a long price such as ₱1,074.00 still fits on the save button.
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(0, 56));

        JButton cancel = new JButton("Go Back to Order");
        cancel.setFont(regular.deriveFont(14f));
        cancel.setForeground(BROWN);
        cancel.setBackground(OFF_WHITE);
        cancel.setBorder(BorderFactory.createLineBorder(LINE));
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> this.onCancel.run());

        saveButton = new JButton();
        saveButton.setFont(bold.deriveFont(14f));
        saveButton.setForeground(Color.WHITE);
        saveButton.setBackground(ORANGE);
        saveButton.setBorderPainted(false);
        saveButton.setFocusPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { saveButton.setBackground(HOVER); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { saveButton.setBackground(ORANGE); }
        });
        saveButton.addActionListener(e -> saveOrder());

        saveButton.setMargin(new Insets(0, 6, 0, 6));
        cancel.setMargin(new Insets(0, 6, 0, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 0.4;
        gbc.insets = new Insets(0, 0, 0, 6);
        buttons.add(cancel, gbc);
        gbc.weightx = 0.6;
        gbc.insets = new Insets(0, 6, 0, 0);
        buttons.add(saveButton, gbc);
        return buttons;
    }

    private void changeQuantity(int change) {
        quantity = Math.max(1, quantity + change);
        quantityLabel.setText(String.valueOf(quantity));
        updateSaveButton();
    }

    private void updateSaveButton() {
        if (saveButton != null) {
            saveButton.setText("Save Changes to Order • " + formatPrice(product.getPrice() * quantity));
        }
    }

    private void saveOrder() {
        // Edit the copy first, then commit it to the real cart in one step.
        draft.setQuantity(product, quantity);
        cart.replaceWith(draft);
        onSaved.run();
    }

    private JButton quantityButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        button.setForeground(Color.WHITE);
        button.setBackground(ORANGE);
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

    private static String formatPrice(double price) {
        return "₱" + String.format("%,.2f", price);
    }

    private static ImageIcon scaledIcon(String path, int width, int height) {
        Image image = new ImageIcon(path).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private static Font loadFont(String file) {
        try {
            return FontLoader.loadFont("public/assets/fonts/" + file);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load font: " + file, e);
        }
    }
}
