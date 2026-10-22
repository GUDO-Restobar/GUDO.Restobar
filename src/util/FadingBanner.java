package util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.Timer;

public class FadingBanner extends JLabel {
    private static final int DISPLAY_MS = 6000;
    private static final int FADE_MS = 800;

    private final BufferedImage[] images;
    private final Timer timer;
    private int currentIndex;
    private int nextIndex = 1;
    private long transitionStart;
    private long lastChange = System.nanoTime();
    private boolean transitioning;

    public FadingBanner(String[] paths, int width, int height) {
        if (paths == null || paths.length == 0) {
            throw new IllegalArgumentException("At least one banner image path is required.");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Banner width and height must be positive.");
        }

        images = new BufferedImage[paths.length];
        for (int i = 0; i < paths.length; i++) {
            try {
                images[i] = ImageIO.read(new File(paths[i]));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                    "Could not load banner image: " + paths[i], e
                );
            }

            if (images[i] == null) {
                throw new IllegalArgumentException(
                    "Unsupported or invalid banner image: " + paths[i]
                );
            }
        }

        setPreferredSize(new Dimension(width, height));
        setOpaque(false);

        timer = new Timer(30, e -> {
            long now = System.nanoTime();

            if (images.length > 1 && !transitioning
                    && now - lastChange >= DISPLAY_MS * 1_000_000L) {
                transitioning = true;
                transitionStart = now;
                nextIndex = (currentIndex + 1) % images.length;
            }

            if (transitioning && now - transitionStart >= FADE_MS * 1_000_000L) {
                currentIndex = nextIndex;
                transitioning = false;
                lastChange = now;
            }

            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        lastChange = System.nanoTime();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.drawImage(images[currentIndex], 0, 0, getWidth(), getHeight(), this);

        if (transitioning) {
            float progress = Math.min(
                1f,
                (System.nanoTime() - transitionStart) / (FADE_MS * 1_000_000f)
            );
            g2.setComposite(AlphaComposite.SrcOver.derive(progress));
            g2.drawImage(images[nextIndex], 0, 0, getWidth(), getHeight(), this);
        }

        g2.dispose();
    }
}
