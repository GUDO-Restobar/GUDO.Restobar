package ui.kiosk;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import util.*;

/**
 * "Add Something Extra to Your Meal": shows the tapped product, lets the
 * customer pick a quantity, extras and favorites, and puts the result into the
 * cart as a new line.
 *
 * The shared layout and rules live in {@link QuantityPage}; this class only
 * fills in what is different about adding a new meal.
 * Editing a meal that is already in the order is handled by {@link EditOrder}.
 */
public class AddOrder extends QuantityPage {

    public AddOrder(Product product, Cart cart, List<Product> extras, List<Product> favorites,
                    Runnable onSaved, Runnable onCancel) {
        super(new OrderLine(product, 1), cart, extras, favorites, onSaved, onCancel);
    }

    @Override protected String[] titleLines() {
        return new String[] {"Add Something Extra", "to Your Meal"};
    }

    @Override protected String saveButtonPrefix() {
        return "Add to Order";
    }

    /** Always a new line, even if the product is already in the order. */
    @Override protected void commitOrder() {
        cart.add(work);
    }

    /** Cancel on the left, the orange "Add to Order" button on the right. */
    @Override protected FadeablePanel buildButtons() {
        FadeablePanel buttons = buttonBar(new GridLayout(1, 2, 12, 0), 56);

        JButton cancel = createOutlineButton("Cancel Order", UiColors.BROWN);
        cancel.addActionListener(e -> cancel());

        saveButton = createSaveButton();

        buttons.add(cancel);
        buttons.add(saveButton);
        return buttons;
    }
}