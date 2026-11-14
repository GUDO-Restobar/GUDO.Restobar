package util.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * A button that is only its picture, and gets darker while the mouse is over it.
 *
 * Main purpose: reusable image button (e.g. "Go Back"). The picture is drawn
 * as-is, so its transparent corners stay clear and whatever is behind the
 * button shows through. The hover picture is a darkened copy made once, so
 * hovering is just a swap - the same ~15% darker step as the other orange buttons.
 */
public class ImageButton extends JButton {

    /** 38/255 of black over the orange picture: #EC5E02 -> #C94F00. */
    private static final int HOVER_DARKEN_ALPHA = 38;

    private final Icon normal;
    private final Icon hover;

    public ImageButton(ImageIcon icon) {
        super(icon);
        this.normal = icon;
        this.hover = darken(icon);

        setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Icon icon = getModel().isRollover() ? hover : normal;
        icon.paintIcon(this, g,
            (getWidth() - icon.getIconWidth()) / 2,
            (getHeight() - icon.getIconHeight()) / 2);
    }

    /** "Source atop" darkens only the drawn pixels; clear corners stay transparent. */
    private static Icon darken(ImageIcon icon) {
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = copy.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(new Color(0, 0, 0, HOVER_DARKEN_ALPHA));
        g.fillRect(0, 0, width, height);
        g.dispose();

        return new ImageIcon(copy);
    }
}
