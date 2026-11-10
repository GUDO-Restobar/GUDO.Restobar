package util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
            Image image = new ImageIcon(path).getImage()
                .getScaledInstance(width, height, Image.SCALE_SMOOTH);
            cached = new ImageIcon(image);
            ICONS.put(key, cached);
        }
        return cached;
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
}
