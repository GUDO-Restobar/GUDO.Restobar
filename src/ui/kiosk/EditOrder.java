package ui.kiosk;

import java.awt.*;
import javax.swing.*;
import util.Cart;
import util.Inventory;
import util.Product;
import util.UiColors;
import util.UiKit;

/**
 * Lets the customer change the quantity of a meal that is already in the
 * order, or take it out completely.
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

    public EditOrder(Product product, Cart cart, Runnable onSaved, Runnable onCancel) {
        super(product, cart, onSaved, onCancel);
        this.draft = cart.copy();
    }

    @Override protected String pageTitle() {
        return "<html>Tweak Your Order<br>Your Way</html>";
    }

    /** Starts at the amount that is already in the order. */
    @Override protected int startingQuantity() {
        return cart.getItems().getOrDefault(product, 1);
    }

    /** The order already holds this meal, so the limit is simply what is in stock. */
    @Override protected int maxQuantity() {
        return Math.max(1, Inventory.getStock(product));
    }

    @Override protected String saveButtonPrefix() {
        return "Save Changes to Order";
    }

    /** Edits the copy first, then commits it to the real cart in one step. */
    @Override protected void commitOrder() {
        draft.setQuantity(product, quantity);
        cart.replaceWith(draft);
    }

    /** The glue plus the "Remove from Order" link. */
    @Override protected void addDetailsFooter(JPanel details) {
        details.add(Box.createVerticalGlue());
        details.add(buildRemoveButton());
    }

    /** Takes the whole meal out of the order. */
    private JButton buildRemoveButton() {
        JButton remove = new JButton("Remove from Order");
        remove.setFont(UiKit.font("Montserrat/static/Montserrat-Regular.ttf").deriveFont(14f));
        remove.setForeground(UiColors.DANGER);
        remove.setBackground(UiColors.BACKGROUND);
        remove.setBorder(BorderFactory.createLineBorder(UiColors.DANGER));
        remove.setFocusPainted(false);
        remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        remove.setAlignmentX(Component.LEFT_ALIGNMENT);
        remove.setPreferredSize(new Dimension(200, 34));
        remove.setMaximumSize(new Dimension(200, 34));
        remove.addActionListener(e -> removeItem());
        return remove;
    }

    /** "Go Back to Order" (40%) and "Save Changes to Order" (60%), so a long
     *  price such as ₱1,074.00 still fits on the save button. */
    @Override protected JPanel buildButtons() {
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(0, 56));

        JButton cancel = createCancelButton("Go Back to Order");
        cancel.setMargin(new Insets(0, 6, 0, 6));
        cancel.addActionListener(e -> cancel());

        saveButton = createSaveButton();
        saveButton.setMargin(new Insets(0, 6, 0, 6));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 0.4;
        gbc.insets = new Insets(0, 0, 0, 6);
        buttons.add(cancel, gbc);
        gbc.weightx = 0.6;
        gbc.insets = new Insets(0, 6, 0, 0);
        buttons.add(saveButton, gbc);
        return buttons;
    }

    /** Drops the meal from the order; the running total recalculates itself. */
    private void removeItem() {
        cart.setQuantity(product, 0);
        onSaved.run();
    }
}
