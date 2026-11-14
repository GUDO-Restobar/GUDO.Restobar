package util.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * An on / off switch whose knob slides across and turns orange when on.
 *
 * Main purpose: reusable UI component for yes / no choices (extra water,
 * cutlery). The owner is told about every change through a callback.
 */
public class ToggleSwitch extends JComponent {

    private static final int WIDTH = 46;
    private static final int HEIGHT = 24;
    private static final int KNOB = 18;
    private static final int SLIDE_MILLIS = 140;

    private final Consumer<Boolean> onToggle;
    private final Timer slide = new Timer(10, e -> tick());
    private boolean on;
    /** Where the knob is: 0 = left (off), 1 = right (on). */
    private float position;
    private float from;
    private long startedAt;

    public ToggleSwitch(boolean on, Consumer<Boolean> onToggle) {
        this.on = on;
        this.position = on ? 1f : 0f;
        this.onToggle = onToggle;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { toggle(); }
        });
    }

    private void toggle() {
        on = !on;
        from = position;
        startedAt = System.nanoTime();
        slide.restart();
        onToggle.accept(on);
    }

    private void tick() {
        float progress = Math.min(1f, (System.nanoTime() - startedAt) / (SLIDE_MILLIS * 1_000_000f));
        float eased = 1f - (1f - progress) * (1f - progress);
        position = from + ((on ? 1f : 0f) - from) * eased;
        repaint();
        if (progress >= 1f) {
            slide.stop();
        }
    }

    @Override
    public void removeNotify() {
        slide.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = getWidth() - WIDTH;                    // right-aligned in whatever room it gets
        int y = (getHeight() - HEIGHT) / 2;
        g2.setColor(UiColors.DISABLED);
        g2.fillRoundRect(x, y, WIDTH, HEIGHT, HEIGHT, HEIGHT);

        int pad = (HEIGHT - KNOB) / 2;
        int knobX = x + pad + Math.round(position * (WIDTH - KNOB - 2 * pad));
        g2.setColor(position > 0.5f ? UiColors.ORANGE : UiColors.TEXT_HINT);
        g2.fillOval(knobX, y + pad, KNOB, KNOB);
        g2.dispose();
    }
}
