package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * One product tile on the menu grid.
 *
 * Main purpose: reusable UI component - shows the product's picture, name and
 * price and calls {@code onSelect} when it is tapped. While the product is
 * sold out (or fully reserved by the current cart) it greys itself out, shows
 * a red UNAVAILABLE label and refuses to fire the callback.
 */
public class ItemCard extends JPanel {

    private static final int WIDTH = 188;
    private static final int HEIGHT = 221;
    private static final String UNAVAILABLE_TEXT = "UNAVAILABLE";

    private final Product product;
    private final Font font;
    private PictureLabel picture;
    private boolean available = true;

    public ItemCard(Product product, Font font, Consumer<Product> onSelect) {
        this.product = product;
        this.font = font;
        setLayout(new BorderLayout());
        setBackground(UiColors.BACKGROUND);
        setBorder(BorderFactory.createLineBorder(UiColors.BORDER));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        picture = createPicture(product);
        add(picture, BorderLayout.CENTER);
        add(createTextArea(product, font), BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (available && onSelect != null) {
                    onSelect.accept(product);
                }
            }
        });
    }

    private PictureLabel createPicture(Product product) {
        ImageIcon icon = new ImageIcon(product.getImagePath());
        Image scaled = icon.getImage().getScaledInstance(130, 130, Image.SCALE_SMOOTH);
        return new PictureLabel(new ImageIcon(scaled));
    }

    /** Greys the card out and shows the red UNAVAILABLE text when the product can't be ordered. */
    public void setAvailable(boolean available) {
        if (this.available == available) {
            return;
        }
        this.available = available;
        setCursor(Cursor.getPredefinedCursor(available ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        picture.repaint();
    }

    public Product getProduct() {
        return product;
    }

    /** The product picture; paints a white veil and the red text on top when unavailable. */
    private class PictureLabel extends JLabel {
        PictureLabel(Icon icon) {
            super(icon, SwingConstants.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (available) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(UiColors.overlay(UiColors.BACKGROUND, 175));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(font.deriveFont(Font.BOLD, 17f));
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(UNAVAILABLE_TEXT)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(UiColors.DANGER);
            g2.drawString(UNAVAILABLE_TEXT, x, y);
            g2.dispose();
        }
    }

    private JPanel createTextArea(Product product, Font font) {
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        text.setPreferredSize(new Dimension(WIDTH, 92));

        JTextArea name = new JTextArea(product.getName());
        name.setFont(font.deriveFont(15f));
        name.setForeground(UiColors.BROWN);
        name.setOpaque(false);
        name.setEditable(false);
        name.setFocusable(false);
        name.setLineWrap(true);
        name.setWrapStyleWord(true);
        name.setBorder(null);
        name.setMargin(new Insets(0, 0, 0, 0));

        JLabel price = new JLabel("\u20B1" + String.format("%.2f", product.getPrice()));
        price.setFont(font.deriveFont(16f));
        price.setForeground(UiColors.ORANGE_DARK);

        text.add(name, BorderLayout.CENTER);
        text.add(price, BorderLayout.SOUTH);
        return text;
    }
}
