package util;

import java.awt.*;
import javax.swing.*;

/**
 * A label that can fade in, text and icon together.
 *
 * Main purpose: reusable UI component - the menu's entrance animation fades
 * the logo and the running total with it. Only the label is affected; the
 * rest of its row stays fully visible.
 */
public class FadeLabel extends JLabel {

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
