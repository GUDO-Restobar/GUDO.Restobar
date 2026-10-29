package util;

import data.MenuItemDAO;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * The kiosk's copy of the stock in the MENU_ITEM table.
 *
 * {@link #refresh()} reads it from MySQL; the stock itself is reduced by
 * {@code OrderDAO.saveOrder} when an order is completed. Until then, items in
 * the cart only "reserve" part of the stock (see {@link #getAvailable}).
 */
public final class Inventory {
    private static final Map<Integer, Integer> STOCK = new HashMap<>();

    private Inventory() {}

    /** Reloads the stock of every product from the database. */
    public static synchronized void refresh() throws SQLException {
        Map<Integer, Integer> latest = MenuItemDAO.loadStock();
        STOCK.clear();
        STOCK.putAll(latest);
    }

    /** Pieces of this product that can still be ordered. */
    public static synchronized int getStock(Product product) {
        return STOCK.getOrDefault(product.getItemID(), 0);
    }

    /** Pieces of this product that are in stock and not yet in the given cart. */
    public static int getAvailable(Product product, Cart cart) {
        int inCart = cart.getItems().getOrDefault(product, 0);
        return Math.max(0, getStock(product) - inCart);
    }

    public static boolean isInStock(Product product) {
        return getStock(product) > 0;
    }
}
