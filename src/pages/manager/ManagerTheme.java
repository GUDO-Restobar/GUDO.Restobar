package pages.manager;

import java.awt.*;
import javax.swing.*;
import util.ui.KioskScrollBarUI;
import util.ui.UiKit;

/** Fonts, the logo and the kiosk-style scroll bar shared by the manager screens.
 *  Colors come from {@link util.ui.UiColors} so the manager matches the kiosk exactly. */
public final class ManagerTheme {
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
        return UiKit.font(file);
    }
}