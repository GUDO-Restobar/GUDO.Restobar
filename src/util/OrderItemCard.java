package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.function.IntConsumer;
import javax.swing.*;

public class OrderItemCard extends JPanel {

    private static final Color ORANGE = new Color(236, 94, 2);
    private static final Color ORANGE_HOVER = new Color(255, 120, 35);
    private static final Color DARK_TEXT = new Color(43, 43, 43);
    private static final Color ADDON_TEXT = new Color(90, 90, 90);
    private static final Color CARD_BORDER = new Color(226, 226, 226);
    private static final Color BUTTON_BORDER = new Color(190, 190, 190);
    private static final Color BUTTON_HOVER = new Color(242, 242, 242);
    private static final Color CONFIRM_RED = new Color(198, 40, 40); // #C62828

    private static final String TRASH_ICON = "public/assets/icons/trash.png";
    private static final int TRASH_SIZE = 23;

    /** Shown next to the trash icon after the first press. */
    private static final String CONFIRM_TEXT = "Are you sure?";

    private static final int CARD_HEIGHT = 200;
    private static final int ARC = 14;
    private static final int SHADOW = 3;

    private final Font bold = loadFont("Montserrat-Bold.ttf");
    private final Font regular = loadFont("Montserrat-Regular.ttf");

    /** Set by the first press of the trash icon. */
    private boolean confirming;

    /** Needed to place the confirm text right beside the icon. */
    private JButton trashButton;

    /** Runs on the second press of the trash icon. */
    private Runnable onRemove = () -> {};

    private final Product product;
    private final IntConsumer onQuantityChange;
    private int quantity;
    private JLabel quantityLabel;
    private JLabel priceLabel;

    /**
     * @param product          the product this card shows
     * @param quantity         how many of it are in the order
     * @param onQuantityChange called with the new quantity after the + or - circle is pressed
     * @param onEdit           runs when "Edit Meal" is pressed
     * @param onRemove         runs on the second press of the trash icon
     */
    public OrderItemCard(Product product,
                         int quantity,
                         IntConsumer onQuantityChange,
                         Runnable onEdit,
                         Runnable onRemove) {
        this(product, quantity, new String[0], new String[0], onQuantityChange, onEdit, onRemove);
    }

    public OrderItemCard(Product product,
                         int quantity,
                         String[] leftAddOns,
                         String[] rightAddOns,
                         IntConsumer onQuantityChange,
                         Runnable onEdit,
                         Runnable onRemove) {

        this.product = product;
        this.quantity = Math.max(1, quantity);
        this.onQuantityChange = onQuantityChange != null ? onQuantityChange : q -> {};
        Runnable edit = onEdit != null ? onEdit : () -> {};
        this.onRemove = onRemove != null ? onRemove : () -> {};

        setLayout(new BorderLayout(18, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16 + SHADOW, 20));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        add(buildImage(product.getImagePath()), BorderLayout.WEST);
        add(buildInfo(product.getName(), leftAddOns, rightAddOns, edit), BorderLayout.CENTER);
        add(buildRightColumn(), BorderLayout.EAST);

        setPreferredSize(new Dimension(760, CARD_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, CARD_HEIGHT));
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    /** Applies a +1 / -1 change. The quantity never drops below 1; the trash icon removes the item. */
    private void changeQuantity(int change) {
        int updated = Math.max(1, quantity + change);
        if (updated == quantity) {
            return;
        }
        quantity = updated;
        quantityLabel.setText(String.valueOf(quantity));
        priceLabel.setText(formatPrice(product.getPrice() * quantity));
        onQuantityChange.accept(quantity);
    }

    private static String formatPrice(double price) {
        return "\u20B1" + String.format("%,.2f", price);
    }

    /** Lets the owner decide what "remove" means for this specific card. */
    public void setOnRemove(Runnable action) {
        this.onRemove = action != null ? action : () -> {};
    }

    private JLabel buildImage(String imagePath) {
        JLabel image = new JLabel(scaledIcon(imagePath, 130, 130));
        image.setVerticalAlignment(SwingConstants.CENTER);
        return image;
    }

    private JPanel buildInfo(String name,
                             String[] leftAddOns, String[] rightAddOns,
                             Runnable onEdit) {
        JPanel info = new JPanel(new BorderLayout(0, 8));
        info.setOpaque(false);
        info.add(buildHeader(name), BorderLayout.NORTH);
        info.add(buildAddOns(leftAddOns, rightAddOns), BorderLayout.CENTER);
        info.add(buildEditRow(onEdit), BorderLayout.SOUTH);
        return info;
    }

    private JPanel buildHeader(String name) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(bold.deriveFont(20f));
        nameLabel.setForeground(DARK_TEXT);

        quantityLabel = new JLabel(String.valueOf(quantity));
        quantityLabel.setFont(bold.deriveFont(20f));
        quantityLabel.setForeground(ORANGE);

        CircleButton minus = new CircleButton(false);
        minus.addActionListener(e -> changeQuantity(-1));
        CircleButton plus = new CircleButton(true);
        plus.addActionListener(e -> changeQuantity(1));

        header.add(nameLabel);
        header.add(Box.createHorizontalStrut(14));
        header.add(minus);
        header.add(Box.createHorizontalStrut(5));
        header.add(quantityLabel);
        header.add(Box.createHorizontalStrut(5));
        header.add(plus);
        return header;
    }

    private JPanel buildAddOns(String[] leftAddOns, String[] rightAddOns) {
        JPanel columns = new JPanel();
        columns.setLayout(new BoxLayout(columns, BoxLayout.X_AXIS));
        columns.setOpaque(false);

        columns.add(buildAddOnColumn(leftAddOns));
        columns.add(Box.createHorizontalStrut(40));
        columns.add(buildAddOnColumn(rightAddOns));
        columns.add(Box.createHorizontalGlue());
        return columns;
    }

    private JPanel buildAddOnColumn(String[] addOns) {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setAlignmentY(Component.TOP_ALIGNMENT);
        column.setPreferredSize(new Dimension(240, 100));
        column.setMaximumSize(new Dimension(240, Integer.MAX_VALUE));

        if (addOns != null) {
            for (String addOn : addOns) {
                JLabel label = new JLabel("\u2022 " + addOn);
                label.setFont(regular.deriveFont(11f));
                label.setForeground(ADDON_TEXT);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
                column.add(label);
            }
        }
        column.add(Box.createVerticalGlue());
        return column;
    }

    private JPanel buildEditRow(Runnable onEdit) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);

        OutlineButton edit = new OutlineButton("Edit Meal");
        edit.setFont(regular.deriveFont(14f));
        edit.setForeground(DARK_TEXT);
        edit.setPreferredSize(new Dimension(145, 25));
        edit.addActionListener(e -> onEdit.run());

        row.add(edit);
        return row;
    }

    private JPanel buildRightColumn() {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);

        priceLabel = new JLabel(formatPrice(product.getPrice() * quantity));
        priceLabel.setFont(bold.deriveFont(15f));
        priceLabel.setForeground(ORANGE);

        JPanel priceRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        priceRow.setOpaque(false);
        priceRow.add(priceLabel);
        column.add(priceRow, BorderLayout.NORTH);

        JButton remove = new JButton(scaledIcon(TRASH_ICON, TRASH_SIZE, TRASH_SIZE));
        remove.setOpaque(false);
        remove.setContentAreaFilled(false);
        remove.setBorderPainted(false);
        remove.setFocusPainted(false);
        remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        remove.setToolTipText("Remove item");
        remove.addActionListener(e -> {
            if (confirming) {
                onRemove.run();
            } else {
                // First press only asks; the second one removes this card.
                confirming = true;
                repaint();
            }
        });
        trashButton = remove;

        JPanel trashRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        trashRow.setOpaque(false);
        trashRow.add(remove);
        column.add(trashRow, BorderLayout.SOUTH);

        return column;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int faceHeight = getHeight() - SHADOW;

        for (int offset = SHADOW; offset > 0; offset--) {
            g2.setColor(new Color(0, 0, 0, 4 + (SHADOW - offset) * 3));
            g2.fillRoundRect(0, offset, getWidth() - 1, faceHeight, ARC, ARC);
        }

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth() - 1, faceHeight, ARC, ARC);

        g2.setColor(CARD_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, getWidth() - 2, faceHeight - 1, ARC, ARC);

        // Drawn instead of added as a component, so showing it never re-lays
        // out the card and never squeezes the add-on columns.
        if (confirming && trashButton != null) {
            Point icon = SwingUtilities.convertPoint(trashButton, 0, 0, this);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(bold.deriveFont(11f));
            g2.setColor(CONFIRM_RED);
            FontMetrics fm = g2.getFontMetrics();
            int x = icon.x - 8 - fm.stringWidth(CONFIRM_TEXT);
            int y = icon.y + (trashButton.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(CONFIRM_TEXT, x, y);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private static Font loadFont(String file) {
        try {
            return FontLoader.loadFont("public/assets/fonts/Montserrat/static/" + file);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load font: " + file, e);
        }
    }

    private static ImageIcon scaledIcon(String path, int width, int height) {
        Image image = new ImageIcon(path).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    /** Small orange circle holding a white minus or plus. */
    private static class CircleButton extends JButton {
        private final boolean plus;
        private boolean hover;

        CircleButton(boolean plus) {
            this.plus = plus;
            setPreferredSize(new Dimension(30, 30));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(hover ? ORANGE_HOVER : ORANGE);
            g2.fillOval(0, 0, 25, 25);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(8, 13, 18, 13);
            if (plus) {
                g2.drawLine(13, 8, 13, 18);
            }
            g2.dispose();
        }
    }

    /** White button with a thin grey outline, used for "Edit Meal". */
    private static class OutlineButton extends JButton {
        private boolean hover;

        OutlineButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hover ? BUTTON_HOVER : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            g2.setColor(BUTTON_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
