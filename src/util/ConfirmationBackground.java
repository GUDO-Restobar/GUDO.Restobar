package util;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;

public class ConfirmationBackground extends JPanel {
    private static final Color ORANGE = new Color(236, 94, 2);
    private static final Color OFF_WHITE = new Color(246, 246, 246);

    public ConfirmationBackground() {
        setBackground(OFF_WHITE);
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

        g2.setColor(ORANGE);

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

