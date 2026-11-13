package ui.kiosk;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import util.*;

/**
 * Lets the customer change a meal that is already in the order - its quantity,
 * extras and favorites - or take it out completely.
 *
 * The shared layout and rules live in {@link QuantityPage}; this class only
 * fills in what is different about editing, namely the "Remove from Order"
 * button.
 *
 * Uses the Prototype pattern: the page works on a copy of the line
 * ({@code line.copy()}), so nothing in the real order changes until the customer
 * presses "Save Changes to Order" or "Remove from Order". "Go Back to Order"
 * simply throws the copy away.
 */
public class EditOrder extends QuantityPage {

    /** The line in the real order; the page itself edits a copy of it. */
    private final OrderLine line;

    public EditOrder(OrderLine line, Cart cart, List<Product> extras, List<Product> favorites,
                     Runnable onSaved, Runnable onCancel) {
        super(line.copy(), cart, extras, favorites, onSaved, onCancel);
        this.line = line;
    }

    @Override protected String[] titleLines() {
        return new String[] {"Tweak Your Order", "Your Way"};
    }

    @Override protected String saveButtonPrefix() {
        return "Save Changes to Order";
    }

    /** Nothing to save until the customer changes something. */
    @Override protected boolean canSave() {
        return hasChanges();
    }

    /** Puts the edited copy in place of the original line. */
    @Override protected void commitOrder() {
        cart.update(work);
    }

    /**
     * "Remove from Order" above "Go Back to Order" on the left (40%), and the
     * orange save button on the right (60%) as tall as both of them together.
     */
    @Override protected FadeablePanel buildButtons() {
        FadeablePanel buttons = buttonBar(new GridBagLayout(), 96);

        JButton remove = createOutlineButton("Remove from Order", UiColors.DANGER);
        remove.addActionListener(e -> removeItem());
        JButton back = createOutlineButton("Go Back to Order", UiColors.BROWN);
        back.addActionListener(e -> cancel());
        saveButton = createSaveButton();
        saveButton.setMargin(new Insets(0, 6, 0, 6));

        // Preferred widths in the 40 : 60 ratio, so the extra room splits the same way.
        remove.setPreferredSize(new Dimension(40, 44));
        back.setPreferredSize(new Dimension(40, 44));
        saveButton.setPreferredSize(new Dimension(60, 96));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.4;
        gbc.insets = new Insets(0, 0, 4, 6);
        buttons.add(remove, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 0, 6);
        buttons.add(back, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 2; gbc.weightx = 0.6;
        gbc.insets = new Insets(0, 6, 0, 0);
        buttons.add(saveButton, gbc);
        return buttons;
    }

    /** Takes the meal, with its add-ons, out of the order. */
    private void removeItem() {
        cart.remove(line);
        onSaved.run();
    }
}