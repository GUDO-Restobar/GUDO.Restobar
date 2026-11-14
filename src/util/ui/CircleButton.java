package util.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import javax.swing.JButton;

/**
 * A small orange circle holding a white minus or plus.
 *
 * Main purpose: reusable UI component shared by every "- / quantity / +"
 * control (the order cards and the Add / Edit Order pages). Greys out when
 * disabled.
 */
public class CircleButton extends JButton {

    private final boolean plus;
    private final int size;
    private boolean hover;

    public CircleButton(boolean plus, int size) {
        this.plus = plus;
        this.size = size;
        setPreferredSize(new Dimension(size, size));
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
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.setColor(!isEnabled() ? UiColors.DISABLED
            : hover ? UiColors.ORANGE_HOVER : UiColors.ORANGE);
        g2.fillOval(0, 0, size, size);

        double mid = size / 2.0;
        double edge = size * 0.27;
        g2.setColor(UiColors.WHITE);
        g2.setStroke(new BasicStroke(size * 0.11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(edge, mid, size - edge, mid));
        if (plus) {
            g2.draw(new Line2D.Double(mid, edge, mid, size - edge));
        }
        g2.dispose();
    }
}
