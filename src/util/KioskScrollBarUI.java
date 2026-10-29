package util;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

// Scroll bar drawn from two images: the frame (track) and the slider (thumb).
public class KioskScrollBarUI extends BasicScrollBarUI {

    private final Image thumbImage;
    private final Image trackImage;

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
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(image, r.x, r.y, r.width, r.height, c);
        g2.dispose();
    }

    private JButton emptyButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        return button;
    }
}