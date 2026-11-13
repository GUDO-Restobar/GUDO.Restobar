package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One line of the order: a product, how many of it, its own extras and
 * Gudo's Favorites, and its own identity.
 *
 * Main purpose: lets the same product appear on several lines, because each
 * "Add to Order" is a separate line (its extras may differ). Equality is by
 * id, so a copy of a line (see {@link Cart#copy()}) is still the same line.
 */
public class OrderLine {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final int id;
    private final Product product;
    private int quantity;
    /** Paid extras and free sides picked for this line. */
    private final List<AddOn> extras = new ArrayList<>();
    /** Gudo's Favorites picked for this line. */
    private final List<AddOn> favorites = new ArrayList<>();

    OrderLine(Product product, int quantity) {
        this(NEXT_ID.incrementAndGet(), product, quantity);
    }

    private OrderLine(int id, Product product, int quantity) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }

    public int getQuantity() { return quantity; }

    public List<AddOn> getExtras() { return Collections.unmodifiableList(extras); }

    public List<AddOn> getFavorites() { return Collections.unmodifiableList(favorites); }

    /** The meal plus everything picked for this line. */
    public double getSubtotal() {
        double total = product.getPrice() * quantity;
        for (AddOn extra : extras) {
            total += extra.getSubtotal();
        }
        for (AddOn favorite : favorites) {
            total += favorite.getSubtotal();
        }
        return total;
    }

    void setQuantity(int quantity) { this.quantity = quantity; }

    /** Replaces this line's add-ons with independent copies of the given ones. */
    void setAddOns(List<AddOn> newExtras, List<AddOn> newFavorites) {
        extras.clear();
        for (AddOn extra : newExtras) {
            extras.add(extra.copy());
        }
        favorites.clear();
        for (AddOn favorite : newFavorites) {
            favorites.add(favorite.copy());
        }
    }

    OrderLine copy() {
        OrderLine clone = new OrderLine(id, product, quantity);
        clone.setAddOns(extras, favorites);
        return clone;
    }

    @Override public boolean equals(Object o) {
        return o instanceof OrderLine && ((OrderLine) o).id == id;
    }

    @Override public int hashCode() { return id; }
}
