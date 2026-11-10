package ui.kiosk;

import java.awt.*;
import javax.swing.*;
import util.Cart;
import util.Inventory;
import util.Product;

/**
 * "Add Something Extra to Your Meal": shows the tapped product, lets the
 * customer pick a quantity and puts it into the cart.
 *
 * The shared layout and quantity logic live in {@link QuantityPage}; this
 * class only fills in what is different about adding a new meal.
 *
 * Editing a meal that is already in the order is handled by {@link EditOrder}.
 */
public class AddOrder extends QuantityPage {

    public AddOrder(Product product, Cart cart, Runnable onSaved, Runnable onCancel) {
        super(product, cart, onSaved, onCancel);
    }

    @Override protected String pageTitle() {
        return "<html>Add Something Extra<br>to Your Meal</html>";
    }

    /**
     * The most the customer can add: what is in stock, minus what is already
     * in the order.
     */
    @Override protected int maxQuantity() {
        int stock = Inventory.getStock(product);
        stock -= cart.getItems().getOrDefault(product, 0);
        return Math.max(1, stock);
    }

    @Override protected String saveButtonPrefix() {
        return "Add to Order";
    }

    /** Puts the chosen quantity into the cart, one piece at a time. */
    @Override protected void commitOrder() {
        for (int i = 0; i < quantity; i++) {
            cart.add(product);
        }
    }

    /** Nothing extra under the placeholder sections. */
    @Override protected void addDetailsFooter(JPanel details) {
        details.add(Box.createVerticalGlue());
    }

    /** Cancel on the left, the orange "Add to Order" button on the right. */
    @Override protected JPanel buildButtons() {
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(0, 56));

        JButton cancel = createCancelButton("Cancel Order");
        cancel.addActionListener(e -> cancel());

        saveButton = createSaveButton();

        buttons.add(cancel);
        buttons.add(saveButton);
        return buttons;
    }
}
