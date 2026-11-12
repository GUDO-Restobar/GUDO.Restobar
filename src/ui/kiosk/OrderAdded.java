package ui.kiosk;

/**
 * Confirmation shown after an item was added to the order. It returns to the
 * menu by itself after {@link #AUTO_REDIRECT_MILLIS}; the page itself is
 * built by {@link OrderConfirmation}.
 */
public class OrderAdded extends OrderConfirmation {

    public OrderAdded(double total, Runnable onBackToMenu) {
        super("Your Cravings has", "Been Answered", total, onBackToMenu);
    }
}