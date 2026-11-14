package util.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.*;
import util.feat.Product;

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
    private static final int ARC = 14;          // rounded corners of the card
    private static final int SHADOW = 4;        // soft drop shadow below the card
    private static final int BORDER_ALPHA = 84; // 0.33 border weight (33% opacity)
    /** Size of the product picture; smaller than its slot so every picture
     *  keeps a margin from the card's edges (the drink shots in particular
     *  are tall and would otherwise touch the top edge). */
    private static final int PICTURE_SIZE = 112;
    private static final String UNAVAILABLE_TEXT = "UNAVAILABLE";

    private final Product product;
    private final Font font;
    private final Consumer<Product> onSelect;
    private PictureLabel picture;
    private boolean available = true;

    /** 0 = invisible, 1 = fully visible (the menu's entrance fade). */
    private float fade = 1f;

    /** One picture of the whole card, used instead of a live repaint while fading. */
    private BufferedImage snapshot;

    /** Fires the product's action; shared by the card and everything inside it. */
    private final MouseAdapter tap = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (available && onSelect != null) {
                onSelect.accept(product);
            }
        }
    };

    public ItemCard(Product product, Font font, Consumer<Product> onSelect) {
        this.product = product;
        this.font = font;
        this.onSelect = onSelect;
        setLayout(new BorderLayout());
        setOpaque(false);                       // painted in paintComponent instead
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1 + SHADOW, 1));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        picture = createPicture(product);
        add(picture, BorderLayout.CENTER);
        add(createTextArea(product, font), BorderLayout.SOUTH);

        UiKit.listenEverywhere(this, tap);   // picture, name and price included
    }

    /** The rounded card: white fill, a soft 0.33-weight outline and a drop shadow. */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int h = getHeight() - SHADOW;
        for (int i = SHADOW; i > 0; i--) {
            g2.setColor(UiColors.shadow(4 + (SHADOW - i) * 4));
            g2.fillRoundRect(0, i, getWidth(), h, ARC, ARC);
        }
        g2.setColor(UiColors.BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth(), h, ARC, ARC);
        g2.setColor(UiColors.overlay(UiColors.BROWN, BORDER_ALPHA));   // 0.33 border weight
        g2.drawRoundRect(0, 0, getWidth() - 1, h - 1, ARC, ARC);

        g2.dispose();
    }

    private PictureLabel createPicture(Product product) {
        return new PictureLabel(UiKit.icon(product.getImagePath(), PICTURE_SIZE, PICTURE_SIZE));
    }

    /** Sets the opacity and repaints (used by the menu's entrance fade). */
    public void setFade(float fade) {
        fade = Math.max(0f, Math.min(1f, fade));
        if (fade == this.fade
                || (fade != 0f && fade != 1f && Math.abs(this.fade - fade) < 0.01f)) {
            return;                        // nothing visible would change
        }
        if (fade == 0f) {
            snapshot = null;               // a new fade starts: re-render the card once
        }
        this.fade = fade;
        repaint();
    }

    /**
     * While the card fades it is drawn from one picture of itself instead of
     * packing every child again on each frame - a 60 fps fade then costs a
     * single image copy per card instead of a full repaint.
     */
    @Override
    public void paint(Graphics g) {
        if (fade >= 1f) {
            snapshot = null;       // fully visible again: paint live
            super.paint(g);
            return;
        }
        Graphics2D g2 = UiKit.translucent(g, fade);
        if (g2 == null || getWidth() <= 0 || getHeight() <= 0) {
            super.paint(g);
            return;
        }
        if (snapshot == null || snapshot.getWidth() != getWidth()
                || snapshot.getHeight() != getHeight()) {
            snapshot = renderPicture();
        }
        g2.drawImage(snapshot, 0, 0, null);
        g2.dispose();
    }

    /** Paints the card at full opacity into its own picture. */
    private BufferedImage renderPicture() {
        BufferedImage picture = new BufferedImage(getWidth(), getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = picture.createGraphics();
        float keep = fade;
        fade = 1f;                 // so super.paint draws it normally
        super.paint(g2);
        fade = keep;
        g2.dispose();
        return picture;
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
        text.setPreferredSize(new Dimension(WIDTH, 92 - SHADOW));

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
