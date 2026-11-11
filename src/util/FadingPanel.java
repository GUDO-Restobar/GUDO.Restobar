package util;

import java.awt.*;
import javax.swing.*;

/**
 * A transparent panel that fades the component it wraps in and out.
 *
 * Decorator pattern: the panel sits between a component and its container
 * and paints the component through an {@link AlphaComposite}, so whoever
 * animates a page only has to call {@link #setAlpha} (0 = invisible,
 * 1 = fully shown). Layout, sizes and behaviour of the wrapped component are
 * untouched - only its painting is affected.
 *
 * Subclasses can override {@link #paintContent} to move the wrapped component
 * around while it is being painted (see {@link RisingPanel}).
 */
public class FadingPanel extends JPanel {

    /** Visibility of the wrapped component: 0 = transparent, 1 = opaque. */
    private float alpha = 1f;

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
        setOpaque(false);
        add(content);
    }

    /** 0 = fully transparent, 1 = fully visible. */
    public void setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
        repaint();
    }

    /**
     * The wrapper is exactly as stretchable as the component it replaces:
     * most Swing widgets (labels, buttons) never grow beyond their preferred
     * size in a BoxLayout, so the wrapper must not either - otherwise the
     * page would re-flow around it.
     */
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(getPreferredSize());
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (alpha >= 1f) {                       // fully shown: paint normally
            paintContent(g);
        } else if (alpha > 0f) {                 // in between: paint translucent
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            paintContent(g2);
            g2.dispose();
        }
        // alpha == 0: nothing to paint
    }

    /** Paints the wrapped component; subclasses may shift it while painting. */
    protected void paintContent(Graphics g) {
        super.paintChildren(g);
    }
}
