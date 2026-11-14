package util.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/** Rounded button with a soft drop shadow, an optional outline and a hover colour. */
public class RoundedButton extends JButton {
    private static final int SHADOW = 4;
    private static final int ARC = 8;

    private final Color base;
    private final Color hover;
    private final Color outline;

    public RoundedButton(String text, Icon icon, Color base, Color hover, Color outline) {
        super(text, icon);
        this.base = base;
        this.hover = hover;
        this.outline = outline;
        setBackground(base);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) setBackground(hover); }
            @Override public void mouseExited(MouseEvent e) { setBackground(base); }
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

        int faceHeight = getHeight() - SHADOW;

        for (int offset = SHADOW; offset > 0; offset--) {
            g2.setColor(UiColors.shadow(5 + (SHADOW - offset) * 5));
            g2.fillRoundRect(0, offset, getWidth(), faceHeight, ARC, ARC);
        }

        g2.setColor(isEnabled() ? getBackground() : UiColors.DISABLED);
        g2.fillRoundRect(0, 0, getWidth(), faceHeight, ARC, ARC);

        if (outline != null) {
            g2.setColor(outline);
            g2.setStroke(new BasicStroke(UiKit.BORDER_WIDTH));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, faceHeight - 1, ARC, ARC));
        }
        g2.dispose();

        Graphics textGraphics = g.create();
        textGraphics.translate(0, -SHADOW / 2);
        super.paintComponent(textGraphics);
        textGraphics.dispose();
    }
}
