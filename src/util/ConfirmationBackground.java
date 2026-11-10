package util;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;

/**
 * Base class for the full-screen confirmation pages (Order Added, Order
 * Edited, Start Over).
 *
 * It supplies the off-white background with the two orange corner ribbons
 * that every confirmation page is drawn on top of.
 */
public class ConfirmationBackground extends JPanel {

    /**
     * How long a confirmation page stays on screen before the kiosk moves on
     * by itself. OrderAdded and OrderEdited share this value so both feel
     * identical to the customer.
     */
    public static final int AUTO_REDIRECT_MILLIS = 3000;

    public ConfirmationBackground() {
        setBackground(UiColors.BACKGROUND);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        double w = getWidth();
        double h = getHeight();

        g2.setColor(UiColors.ORANGE);

        // Top-left orange corner
        Path2D top = new Path2D.Double();
        top.moveTo(0, 0);
        top.lineTo(w * 0.42, 0);
        top.quadTo(w * 0.28, h * 0.035, w * 0.09, h * 0.03);
        top.quadTo(w * 0.035, h * 0.03, w * 0.045, h * 0.055);
        top.quadTo(w * 0.055, h * 0.07, 0, h * 0.072);
        top.closePath();
        g2.fill(top);

        // Bottom-right orange corner
        Path2D bottom = new Path2D.Double();
        bottom.moveTo(w, h);
        bottom.lineTo(w * 0.58, h);
        bottom.quadTo(w * 0.72, h * 0.965, w * 0.91, h * 0.97);
        bottom.quadTo(w * 0.965, h * 0.97, w * 0.955, h * 0.945);
        bottom.quadTo(w * 0.945, h * 0.93, w, h * 0.928);
        bottom.closePath();
        g2.fill(bottom);

        g2.dispose();
    }
}
