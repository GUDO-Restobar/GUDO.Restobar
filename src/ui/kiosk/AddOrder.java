package ui.kiosk;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import util.Cart;
import util.Inventory;
import util.Product;

/**
 * "Add Something Extra to Your Meal": shows the tapped product, lets the
 * customer pick a quantity, extras and Gudo's Favorites, and puts it all into
 * the cart as one new order line.
 *
 * The shared layout and quantity logic live in {@link QuantityPage}; this
 * class only fills in what is different about adding a new meal.
 *
 * Editing a meal that is already in the order is handled by {@link EditOrder}.
 */
public class AddOrder extends QuantityPage {

    public AddOrder(Product product, Cart cart,
                    List<Product> extras, List<Product> favorites,
                    Runnable onSaved, Runnable onCancel) {
        super(product, null, cart, extras, favorites, onSaved, onCancel);
    }

    @Override protected String[] pageTitleLines() {
        return new String[] {"Add Something Extra", "to Your Meal"};
    }

    /**
     * The most the customer can add: what is in stock, minus what is already
     * in the order.
     */
    @Override protected int maxQuantity() {
        return Math.max(1, Inventory.getAvailable(product, cart));
    }

    @Override protected String saveButtonPrefix() {
        return "Add to Order";
    }

    /** Always a new line, carrying this page's extras and favorites. */
    @Override protected void commitOrder() {
        cart.add(product, quantity, selectedExtras(), selectedFavorites());
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
