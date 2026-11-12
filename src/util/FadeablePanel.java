package util;

import java.awt.*;
import javax.swing.JPanel;

/**
 * A panel that can fade in, together with everything painted on it.
 *
 * Main purpose: reusable base class for page elements that animate their
 * entrance (order cards, the empty-order box). Subclasses only call
 * {@link #setFade}; painting through a translucent context fades the panel's
 * own drawing and all of its children in one go.
 */
public class FadeablePanel extends JPanel {

    /** 0 = invisible, 1 = fully visible. */
    private float fade = 1f;

    /** Sets the opacity and repaints. */
    public void setFade(float fade) {
        this.fade = Math.max(0f, Math.min(1f, fade));
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