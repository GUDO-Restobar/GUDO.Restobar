package util.ui;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Timer;

/**
 * Replaces the jumpy mouse-wheel scrolling of a scroll pane with a short
 * glide: the view eases toward its target position at ~60 frames per second.
 * This is the same behaviour as the Menu page's list.
 */
public final class SmoothScroller {

    private final JScrollBar bar;
    private final Timer timer;
    /** Where the view is currently gliding to. */
    private int target;

    private SmoothScroller(JScrollPane scrollPane) {
        bar = scrollPane.getVerticalScrollBar();
        scrollPane.setWheelScrollingEnabled(false);

        timer = new Timer(16, e -> {                    // ~60 frames per second
            int value = bar.getValue();
            int next = value + (int) Math.round((target - value) * 0.16);
            if (Math.abs(target - next) <= 1) {
                next = target;
                ((Timer) e.getSource()).stop();
            }
            bar.setValue(next);
        });

        scrollPane.addMouseWheelListener(e -> {
            int max = bar.getMaximum() - bar.getVisibleAmount();
            int step = e.getWheelRotation() * bar.getUnitIncrement() * 3;
            target = Math.max(0, Math.min(max, target + step));
            if (!timer.isRunning()) {
                timer.start();
            }
        });

        // Dragging the scroll bar thumb takes over from the glide.
        bar.addAdjustmentListener(e -> {
            if (e.getValueIsAdjusting()) {
                timer.stop();
                target = bar.getValue();
            } else if (!timer.isRunning()) {
                target = bar.getValue();
            }
        });
    }

    /** Turns on the smooth wheel scrolling for the given scroll pane. */
    public static void install(JScrollPane scrollPane) {
        new SmoothScroller(scrollPane);
    }
}
