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
    private static final Color DISABLED_GREY = new Color(200, 200, 200);

    /** Height of the name / quantity row. Matches the 30px circle buttons. */
    private static final int HEADER_HEIGHT = 30;
    private static final int NAME_GAP = 14;

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
    private CircleButton plusButton;

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
        int updated = Math.min(maxQuantity(), Math.max(1, quantity + change));
        if (updated == quantity) {
            return;
        }
        quantity = updated;
        quantityLabel.setText(String.valueOf(quantity));
        priceLabel.setText(formatPrice(product.getPrice() * quantity));
        updatePlusButton();
        onQuantityChange.accept(quantity);
    }

    /** The most of this product the customer can have: whatever is in stock. */
    private int maxQuantity() {
        return Math.max(1, Inventory.getStock(product));
    }

    /** Disables the + circle once the quantity has reached the stock. */
    private void updatePlusButton() {
        if (plusButton != null) {
            plusButton.setEnabled(quantity < maxQuantity());
        }
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
        JPanel header = new JPanel(new HeaderLayout());
        header.setOpaque(false);

        MealNameLabel nameLabel = new MealNameLabel(name, bold, DARK_TEXT);

        quantityLabel = new JLabel(String.valueOf(quantity));
        quantityLabel.setFont(bold.deriveFont(20f));
        quantityLabel.setForeground(ORANGE);

        CircleButton minus = new CircleButton(false);
        minus.addActionListener(e -> changeQuantity(-1));
        CircleButton plus = new CircleButton(true);
        plus.addActionListener(e -> changeQuantity(1));
        plusButton = plus;
        updatePlusButton();

        // Same controls and spacing as before, just grouped so the name can
        // use whatever width is left over.
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        controls.setOpaque(false);
        controls.add(minus);
        controls.add(Box.createHorizontalStrut(5));
        controls.add(quantityLabel);
        controls.add(Box.createHorizontalStrut(5));
        controls.add(plus);

        header.add(nameLabel);
        header.add(controls);
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

    /**
     * Lays out the name followed by the quantity controls, exactly like the old
     * left-aligned row. The name only gets the width that is left after the
     * controls, so a long name can never push the controls out of the card.
     */
    private static class HeaderLayout implements LayoutManager {
        @Override public void addLayoutComponent(String name, Component comp) {}
        @Override public void removeLayoutComponent(Component comp) {}

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(0, HEADER_HEIGHT);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0, HEADER_HEIGHT);
        }

        @Override
        public void layoutContainer(Container parent) {
            MealNameLabel name = (MealNameLabel) parent.getComponent(0);
            Component controls = parent.getComponent(1);

            Dimension controlsSize = controls.getPreferredSize();
            int available = parent.getWidth() - controlsSize.width - NAME_GAP;

            name.fitTo(available);
            Dimension nameSize = name.getPreferredSize();

            int nameY = (HEADER_HEIGHT - nameSize.height) / 2;
            int controlsY = (HEADER_HEIGHT - controlsSize.height) / 2;
            name.setBounds(0, nameY, nameSize.width, nameSize.height);
            controls.setBounds(nameSize.width + NAME_GAP, controlsY,
                controlsSize.width, controlsSize.height);
        }
    }

    /**
     * The meal name. Keeps the normal 20pt look when it fits; otherwise it
     * shrinks, then wraps onto two lines (still inside the row height), and
     * only as a last resort is cut with "...".
     */
    private static class MealNameLabel extends JComponent {
        private static final float NORMAL_SIZE = 20f;
        private static final float MIN_SINGLE_LINE_SIZE = 16f;
        private static final float MIN_SIZE = 9f;

        private final String text;
        private final Font baseFont;
        private String[] lines;
        private Font font;

        MealNameLabel(String text, Font baseFont, Color color) {
            this.text = text == null ? "" : text.trim();
            this.baseFont = baseFont;
            setForeground(color);
            setToolTipText(this.text);
            fitTo(10_000);
        }

        /** Chooses the font size and line breaks so the name fits in the given width. */
        void fitTo(int width) {
            int maxWidth = Math.max(40, width);

            // 1) Normal size down to 16pt on a single line.
            for (float size = NORMAL_SIZE; size >= MIN_SINGLE_LINE_SIZE; size--) {
                Font candidate = baseFont.deriveFont(size);
                if (getFontMetrics(candidate).stringWidth(text) <= maxWidth) {
                    apply(candidate, new String[] {text});
                    return;
                }
            }

            // 2) Two lines, as large as the row height and width allow.
            for (float size = 15f; size >= MIN_SIZE; size--) {
                Font candidate = baseFont.deriveFont(size);
                FontMetrics fm = getFontMetrics(candidate);
                if (fm.getHeight() * 2 > HEADER_HEIGHT) {
                    continue;
                }
                String[] wrapped = wrap(text, fm, maxWidth);
                if (wrapped != null) {
                    apply(candidate, wrapped);
                    return;
                }
            }

            // 3) Last resort: smallest two-line size, second line cut with "...".
            Font smallest = baseFont.deriveFont(MIN_SIZE);
            FontMetrics fm = getFontMetrics(smallest);
            apply(smallest, wrapWithEllipsis(text, fm, maxWidth));
        }

        private void apply(Font newFont, String[] newLines) {
            font = newFont;
            lines = newLines;
            FontMetrics fm = getFontMetrics(font);
            int widest = 0;
            for (String line : lines) {
                widest = Math.max(widest, fm.stringWidth(line));
            }
            setPreferredSize(new Dimension(widest, fm.getHeight() * lines.length));
        }

        /** Greedy word wrap into at most two lines; null if it cannot fit. */
        private static String[] wrap(String text, FontMetrics fm, int maxWidth) {
            String[] words = text.split("\\s+");
            StringBuilder first = new StringBuilder();
            int i = 0;
            while (i < words.length) {
                String attempt = first.length() == 0 ? words[i] : first + " " + words[i];
                if (fm.stringWidth(attempt) > maxWidth) {
                    break;
                }
                first = new StringBuilder(attempt);
                i++;
            }
            if (first.length() == 0) {
                return null; // a single word is wider than the line
            }
            if (i == words.length) {
                return new String[] {first.toString()};
            }
            StringBuilder second = new StringBuilder();
            for (; i < words.length; i++) {
                second.append(second.length() == 0 ? "" : " ").append(words[i]);
            }
            if (fm.stringWidth(second.toString()) > maxWidth) {
                return null;
            }
            return new String[] {first.toString(), second.toString()};
        }

        private static String[] wrapWithEllipsis(String text, FontMetrics fm, int maxWidth) {
            String[] words = text.split("\\s+");
            StringBuilder first = new StringBuilder();
            int i = 0;
            while (i < words.length) {
                String attempt = first.length() == 0 ? words[i] : first + " " + words[i];
                if (fm.stringWidth(attempt) > maxWidth) {
                    break;
                }
                first = new StringBuilder(attempt);
                i++;
            }
            if (first.length() == 0) {
                first = new StringBuilder(cut(words[0], fm, maxWidth));
                return new String[] {first.toString()};
            }
            if (i == words.length) {
                return new String[] {first.toString()};
            }
            StringBuilder rest = new StringBuilder();
            for (; i < words.length; i++) {
                rest.append(rest.length() == 0 ? "" : " ").append(words[i]);
            }
            return new String[] {first.toString(), cut(rest.toString(), fm, maxWidth)};
        }

        private static String cut(String value, FontMetrics fm, int maxWidth) {
            if (fm.stringWidth(value) <= maxWidth) {
                return value;
            }
            String ellipsis = "...";
            String result = value;
            while (result.length() > 1 && fm.stringWidth(result + ellipsis) > maxWidth) {
                result = result.substring(0, result.length() - 1);
            }
            return result + ellipsis;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(font);
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int y = fm.getAscent();
            for (String line : lines) {
                g2.drawString(line, 0, y);
                y += fm.getHeight();
            }
            g2.dispose();
        }
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
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(!isEnabled() ? DISABLED_GREY : hover ? ORANGE_HOVER : ORANGE);
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
