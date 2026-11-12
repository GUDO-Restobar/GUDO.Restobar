package util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Fades a list of page elements in one after the other.
 *
 * Main purpose: reusable entrance animation. Each element is registered as a
 * "step" - a function that receives its visibility (0 = hidden, 1 = shown).
 * The steps start in the order they were added, each a little later than the
 * one before, and ease out to fully visible. The page owns the elements; this
 * class only decides how visible each one is at any moment.
 */
public class FadeInSequence {

    private final List<Consumer<Float>> steps = new ArrayList<>();
    private final JComponent owner;
    private final int fadeMillis;
    private final int staggerMillis;
    private final Timer timer = new Timer(10, e -> tick());
    private long startNanos;

    /**
     * @param owner         the page to repaint while the animation runs
     * @param fadeMillis    how long one element takes to become fully visible
     * @param staggerMillis how much later each element starts than the previous one
     */
    public FadeInSequence(JComponent owner, int fadeMillis, int staggerMillis) {
        this.owner = owner;
        this.fadeMillis = fadeMillis;
        this.staggerMillis = staggerMillis;
    }

    /** Adds the next element in the order; it receives its visibility from 0 to 1. */
    public void add(Consumer<Float> step) {
        steps.add(step);
    }

    /** Hides every element, then fades them in again from the first one. */
    public void start() {
        showAll(0f);
        startNanos = System.nanoTime();
        timer.restart();
    }

    /** Stops the animation and hides every element, ready for the next start(). */
    public void reset() {
        timer.stop();
        showAll(0f);
    }

    private void showAll(float alpha) {
        steps.forEach(step -> step.accept(alpha));
        owner.repaint();
    }

    /** One animation tick: each element moves along its own fade. */
    private void tick() {
        long elapsed = (System.nanoTime() - startNanos) / 1_000_000L;
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).accept(fraction(elapsed - (long) i * staggerMillis));
        }
        owner.repaint();

        if (elapsed >= (steps.size() - 1L) * staggerMillis + fadeMillis) {
            timer.stop();   // the last tick above already left every element at 1
        }
    }

    /** 0 while waiting for its turn, then a quadratic ease-out up to 1. */
    private float fraction(long millisSinceStart) {
        if (millisSinceStart <= 0) {
            return 0f;
        }
        if (millisSinceStart >= fadeMillis) {
            return 1f;
        }
        float progress = millisSinceStart / (float) fadeMillis;
        return 1f - (1f - progress) * (1f - progress);
    }
}