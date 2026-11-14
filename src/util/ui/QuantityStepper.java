package util.ui;

import java.awt.*;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import javax.swing.*;

/**
 * The "- 3 +" control: two circle buttons around the current amount.
 *
 * Main purpose: reusable UI component. It owns only the number and the two
 * buttons; what the number means (a meal, an extra, a favourite) and how
 * many may still be added is decided by its owner through the constructor
 * callbacks, so the same control works everywhere.
 */
public class QuantityStepper extends JPanel {

    private final int min;
    private final BooleanSupplier canIncrease;
    private final IntConsumer onChange;
    private final JLabel label = new JLabel("", SwingConstants.CENTER);
    private final CircleButton minus;
    private final CircleButton plus;
    private int value;

    /**
     * @param value       the amount shown at first
     * @param min         the lowest amount the minus button goes down to
     * @param size        diameter of the circle buttons
     * @param font        font of the number
     * @param canIncrease asked every time the plus button is pressed or redrawn
     * @param onChange    called with the new amount after a press
     */
    public QuantityStepper(int value, int min, int size, Font font,
                           BooleanSupplier canIncrease, IntConsumer onChange) {
        super(new GridBagLayout());          // centres the row both ways, even in a taller slot
        this.value = value;
        this.min = min;
        this.canIncrease = canIncrease;
        this.onChange = onChange;

        setOpaque(false);
        label.setFont(font);
        label.setForeground(UiColors.ORANGE);
        label.setPreferredSize(new Dimension(size * 2, size));

        minus = new CircleButton(false, size);
        plus = new CircleButton(true, size);
        minus.addActionListener(e -> step(-1));
        plus.addActionListener(e -> step(1));

        GridBagConstraints gap = new GridBagConstraints();
        gap.insets = new Insets(0, size / 4, 0, size / 4);
        add(minus, gap);
        add(label, gap);
        add(plus, gap);
        refresh();
    }

    public int getValue() {
        return value;
    }

    private void step(int change) {
        if (change < 0 ? value <= min : !canIncrease.getAsBoolean()) {
            return;
        }
        value += change;
        onChange.accept(value);
        refresh();
    }

    /** Re-reads the amount and greys out whichever button can no longer be used. */
    public void refresh() {
        label.setText(String.valueOf(value));
        minus.setEnabled(value > min);
        plus.setEnabled(canIncrease.getAsBoolean());
    }
}
