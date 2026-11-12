package ui.kiosk;

/**
 * Confirmation shown after "Save Changes to Order". It hands control back to
 * KioskMain after the same delay as {@link OrderAdded}; the page itself is
 * built by {@link OrderConfirmation}.
 */
public class OrderEdited extends OrderConfirmation {

    public OrderEdited(double total, Runnable onFinished) {
        super("Your Cravings has", "Been Modified", total, onFinished);
    }
}