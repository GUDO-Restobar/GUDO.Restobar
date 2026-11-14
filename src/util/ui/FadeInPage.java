package util.ui;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Base class for full-screen pages whose elements fade in one after the other
 * every time the page is shown.
 *
 * Template Method pattern: this class owns the animation (top ribbon first,
 * bottom ribbon last, replayed on every visit); a page only builds its content,
 * wrapping each element in {@link #fade} in the order it should appear, and
 * calls {@link #finishEntrance()} when it is done.
 *
 * Bridge pattern: the animation is held as an {@link Entrance}, not as the
 * concrete {@link FadeInSequence}, so the page and the animation can change
 * independently. Both live in this file because they are two halves of one
 * reusable entrance toolkit.
 */
public abstract class FadeInPage extends ConfirmationBackground {

    /** How long one element takes to appear, and the delay between elements. */
    private static final int FADE_MILLIS = 125;
    private static final int STAGGER_MILLIS = 25;

    private final Entrance entrance = new FadeInSequence(this, FADE_MILLIS, STAGGER_MILLIS);

    protected FadeInPage() {
        entrance.add(alpha -> topLeftRibbonAlpha = alpha);

        // Replay the fade-in on every visit; hiding the page resets it to blank.
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) {
                onShown();
                if (playsOnShow()) {
                    entrance.start();
                }
            }
            @Override public void componentHidden(ComponentEvent e) {
                entrance.reset();
            }
        });
    }

    /** Wraps an element so it fades, and adds it as the next step of the entrance. */
    protected final FadingPanel fade(Component element) {
        FadingPanel fading = new FadingPanel(element);
        entrance.add(fading::setFade);
        return fading;
    }

    /** Call once all content is added: the bottom ribbon fades in last. */
    protected final void finishEntrance() {
        entrance.add(alpha -> bottomRightRibbonAlpha = alpha);
        entrance.reset();
    }

    /** Hook: runs each time the page is shown, just before the fade-in starts. */
    protected void onShown() {}

    /** Hook: return false when the page must wait (e.g. for data) and call {@link #playEntrance()} itself. */
    protected boolean playsOnShow() {
        return true;
    }

    /** Starts the fade-in now; for pages that delayed it with {@link #playsOnShow()}. */
    protected final void playEntrance() {
        entrance.start();
    }

    // ============================================================
    // Bridge abstraction
    // ============================================================

    /**
     * The animation that reveals the parts of a page.
     *
     * Bridge pattern: {@link FadeInPage} (the abstraction) talks only to this
     * interface, while {@link FadeInSequence} (the implementation) owns the
     * timers and easing. A page can therefore be built and reused with a
     * different entrance animation without changing the page class.
     */
    public interface Entrance {

        /** Adds the next element; it receives its visibility from 0 (hidden) to 1 (shown). */
        void add(Consumer<Float> step);

        /** Forgets every element, e.g. before a page rebuilds its content. */
        void clear();

        /** Hides every element, then plays the whole entrance again from the first one. */
        void start();

        /** Stops the animation and hides every element, ready for the next start(). */
        void reset();
    }

    // ============================================================
    // Bridge implementation
    // ============================================================

    /**
     * Fades a list of page elements in one after the other.
     *
     * Main purpose: reusable entrance animation. Each element is registered as
     * a "step" - a function that receives its visibility (0 = hidden, 1 =
     * shown). The steps start in the order they were added, each a little
     * later than the one before, and ease out to fully visible. The page owns
     * the elements; this class only decides how visible each one is at any
     * moment.
     */
    public static class FadeInSequence implements Entrance {

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
        @Override
        public void add(Consumer<Float> step) {
            steps.add(step);
        }

        /** Forgets every element, e.g. before a page rebuilds its content. */
        @Override
        public void clear() {
            timer.stop();
            steps.clear();
        }

        /** Hides every element, then fades them in again from the first one. */
        @Override
        public void start() {
            showAll(0f);
            startNanos = System.nanoTime();
            timer.restart();
        }

        /** Stops the animation and hides every element, ready for the next start(). */
        @Override
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
}
