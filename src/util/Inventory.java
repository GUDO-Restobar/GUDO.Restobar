package util;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory product stock (no database yet).
 *
 * Every product starts with {@link #DEFAULT_STOCK} pieces. Stock is only
 * deducted when an order is completed ({@link #deduct(Cart)}); until then the
 * items in the cart simply "reserve" part of it (see {@link #getAvailable}).
 */
public final class Inventory {
    public static final int DEFAULT_STOCK = 20;

    private static final Map<Integer, Integer> STOCK = new HashMap<>();

    private Inventory() {}

    /** Pieces of this product that are still in stock. */
    public static synchronized int getStock(Product product) {
        return STOCK.getOrDefault(product.getItemID(), DEFAULT_STOCK);
    }

    /** Pieces of this product that are in stock and not yet in the given cart. */
    public static int getAvailable(Product product, Cart cart) {
        int inCart = cart.getItems().getOrDefault(product, 0);
        return Math.max(0, getStock(product) - inCart);
    }

    public static boolean isInStock(Product product) {
        return getStock(product) > 0;
    }

    /** Removes the ordered quantities from stock. Call once, when the order is completed. */
    public static synchronized void deduct(Cart cart) {
        for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
            int left = getStock(entry.getKey()) - entry.getValue();
            STOCK.put(entry.getKey().getItemID(), Math.max(0, left));
        }
    }

    /** Puts every product back to the default stock (for testing / a new day). */
    public static synchronized void resetAll() {
        STOCK.clear();
    }
}
