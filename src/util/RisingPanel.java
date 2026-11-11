package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * A {@link FadingPanel} whose content floats upwards while the mouse is over
 * it, then eases back down to its resting place when the mouse leaves.
 *
 * The panel reserves as much free room above the content as it can rise, so
 * the floating copy is never cut off at the panel edge. The hover state is
 * animated along a smooth ease-in-out curve, so the content leaves and
 * reaches its resting place gently instead of jumping.
 *
 * Other content can be made to move along with it through {@link #follower}
 * - for example the pointing hand under an order card.
 */
public class RisingPanel extends FadingPanel {

    /** How long a rise (or a settle) takes, in milliseconds. */
    private static final int RISE_MILLIS = 180;

    /** How far up the content floats at most, in pixels. */
    private final int maxRise;

    /** Wrappers that always copy this panel's lift. */
    private final List<RisingPanel> followers = new ArrayList<>();

    /** How far up the content is drawn right now: 0 .. maxRise. */
    private float rise;

    /** Drives the rise animation. */
    private final Timer riseTimer;
    private long riseStartNanos;
    private float riseFrom;
    private float riseTarget;

    /**
     * Wraps {@code content} and floats it up by up to {@code maxRise} pixels
     * while the mouse is over it. The panel is {@code 2 * maxRise} pixels
     * taller than the content, so the raised copy still fits inside it.
     */
    public RisingPanel(Component content, int maxRise) {
        this(content, Math.max(0, maxRise), Math.max(0, maxRise), true);
    }

    /**
     * @param headroom  free room above (and below) the content
     * @param hoverable whether this panel reacts to the mouse itself
     */
    private RisingPanel(Component content, int maxRise, int headroom, boolean hoverable) {
        super(content, headroom);
        this.maxRise = maxRise;
        riseTimer = new Timer(10, e -> animateRise());

        if (hoverable) {
            content.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    riseTo(maxRise);
                }
                @Override public void mouseExited(MouseEvent e) {
                    riseTo(0f);
                }
            });
        }
    }

    /**
     * Wraps {@code content} so it rises and settles exactly together with
     * this panel, without reacting to the mouse itself. No headroom is
     * reserved - the caller must make sure the top of the content is empty
     * (the pointing-hand pictures have plenty of transparent room above the
     * fingers), otherwise the raised part would be cut off.
     */
    public RisingPanel follower(Component content) {
        RisingPanel follower = new RisingPanel(content, maxRise, 0, false);
        followers.add(follower);
        return follower;
    }

    /** Drops the content straight back to its resting place (no animation). */
    public void settle() {
        riseTimer.stop();
        riseTarget = 0f;
        applyRise(0f);
    }

    /** Eases the content towards the given lift. */
    private void riseTo(float target) {
        if (target == riseTarget) {
            return;                       // already resting there or on its way
        }
        riseTarget = target;
        riseFrom = rise;
        riseStartNanos = System.nanoTime();
        riseTimer.restart();
    }

    /** One animation tick: moves the content along the ease-in-out curve. */
    private void animateRise() {
        float progress = Math.min(1f,
            (System.nanoTime() - riseStartNanos) / (RISE_MILLIS * 1_000_000f));

        // Smoothstep: eases in from its old place, eases out into the new one.
        float eased = progress * progress * (3f - 2f * progress);
        applyRise(riseFrom + (riseTarget - riseFrom) * eased);

        if (progress >= 1f) {
            applyRise(riseTarget);
            riseTimer.stop();
        }
    }

    /** Shows the content at the given lift and carries every follower along. */
    private void applyRise(float value) {
        rise = value;
        repaint();
        for (RisingPanel follower : followers) {
            follower.applyRise(value);
        }
    }

    @Override
    protected void paintContent(Graphics g) {
        if (rise < 1f) {
            super.paintContent(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(0, -Math.round(rise));   // whole pixels keep the content crisp
        super.paintContent(g2);
        g2.dispose();
    }
}
