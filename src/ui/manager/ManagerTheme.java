package ui.manager;

import java.awt.*;
import java.io.IOException;
import javax.swing.*;
import util.FontLoader;
import util.KioskScrollBarUI;

/** Colors, fonts and the logo shared by the manager screens. */
public final class ManagerTheme {
    public static final Color MAINWHITE = new Color(246, 246, 246);
    public static final Color MAINORANGE = Color.decode("#EC5E02");
    public static final ImageIcon LOGO = new ImageIcon("public\\assets\\logo\\logo-circle.png");
    public static final Font BOLD = loadFont("Montserrat/static/Montserrat-Bold.ttf");
    public static final Font REGULAR = loadFont("Montserrat/static/Montserrat-Regular.ttf");

    // Scroll bar images (the same ones the kiosk MenuPage uses).
    private static final String SCROLL_THUMB = "public/assets/scrollbar/scroll-thumb.png";
    private static final String SCROLL_TRACK = "public/assets/scrollbar/scroll-track.png";

    private ManagerTheme() {}

    /**
     * Gives a scroll pane the kiosk-style vertical scroll bar. Only the look is set here;
     * the size of the scroll pane itself (setBounds) stays with each panel.
     *
     * @param background the color behind the bar - use the color of what it sits next to
     */
    public static void styleScrollBar(JScrollPane scrollPane, Color background) {
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        bar.setUnitIncrement(24);
        bar.setUI(new KioskScrollBarUI(SCROLL_THUMB, SCROLL_TRACK));
        bar.setPreferredSize(new Dimension(26, 0));    // bar width; the height follows the scroll pane
        bar.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 8));
        bar.setBackground(background);
    }

    public static Font loadFont(String file) {
        try {
            return FontLoader.loadFont("public/assets/fonts/" + file);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load font: " + file, e);
        }
    }
}