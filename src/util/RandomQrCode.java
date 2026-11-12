package util;

import java.awt.*;
import java.util.Random;
import javax.swing.JComponent;

/**
 * A QR-code-style picture that is drawn at random: every call to
 * {@link #regenerate()} gives a new pattern.
 *
 * Decorative only - it is NOT scannable. The three corner squares are fixed;
 * every other square is black or white by chance.
 */
public class RandomQrCode extends JComponent {

    private static final int MODULES = 25;          // squares per side
    private static final int MODULE_PX = 11;        // size of one square
    private static final int QUIET_PX = 12;         // white margin around the code
    private static final int SIZE = MODULES * MODULE_PX + 2 * QUIET_PX;
    private static final int SHADOW = 3;

    private static final int FINDER = 7;            // corner square, 7 x 7 squares
    private static final int[][] FINDER_ORIGINS = {
        {0, 0}, {0, MODULES - FINDER}, {MODULES - FINDER, 0}};

    private final boolean[][] modules = new boolean[MODULES][MODULES];
    private final Random random = new Random();

    public RandomQrCode() {
        setPreferredSize(new Dimension(SIZE, SIZE + SHADOW));
        regenerate();
    }

    /** Fills the code with a new random pattern. */
    public void regenerate() {
        for (boolean[] row : modules) {
            for (int col = 0; col < MODULES; col++) {
                row[col] = random.nextBoolean();
            }
        }
        for (int[] origin : FINDER_ORIGINS) {
            drawFinder(origin[0], origin[1]);
        }
        repaint();
    }

    /** A 7 x 7 corner square (dark ring, light ring, dark centre) with a clear border. */
    private void drawFinder(int row, int col) {
        for (int dr = -1; dr <= FINDER; dr++) {
            for (int dc = -1; dc <= FINDER; dc++) {
                int r = row + dr;
                int c = col + dc;
                if (r < 0 || c < 0 || r >= MODULES || c >= MODULES) {
                    continue;
                }
                boolean inside = dr >= 0 && dr < FINDER && dc >= 0 && dc < FINDER;
                int ring = Math.max(Math.abs(dr - FINDER / 2), Math.abs(dc - FINDER / 2));
                modules[r][c] = inside && ring != 2;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(UiColors.shadow(40));
        g2.fillRect(0, SHADOW, SIZE, SIZE);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, SIZE, SIZE);
        g2.setColor(UiColors.DISABLED);
        g2.drawRect(0, 0, SIZE - 1, SIZE - 1);

        g2.setColor(Color.BLACK);
        for (int row = 0; row < MODULES; row++) {
            for (int col = 0; col < MODULES; col++) {
                if (modules[row][col]) {
                    g2.fillRect(QUIET_PX + col * MODULE_PX, QUIET_PX + row * MODULE_PX, MODULE_PX, MODULE_PX);
                }
            }
        }
        g2.dispose();
    }
}