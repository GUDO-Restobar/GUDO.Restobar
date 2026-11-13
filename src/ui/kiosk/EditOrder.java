package ui.kiosk;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import util.Cart;
import util.OrderLine;
import util.Product;
import util.UiColors;
import util.UiKit;

/**
 * Lets the customer change the quantity, extras and Gudo's Favorites of a meal
 * that is already in the order, or take it out completely.
 *
 * The shared layout and quantity logic live in {@link QuantityPage}; this
 * class only fills in what is different about editing an existing meal.
 *
 * Uses the Prototype pattern: the screen works on a copy of the cart
 * (cart.copy()), so nothing in the real order changes until the customer
 * presses "Save Changes to Order" or "Remove from Order". "Go Back to Order"
 * simply throws the copy away.
 */
public class EditOrder extends QuantityPage {

    /** Scratch copy of the cart; the real one only changes on Save / Remove. */
    private final Cart draft;

    public EditOrder(OrderLine line, Cart cart,
                     List<Product> extras, List<Product> favorites,
                     Runnable onSaved, Runnable onCancel) {
        super(line.getProduct(), line, cart, extras, favorites, onSaved, onCancel);
        this.draft = cart.copy();
    }

    @Override protected String[] pageTitleLines() {
        return new String[] {"Tweak Your Order", "Your Way"};
    }

    /** Starts at the amount that is already in the order. */
    @Override protected int startingQuantity() {
        return line.getQuantity();
    }

    /** The stock, minus what the other lines of this meal already use. */
    @Override protected int maxQuantity() {
        return Math.max(1, cart.maxQuantity(line));
    }

    @Override protected String saveButtonPrefix() {
        return "Save Changes to Order";
    }

    /** Edits the copy first, then commits it to the real cart in one step. */
    @Override protected void commitOrder() {
        draft.updateLine(line, quantity, selectedExtras(), selectedFavorites());
        cart.replaceWith(draft);
    }

    /**
     * "Remove from Order" sits above "Go Back to Order" on the left; the orange
     * save button spans the full height of both, so the bottom row lines up.
     */
    @Override protected JPanel buildButtons() {
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(0, 86));

        JButton remove = buildRemoveButton();
        remove.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        remove.setPreferredSize(new Dimension(0, 34));

        JButton cancel = createCancelButton("Go Back to Order");
        cancel.setMargin(new Insets(0, 6, 0, 6));
        cancel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        cancel.setPreferredSize(new Dimension(0, 44));
        cancel.addActionListener(e -> cancel());

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(180, 86));
        left.setMinimumSize(new Dimension(180, 86));
        remove.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancel.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(remove);
        left.add(Box.createVerticalStrut(8));
        left.add(cancel);

        saveButton = createSaveButton();
        saveButton.setMargin(new Insets(0, 6, 0, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 6);
        buttons.add(left, gbc);
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 6, 0, 0);
        buttons.add(saveButton, gbc);
        return buttons;
    }

    /** Takes this line out of the order. */
    private JButton buildRemoveButton() {
        JButton remove = new JButton("Remove from Order");
        remove.setFont(UiKit.font("Montserrat/static/Montserrat-Regular.ttf").deriveFont(13f));
        remove.setForeground(UiColors.DANGER);
        remove.setBackground(UiColors.BACKGROUND);
        remove.setBorder(BorderFactory.createLineBorder(UiColors.DANGER));
        remove.setFocusPainted(false);
        remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        remove.addActionListener(e -> removeItem());
        return remove;
    }

    /** Drops the meal from the order; the running total recalculates itself. */
    private void removeItem() {
        cart.remove(line);
        onSaved.run();
    }
}
