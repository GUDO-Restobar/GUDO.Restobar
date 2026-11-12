package util;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Base class for full-screen pages whose elements fade in one after the other
 * every time the page is shown.
 *
 * Template Method pattern: this class owns the animation (top ribbon first,
 * bottom ribbon last, replayed on every visit); a page only builds its content,
 * wrapping each element in {@link #fade} in the order it should appear, and
 * calls {@link #finishEntrance()} when it is done.
 */
public abstract class FadeInPage extends ConfirmationBackground {

    /** How long one element takes to appear, and the delay between elements. */
    private static final int FADE_MILLIS = 125;
    private static final int STAGGER_MILLIS = 25;

    private final FadeInSequence entrance = new FadeInSequence(this, FADE_MILLIS, STAGGER_MILLIS);

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
        entrance.add(fading::setAlpha);
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
}