package util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 * Small shared toolkit for the UI classes: cached pictures, fonts, price
 * formatting and button hover behaviour.
 *
 * Flyweight pattern: every scaled picture is created once and then shared,
 * so pages and cards that show the same image reuse the same ImageIcon
 * object instead of reading and scaling the file again. Fonts are cached
 * the same way in {@link FontLoader}.
 */
public final class UiKit {

    /** Stroke width of the thin outlines on cards and light buttons. */
    public static final float BORDER_WIDTH = 0.33f;

    /** The scaled pictures already created, by "path|width x height". */
    private static final Map<String, ImageIcon> ICONS = new HashMap<>();

    private UiKit() {
        // static-only class
    }

    /**
     * A cached copy of the picture at {@code path}, scaled to width x height.
     * Factory method: callers never see how the image is loaded or scaled.
     */
    public static synchronized ImageIcon icon(String path, int width, int height) {
        String key = path + "|" + width + "x" + height;
        ImageIcon cached = ICONS.get(key);
        if (cached == null) {
            cached = new ImageIcon(scale(new ImageIcon(path), width, height));
            ICONS.put(key, cached);
        }
        return cached;
    }

    /**
     * Draws the picture into real pixels once, fitted inside width x height.
     * The picture keeps its proportions and is centred, so it is never
     * stretched; the icon itself always has exactly the size asked for.
     *
     * getScaledInstance keeps its result in a soft reference instead: whenever
     * memory gets tight (which fades, with their temporary buffers, provoke)
     * it throws the pixels away and re-runs the slow scaling on the next
     * paint - the classic cause of an animation that stutters and jumps.
     */
    private static BufferedImage scale(ImageIcon source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int sourceWidth = source.getIconWidth();
        int sourceHeight = source.getIconHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return scaled;                           // missing picture: leave it blank
        }
        double ratio = Math.min(width / (double) sourceWidth, height / (double) sourceHeight);
        int drawnWidth = (int) Math.round(sourceWidth * ratio);
        int drawnHeight = (int) Math.round(sourceHeight * ratio);

        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(source.getImage(), (width - drawnWidth) / 2, (height - drawnHeight) / 2,
            drawnWidth, drawnHeight, null);
        g2.dispose();
        return scaled;
    }

    /** A font shipped in public/assets/fonts, at the given size. */
    public static Font font(String file, float size) {
        return font(file).deriveFont(size);
    }

    /** A font shipped in public/assets/fonts, unsized (for deriveFont later). */
    public static Font font(String file) {
        try {
            return FontLoader.loadFont("public/assets/fonts/" + file);
        } catch (java.io.IOException | java.awt.FontFormatException e) {
            throw new IllegalStateException("Could not load font: " + file, e);
        }
    }

    /** An amount in pesos with two decimals and thousands separators, e.g. ₱1,234.50. */
    public static String peso(double amount) {
        return "₱" + String.format("%,.2f", amount);
    }

    /** Gives a plain button the usual hover feedback (hover colour while the mouse is on it). */
    public static void hover(JButton button, Color base, Color hoverColor) {
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setBackground(hoverColor); }
            @Override public void mouseExited(MouseEvent e) { button.setBackground(base); }
        });
    }

    /**
     * Adds the listener to a component and to every component inside it.
     *
     * A press goes to the deepest component under the mouse, and Swing does
     * not pass it on to the parent. Without this a card whose picture, name
     * or price is a child label would ignore taps on exactly those parts.
     */
    public static void listenEverywhere(Component component, MouseListener listener) {
        component.addMouseListener(listener);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                listenEverywhere(child, listener);
            }
        }
    }

    /**
     * A copy of the graphics context at the given opacity, or null when the
     * component is fully visible (the caller then paints normally).
     *
     * Shared by the fade-in animation: painting a component through this
     * context fades the component's own drawing and its children together.
     */
    public static Graphics2D translucent(Graphics g, float alpha) {
        if (alpha >= 0.999f) {
            return null;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        return g2;
    }
}