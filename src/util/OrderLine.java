package util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One line of the order: a meal, how many of it, and everything ordered
 * together with it - extras, favorites, extra water and cutlery.
 *
 * Main purpose: lets the same meal appear on several lines, each with its own
 * add-ons, which are saved with this line only and add up to its subtotal.
 * Equality is by id, so a copy of a line (see {@link #copy()}) is still the
 * same line. Add-ons use up stock just like meals do.
 */
public class OrderLine implements Prototype<OrderLine> {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final int id;
    private final Product product;
    private int quantity;
    private final Map<Product, Integer> extras;
    private final Map<Product, Integer> favorites;
    private boolean water;
    private boolean cutlery;

    public OrderLine(Product product, int quantity) {
        this(NEXT_ID.incrementAndGet(), product, quantity, new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    private OrderLine(int id, Product product, int quantity,
                      Map<Product, Integer> extras, Map<Product, Integer> favorites) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.extras = extras;
        this.favorites = favorites;
    }

    public Product getProduct() { return product; }

    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    // ------------------------------------------------------------ add-ons

    public int getExtra(Product extra) { return extras.getOrDefault(extra, 0); }

    /** Sets how many of an extra (0 removes it). */
    public void setExtra(Product extra, int amount) { put(extras, extra, amount); }

    public Map<Product, Integer> getExtras() { return Collections.unmodifiableMap(extras); }

    public int getFavorite(Product favorite) { return favorites.getOrDefault(favorite, 0); }

    /** Sets how many of a favorite (0 removes it). */
    public void setFavorite(Product favorite, int amount) { put(favorites, favorite, amount); }

    public Map<Product, Integer> getFavorites() { return Collections.unmodifiableMap(favorites); }

    public boolean isWater() { return water; }

    public void setWater(boolean water) { this.water = water; }

    public boolean isCutlery() { return cutlery; }

    public void setCutlery(boolean cutlery) { this.cutlery = cutlery; }

    private static void put(Map<Product, Integer> map, Product product, int amount) {
        if (amount > 0) {
            map.put(product, amount);
        } else {
            map.remove(product);
        }
    }

    // ------------------------------------------------------------ totals and stock

    /** The meal plus every extra and favorite on this line. */
    public double getSubtotal() {
        return product.getPrice() * quantity + cost(extras) + cost(favorites);
    }

    private static double cost(Map<Product, Integer> items) {
        return items.entrySet().stream()
            .mapToDouble(item -> item.getKey().getPrice() * item.getValue())
            .sum();
    }

    /** How many pieces of this product the line uses: as the meal, as an extra, as a favorite. */
    public int countOf(Product other) {
        return (product.equals(other) ? quantity : 0) + getExtra(other) + getFavorite(other);
    }

    /** Everything on the line as product -> pieces, the meal first. Used to save and to take stock. */
    public Map<Product, Integer> pieces() {
        Map<Product, Integer> all = new LinkedHashMap<>();
        all.put(product, quantity);
        extras.forEach((item, amount) -> all.merge(item, amount, Integer::sum));
        favorites.forEach((item, amount) -> all.merge(item, amount, Integer::sum));
        return all;
    }

    /**
     * Lowers every amount to what is left of the stock, using that stock up.
     * If not even one meal is left the quantity ends at 0 and the cart drops the line.
     *
     * @return true if anything had to be lowered
     */
    boolean trimTo(Map<Product, Integer> left) {
        int meals = take(left, product, quantity);
        boolean changed = meals != quantity;
        quantity = meals;
        if (meals == 0) {
            return changed;
        }
        return trim(extras, left) | trim(favorites, left) | changed;
    }

    private static boolean trim(Map<Product, Integer> items, Map<Product, Integer> left) {
        boolean changed = false;
        for (Iterator<Map.Entry<Product, Integer>> it = items.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Product, Integer> item = it.next();
            int taken = take(left, item.getKey(), item.getValue());
            if (taken != item.getValue()) {
                changed = true;
                if (taken == 0) {
                    it.remove();
                } else {
                    item.setValue(taken);
                }
            }
        }
        return changed;
    }

    private static int take(Map<Product, Integer> left, Product item, int wanted) {
        int available = left.computeIfAbsent(item, Inventory::getStock);
        int taken = Math.min(wanted, available);
        left.put(item, available - taken);
        return taken;
    }

    // ------------------------------------------------------------ identity

    /** True if the other line holds exactly the same meal, amounts, add-ons and choices. */
    public boolean sameContentAs(OrderLine other) {
        return product.equals(other.product) && quantity == other.quantity
            && extras.equals(other.extras) && favorites.equals(other.favorites)
            && water == other.water && cutlery == other.cutlery;
    }

    @Override
    public OrderLine copy() {
        OrderLine clone = new OrderLine(id, product, quantity,
            new LinkedHashMap<>(extras), new LinkedHashMap<>(favorites));
        clone.water = water;
        clone.cutlery = cutlery;
        return clone;
    }

    @Override public boolean equals(Object o) {
        return o instanceof OrderLine && ((OrderLine) o).id == id;
    }

    @Override public int hashCode() { return id; }
}
