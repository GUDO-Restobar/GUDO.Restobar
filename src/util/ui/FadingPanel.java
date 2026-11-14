package util.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * A panel whose content can fade in and out.
 *
 * Decorator pattern: in wrapper mode the panel sits between a component and
 * its container and paints the component through an {@link AlphaComposite},
 * so whoever animates a page only has to call {@link #setFade} (0 = invisible,
 * 1 = fully shown). Layout, sizes and behaviour of the wrapped component are
 * untouched - only its painting is affected.
 *
 * Self mode (the no-argument constructor) does the same for a panel's own
 * drawing and all of its children. It is the base of the small cards and
 * sections that fade in with their page.
 *
 * This file also holds the two fade-aware controls that belong to the same
 * animation toolkit: {@link FadeLabel} and {@link RisingPanel}.
 */
public class FadingPanel extends JPanel {

    /** Visibility: 0 = transparent, 1 = fully opaque. */
    private float fade = 1f;

    /** True when the panel fades itself instead of wrapping another component. */
    private final boolean self;

    /** A panel that fades its own drawing and all of its children. */
    public FadingPanel() {
        self = true;
    }

    /** Wraps {@code content} with no extra room around it. */
    public FadingPanel(Component content) {
        this(content, 0);
    }

    /**
     * Wraps {@code content}, leaving {@code verticalGap} pixels of free room
     * above and below it. The gap-free horizontal flow layout keeps the panel
     * exactly as wide as the component, so the wrapper can drop into a layout
     * wherever the component itself used to sit.
     */
    public FadingPanel(Component content, int verticalGap) {
        super(new FlowLayout(FlowLayout.CENTER, 0, verticalGap));
        self = false;
        setOpaque(false);
        add(content);
    }

    /** 0 = fully transparent, 1 = fully visible. */
    public void setFade(float fade) {
        this.fade = Math.max(0f, Math.min(1f, fade));
        repaint();
    }

    /**
     * The wrapper is exactly as stretchable as the component it replaces:
     * most Swing widgets (labels, buttons) never grow beyond their preferred
     * size in a BoxLayout, so the wrapper must not either - otherwise the
     * page would re-flow around it. A self-fading panel keeps the normal
     * JPanel behaviour, because it is a real page element with its own sizes.
     */
    @Override
    public Dimension getMaximumSize() {
        return self ? super.getMaximumSize() : new Dimension(getPreferredSize());
    }

    @Override
    public void paint(Graphics g) {
        if (!self) {
            super.paint(g);
            return;
        }

        // Self mode: the panel's own drawing and its children fade together.
        Graphics2D g2 = UiKit.translucent(g, fade);
        if (g2 == null) {
            super.paint(g);
            return;
        }
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (self || fade >= 1f) {                // wrapper fully shown (or self mode)
            paintContent(g);
        } else if (fade > 0f) {                  // wrapper in between: translucent
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(fade));
            paintContent(g2);
            g2.dispose();
        }
        // fade == 0: nothing to paint
    }

    /** Paints the wrapped component; subclasses may shift it while painting. */
    protected void paintContent(Graphics g) {
        super.paintChildren(g);
    }

    // ============================================================
    // Fade-aware label
    // ============================================================

    /**
     * A label that can fade in, text and icon together.
     *
     * Main purpose: the menu's and checkout's entrance animations fade the
     * logo and the running total with it. Only the label is affected; the
     * rest of its row stays fully visible.
     */
    public static class FadeLabel extends JLabel {

        /** 0 = invisible, 1 = fully visible. */
        private float fade = 1f;

        public FadeLabel(Icon icon) {
            super(icon);
        }

        public FadeLabel(String text, Icon icon, int horizontalAlignment) {
            super(text, icon, horizontalAlignment);
        }

        /** Sets the opacity and repaints. */
        public void setFade(float fade) {
            fade = Math.max(0f, Math.min(1f, fade));
            if (fade == this.fade
                    || (fade != 0f && fade != 1f && Math.abs(this.fade - fade) < 0.01f)) {
                return;                        // nothing visible would change
            }
            this.fade = fade;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = UiKit.translucent(g, fade);
            if (g2 == null) {
                super.paint(g);
                return;
            }
            super.paint(g2);
            g2.dispose();
        }
    }

    // ============================================================
    // Rising wrapper
    // ============================================================

    /**
     * A {@link FadingPanel} whose content floats upwards while the mouse is
     * over it, then eases back down to its resting place when the mouse
     * leaves.
     *
     * The panel reserves as much free room above the content as it can rise,
     * so the floating copy is never cut off at the panel edge. The hover
     * state is animated along a smooth ease-in-out curve, so the content
     * leaves and reaches its resting place gently instead of jumping.
     *
     * Other content can be made to move along with it through
     * {@link #follower} - for example the pointing hand under an order card.
     */
    public static class RisingPanel extends FadingPanel {

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
         * Wraps {@code content} and floats it up by up to {@code maxRise}
         * pixels while the mouse is over it. The panel is {@code 2 * maxRise}
         * pixels taller than the content, so the raised copy still fits
         * inside it.
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
         * (the pointing-hand pictures have plenty of transparent room above
         * the fingers), otherwise the raised part would be cut off.
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
}
