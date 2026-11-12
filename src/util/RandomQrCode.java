package util;

import java.awt.*;
import java.io.File;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * Shows one of the real QR code pictures from public/data/qr, chosen at random.
 * Call {@link #pickRandom()} to choose another one (never the same one twice
 * in a row); adding more .png files to the folder adds more choices.
 *
 * The pictures are square and drawn into a square, so they are never stretched,
 * and they already carry their own white margin, which a scanner needs.
 */
public class RandomQrCode extends JComponent {

    private static final File FOLDER = new File("public/data/qr");
    private static final int SIZE = 299;
    private static final int SHADOW = 3;

    private final File[] files = FOLDER.listFiles((dir, name) -> name.endsWith(".png"));
    private final Random random = new Random();

    private File current;
    private ImageIcon picture;

    public RandomQrCode() {
        if (files == null || files.length == 0) {
            throw new IllegalStateException("No QR code pictures found in " + FOLDER);
        }
        setPreferredSize(new Dimension(SIZE, SIZE + SHADOW));
        pickRandom();
    }

    /** Switches to another random QR code picture. */
    public void pickRandom() {
        File next;
        do {
            next = files[random.nextInt(files.length)];
        } while (files.length > 1 && next.equals(current));

        current = next;
        picture = UiKit.icon(next.getPath(), SIZE, SIZE);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(UiColors.shadow(40));
        g2.fillRect(0, SHADOW, SIZE, SIZE);
        picture.paintIcon(this, g2, 0, 0);
        g2.setColor(UiColors.DISABLED);
        g2.drawRect(0, 0, SIZE - 1, SIZE - 1);
        g2.dispose();
    }
}