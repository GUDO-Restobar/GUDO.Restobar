package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.swing.*;

/**
 * One line of the order on the checkout screen.
 *
 * Main purpose: reusable UI component - shows the product picture, name,
 * line total, the - / quantity / + circles, the add-ons (extras on the left,
 * favorites on the right), an "Edit Meal" button and a trash icon. The card
 * grows taller when a line has many add-ons. It asks the owner what "edit" and "remove" mean through its
 * constructor callbacks, so CheckoutPage stays in charge of the cart.
 *
 * The trash icon needs two presses: the first one only shows "Are you sure?".
 */
public class OrderItemCard extends FadeablePanel {

    /** Height of the name / quantity row: room for a two-line name. */
    private static final int HEADER_HEIGHT = 36;
    private static final int NAME_GAP = 14;
    /** One font size for the meal name and the price. */
    private static final float TEXT_SIZE = 14f;
    /** Free space between the quantity controls and the price. */
    private static final int PRICE_GAP = 24;

    private static final String TRASH_ICON = "public/assets/icons/trash.png";
    private static final int TRASH_SIZE = 23;

    /** Shown next to the trash icon after the first press. */
    private static final String CONFIRM_TEXT = "Are you sure?";

    /** The shortest a card gets: it must still fit the picture. */
    private static final int CARD_HEIGHT = 170;
    /** Everything in the card except the add-on columns: borders, name row, edit button, gaps. */
    private static final int CHROME_HEIGHT = 106;
    private static final int ADDON_ROW_HEIGHT = 18;
    private static final int ADDON_COLUMN_WIDTH = 130;
    private static final int ADDON_GAP = 24;
    private static final int IMAGE_SIZE = 124;
    /** Free space between the picture and the left edge of the card. */
    private static final int IMAGE_MARGIN = 14;
    /** How long "Are you sure?" takes to fade in. */
    private static final int CONFIRM_FADE_MILLIS = 200;
    /** How long "Are you sure?" stays before the first press is forgotten. */
    private static final int CONFIRM_MILLIS = 5000;
    /** Space between "Are you sure?" and the trash icon. */
    private static final int CONFIRM_GAP = 3;
    private static final int ARC = 14;
    private static final int SHADOW = 3;

    private final Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
    private final Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");
    private final Font inter = UiKit.font("Inter/static/Inter_18pt-Regular.ttf");

    /** Set by the first press of the trash icon. */
    private boolean confirming;
    /** Visibility of "Are you sure?": fades from 0 to 1 after the first press. */
    private float confirmAlpha = 1f;
    private final FadeInSequence confirmFade = new FadeInSequence(this, CONFIRM_FADE_MILLIS, 0);
    /** Forgets the first press after {@link #CONFIRM_MILLIS}. */
    private final Timer confirmTimer = new Timer(CONFIRM_MILLIS, e -> cancelConfirm());

    /** Needed to place the confirm text right beside the icon. */
    private JButton trashButton;

    /** Runs on the second press of the trash icon. */
    private Runnable onRemove = () -> {};

    private final OrderLine line;
    private final Product product;
    private final IntSupplier maxQuantity;
    private final IntConsumer onQuantityChange;
    private int quantity;
    private JLabel quantityLabel;
    private JLabel priceLabel;
    private CircleButton plusButton;

    /**
     * @param line             the order line this card shows (read live, so totals stay current)
     * @param maxQuantity      the most this line may hold (its share of the stock)
     * @param onQuantityChange called with the new quantity after the + or - circle is pressed
     * @param onEdit           runs when "Edit Meal" is pressed
     * @param onRemove         runs on the second press of the trash icon
     */
    public OrderItemCard(OrderLine line,
                         IntSupplier maxQuantity,
                         IntConsumer onQuantityChange,
                         Runnable onEdit,
                         Runnable onRemove) {

        this.line = line;
        this.product = line.getProduct();
        this.quantity = Math.max(1, line.getQuantity());
        this.maxQuantity = maxQuantity;
        this.onQuantityChange = onQuantityChange != null ? onQuantityChange : q -> {};
        Runnable edit = onEdit != null ? onEdit : () -> {};
        this.onRemove = onRemove != null ? onRemove : () -> {};

        List<String> extras = extraLabels(line);
        List<String> favorites = favoriteLabels(line);

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, IMAGE_MARGIN, 16 + SHADOW, 17));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmFade.add(alpha -> confirmAlpha = alpha);
        confirmTimer.setRepeats(false);

        add(buildImage(product.getImagePath()), BorderLayout.WEST);
        add(buildInfo(product.getName(), extras, favorites, edit), BorderLayout.CENTER);
        add(buildRightColumn(), BorderLayout.EAST);

        int rows = Math.max(extras.size(), favorites.size());
        int height = Math.max(CARD_HEIGHT, CHROME_HEIGHT + rows * ADDON_ROW_HEIGHT);
        setPreferredSize(new Dimension(760, height));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    /** Left column: the extras, then extra water and cutlery. */
    private static List<String> extraLabels(OrderLine line) {
        List<String> labels = new ArrayList<>();
        line.getExtras().forEach((extra, amount) -> labels.add(extra.getName() + " x" + amount));
        if (line.isWater()) {
            labels.add("Extra Water");
        }
        if (line.isCutlery()) {
            labels.add("Cutlery");
        }
        return labels;
    }

    /** Right column: the favorites. */
    private static List<String> favoriteLabels(OrderLine line) {
        List<String> labels = new ArrayList<>();
        line.getFavorites().forEach((favorite, amount) -> labels.add(favorite.getName() + " x" + amount));
        return labels;
    }

    /** Applies a +1 / -1 change. The quantity never drops below 1; the trash icon removes the item. */
    private void changeQuantity(int change) {
        int updated = Math.min(maxQuantity(), Math.max(1, quantity + change));
        if (updated == quantity) {
            return;
        }
        quantity = updated;
        quantityLabel.setText(String.valueOf(quantity));
        updatePlusButton();
        onQuantityChange.accept(quantity);                      // the owner updates the line first...
        priceLabel.setText(UiKit.peso(line.getSubtotal()));     // ...so the price includes the add-ons
    }

    /** The most this line can have: its share of the stock. */
    private int maxQuantity() {
        return Math.max(1, maxQuantity.getAsInt());
    }

    /** Disables the + circle once the quantity has reached the stock. */
    private void updatePlusButton() {
        if (plusButton != null) {
            plusButton.setEnabled(quantity < maxQuantity());
        }
    }

    /** Lets the owner decide what "remove" means for this specific card. */
    public void setOnRemove(Runnable action) {
        this.onRemove = action != null ? action : () -> {};
    }

    private JLabel buildImage(String imagePath) {
        JLabel image = new JLabel(UiKit.icon(imagePath, IMAGE_SIZE, IMAGE_SIZE));
        image.setVerticalAlignment(SwingConstants.CENTER);
        return image;
    }

    private JPanel buildInfo(String name,
                             List<String> leftAddOns, List<String> rightAddOns,
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

        MealNameLabel nameLabel = new MealNameLabel(name, bold.deriveFont(TEXT_SIZE), UiColors.TEXT_DARK);

        quantityLabel = new JLabel(String.valueOf(quantity));
        quantityLabel.setFont(bold.deriveFont(16f));
        quantityLabel.setForeground(UiColors.ORANGE);

        CircleButton minus = new CircleButton(false, 15);
        minus.addActionListener(e -> changeQuantity(-1));
        CircleButton plus = new CircleButton(true, 15);
        plus.addActionListener(e -> changeQuantity(1));
        plusButton = plus;
        updatePlusButton();

        // Same controls and spacing as before, just grouped so the name can
        // use whatever width is left over.
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        controls.setOpaque(false);
        controls.add(minus);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(quantityLabel);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(plus);

        header.add(nameLabel);
        header.add(controls);
        return header;
    }

    private JPanel buildAddOns(List<String> leftAddOns, List<String> rightAddOns) {
        JPanel columns = new JPanel();
        columns.setLayout(new BoxLayout(columns, BoxLayout.X_AXIS));
        columns.setOpaque(false);

        columns.add(buildAddOnColumn(leftAddOns));
        columns.add(Box.createHorizontalStrut(ADDON_GAP));
        columns.add(buildAddOnColumn(rightAddOns));
        columns.add(Box.createHorizontalGlue());
        return columns;
    }

    private JPanel buildAddOnColumn(List<String> addOns) {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setAlignmentY(Component.TOP_ALIGNMENT);
        column.setPreferredSize(new Dimension(ADDON_COLUMN_WIDTH, addOns.size() * ADDON_ROW_HEIGHT));
        column.setMaximumSize(new Dimension(ADDON_COLUMN_WIDTH, Integer.MAX_VALUE));

        for (String addOn : addOns) {
            JLabel label = new JLabel("\u2022 " + addOn);
            label.setFont(regular.deriveFont(11f));
            label.setForeground(UiColors.TEXT_MUTED);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            label.setToolTipText(addOn);
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            column.add(label);
        }
        column.add(Box.createVerticalGlue());
        return column;
    }

    private JPanel buildEditRow(Runnable onEdit) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);

        OutlineButton edit = new OutlineButton("Edit Meal");
        edit.setFont(inter.deriveFont(12.5f));
        edit.setForeground(UiColors.BROWN);
        edit.setPreferredSize(new Dimension(139, 25));
        edit.addActionListener(e -> onEdit.run());

        row.add(edit);
        return row;
    }

    private JPanel buildRightColumn() {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.setBorder(BorderFactory.createEmptyBorder(0, PRICE_GAP, 0, 0));

        priceLabel = new JLabel(UiKit.peso(line.getSubtotal()));
        priceLabel.setFont(bold.deriveFont(TEXT_SIZE));
        priceLabel.setForeground(UiColors.ORANGE);

        JPanel priceRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        priceRow.setOpaque(false);
        priceRow.add(priceLabel);
        column.add(priceRow, BorderLayout.NORTH);

        JButton remove = new JButton(UiKit.icon(TRASH_ICON, TRASH_SIZE, TRASH_SIZE));
        remove.setOpaque(false);
        remove.setContentAreaFilled(false);
        remove.setBorderPainted(false);
        remove.setBorder(BorderFactory.createEmptyBorder());
        remove.setMargin(new Insets(0, 0, 0, 0));
        remove.setFocusPainted(false);
        remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        remove.setToolTipText("Remove item");
        remove.addActionListener(e -> {
            if (confirming) {
                confirmTimer.stop();
                onRemove.run();
            } else {
                // First press only asks; a second press within 5 seconds removes this card.
                confirming = true;
                confirmFade.start();
                confirmTimer.restart();
            }
        });
        trashButton = remove;

        JPanel trashRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        trashRow.setOpaque(false);
        trashRow.add(remove);
        column.add(trashRow, BorderLayout.SOUTH);

        return column;
    }

    /** The 5 seconds are up: "Are you sure?" disappears and the next press asks again. */
    private void cancelConfirm() {
        confirming = false;
        repaint();
    }

    @Override
    public void removeNotify() {
        confirmTimer.stop();   // a removed card must not keep a timer running
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int faceHeight = getHeight() - SHADOW;

        for (int offset = SHADOW; offset > 0; offset--) {
            g2.setColor(UiColors.shadow(4 + (SHADOW - offset) * 3));
            g2.fillRoundRect(0, offset, getWidth() - 1, faceHeight, ARC, ARC);
        }

        g2.setColor(UiColors.SURFACE);
        g2.fillRoundRect(0, 0, getWidth() - 1, faceHeight, ARC, ARC);

        g2.setColor(UiColors.BROWN);
        g2.setStroke(new BasicStroke(UiKit.BORDER_WIDTH));
        g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, faceHeight - 1, ARC, ARC));

        // Drawn instead of added as a component, so showing it never re-lays
        // out the card and never squeezes the add-on columns.
        if (confirming && trashButton != null) {
            Point icon = SwingUtilities.convertPoint(trashButton, 0, 0, this);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(bold.deriveFont(10.5f));
            g2.setColor(UiColors.DANGER);
            g2.setComposite(AlphaComposite.SrcOver.derive(confirmAlpha));
            FontMetrics fm = g2.getFontMetrics();
            int x = icon.x - CONFIRM_GAP - fm.stringWidth(CONFIRM_TEXT);
            int y = icon.y + (trashButton.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(CONFIRM_TEXT, x, y);
        }

        g2.dispose();
        super.paintComponent(g);
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
     * The meal name, always at the same size as the price. A name that is too
     * wide goes onto a second line; only a name too long for two lines is cut
     * with "...".
     */
    private static class MealNameLabel extends JComponent {
        private final String text;
        private final Font font;
        private String[] lines;

        MealNameLabel(String text, Font font, Color color) {
            this.text = text == null ? "" : text.trim();
            this.font = font;
            setForeground(color);
            setToolTipText(this.text);
            fitTo(10_000);
        }

        /** Chooses the line breaks so the name fits in the given width. */
        void fitTo(int width) {
            FontMetrics fm = getFontMetrics(font);
            int maxWidth = Math.max(40, width);
            String[] wrapped = wrap(text, fm, maxWidth);
            apply(wrapped != null ? wrapped : wrapWithEllipsis(text, fm, maxWidth));
        }

        private void apply(String[] newLines) {
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
            g2.setColor(hover ? UiColors.HOVER_GREY_PALE : UiColors.SURFACE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            g2.setColor(UiColors.BROWN);
            g2.setStroke(new BasicStroke(UiKit.BORDER_WIDTH));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 6, 6));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}