package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.List;
import javax.swing.*;

/**
 * A row of components that slides sideways, one item per arrow press.
 *
 * Main purpose: reusable UI component (the "Gudo's Favorites" row). The
 * arrows glide the row to its new position with an ease-out animation timed
 * by the clock, so it moves equally smoothly however busy the screen is.
 * Pressing the arrow repeatedly keeps adding to where the row is heading.
 * An arrow only shows while there is something to slide to in its direction.
 */
public class Carousel extends JPanel {

    private static final int ARROW = 32;
    private static final int SLIDE_MILLIS = 320;

    private final JPanel row = new JPanel();
    private final JViewport viewport = new JViewport();
    private final Arrow previous = new Arrow(false);
    private final Arrow next = new Arrow(true);
    private final Timer slide = new Timer(10, e -> tick());
    /** How far one arrow press moves: one item plus the gap after it. */
    private final int step;

    private int from;
    private int target;
    private long startedAt;

    public Carousel(List<? extends JComponent> items, int gap) {
        setLayout(null);                      // the arrows float over the viewport
        setOpaque(false);

        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        for (JComponent item : items) {
            if (row.getComponentCount() > 0) {
                row.add(Box.createHorizontalStrut(gap));
            }
            item.setMaximumSize(item.getPreferredSize());
            row.add(item);
        }
        row.add(Box.createHorizontalGlue());
        step = items.isEmpty() ? 0 : items.get(0).getPreferredSize().width + gap;

        viewport.setOpaque(false);
        viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);   // no blit: the arrows sit on top
        viewport.setView(row);

        previous.addActionListener(e -> slideBy(-1));
        next.addActionListener(e -> slideBy(1));
        add(previous);                        // first added = painted on top
        add(next);
        add(viewport);
        updateArrows();
    }

    @Override
    public Dimension getPreferredSize() {
        return row.getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height);
    }

    @Override
    public void doLayout() {
        viewport.setBounds(0, 0, getWidth(), getHeight());
        int y = (getHeight() - ARROW) / 2;
        previous.setBounds(2, y, ARROW, ARROW);
        next.setBounds(getWidth() - ARROW - 2, y, ARROW, ARROW);
        target = Math.min(target, maxScroll());   // e.g. after the window grew
        updateArrows();
    }

    @Override
    public void removeNotify() {
        slide.stop();                         // a removed page must not keep a timer running
        super.removeNotify();
    }

    private int maxScroll() {
        return Math.max(0, row.getPreferredSize().width - viewport.getWidth());
    }

    private void slideBy(int items) {
        target = Math.max(0, Math.min(maxScroll(), target + items * step));
        from = viewport.getViewPosition().x;
        startedAt = System.nanoTime();
        slide.restart();
        updateArrows();
    }

    private void tick() {
        float progress = Math.min(1f, (System.nanoTime() - startedAt) / (SLIDE_MILLIS * 1_000_000f));
        float eased = 1f - (float) Math.pow(1f - progress, 3);
        viewport.setViewPosition(new Point(from + Math.round((target - from) * eased), 0));
        if (progress >= 1f) {
            slide.stop();
        }
    }

    private void updateArrows() {
        previous.setVisible(target > 0);
        next.setVisible(target < maxScroll());
    }

    /** The round orange arrow button. */
    private static class Arrow extends JButton {
        private final boolean right;
        private boolean hover;

        Arrow(boolean right) {
            this.right = right;
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
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(UiColors.shadow(40));
            g2.fillOval(0, 1, ARROW, ARROW);
            g2.setColor(hover ? UiColors.ORANGE_HOVER : UiColors.ORANGE);
            g2.fillOval(0, 0, ARROW, ARROW);

            double tip = right ? ARROW * 0.62 : ARROW * 0.38;
            double back = right ? ARROW * 0.40 : ARROW * 0.60;
            double mid = ARROW / 2.0;
            double half = ARROW * 0.18;
            g2.setColor(UiColors.WHITE);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(back, mid - half, tip, mid));
            g2.draw(new Line2D.Double(tip, mid, back, mid + half));
            g2.dispose();
        }
    }
}
