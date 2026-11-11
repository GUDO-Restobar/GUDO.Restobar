package util;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Scroll bar drawn from two images: the frame (track) and the slider (thumb).
 *
 * Main purpose: reusable UI component - drop it on any JScrollPane with
 * {@code bar.setUI(new KioskScrollBarUI(thumbPath, trackPath))} to give that
 * scroll bar the kiosk's look. The arrow buttons are hidden.
 */
public class KioskScrollBarUI extends BasicScrollBarUI {

    private final Image thumbImage;
    private final Image trackImage;

    /** 0 = invisible, 1 = fully visible (the menu's entrance fade). */
    private float fade = 1f;

    public KioskScrollBarUI(String thumbPath, String trackPath) {
        thumbImage = new ImageIcon(thumbPath).getImage();
        trackImage = new ImageIcon(trackPath).getImage();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return emptyButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return emptyButton();
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(10, 44);
    }

    /** Sets the opacity of the track and thumb (used by the menu's entrance fade). */
    public void setFade(float fade) {
        fade = Math.max(0f, Math.min(1f, fade));
        if (fade == this.fade
                || (fade != 0f && fade != 1f && Math.abs(this.fade - fade) < 0.01f)) {
            return;                        // nothing visible would change
        }
        this.fade = fade;
        if (scrollbar != null) {
            scrollbar.repaint();
        }
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        draw(g, trackImage, r, c);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (!r.isEmpty() && scrollbar.isEnabled()) {
            draw(g, thumbImage, r, c);
        }
    }

    private void draw(Graphics g, Image image, Rectangle r, JComponent c) {
        if (fade <= 0f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (fade < 1f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
        }
        g2.drawImage(image, r.x, r.y, r.width, r.height, c);
        g2.dispose();
    }

    private JButton emptyButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        return button;
    }
}