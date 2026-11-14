package util.feat;

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
 *
 * Composite pattern: an OrderLine is the leaf of an order (see
 * {@link OrderComponent}). Builder pattern: {@link Builder} assembles a line
 * step by step, which keeps the constructor short and makes {@link #copy()}
 * easy to read.
 */
public class OrderLine implements Prototype<OrderLine>, OrderComponent {
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

    /** Starts a Builder for a new line of this product. */
    public static Builder builder(Product product) {
        return new Builder(product);
    }

    /**
     * Builder pattern: every method sets one part of the line and returns the
     * builder, so a caller can chain them and only call {@link #build()} when
     * the line is complete.
     */
    public static final class Builder {
        private final Product product;
        private int id;
        private int quantity = 1;
        private final Map<Product, Integer> extras = new LinkedHashMap<>();
        private final Map<Product, Integer> favorites = new LinkedHashMap<>();
        private boolean water;
        private boolean cutlery;

        private Builder(Product product) {
            this.product = product;
        }

        /** Keeps the identity of an existing line; used by {@link OrderLine#copy()}. */
        private Builder id(int id) { this.id = id; return this; }

        public Builder quantity(int quantity) { this.quantity = quantity; return this; }

        public Builder extra(Product extra, int amount) { put(extras, extra, amount); return this; }

        public Builder favorite(Product favorite, int amount) { put(favorites, favorite, amount); return this; }

        public Builder water(boolean water) { this.water = water; return this; }

        public Builder cutlery(boolean cutlery) { this.cutlery = cutlery; return this; }

        private Builder extras(Map<Product, Integer> items) { extras.putAll(items); return this; }

        private Builder favorites(Map<Product, Integer> items) { favorites.putAll(items); return this; }

        /** Creates the finished line. A new id is assigned unless one was supplied. */
        public OrderLine build() {
            int lineID = id != 0 ? id : NEXT_ID.incrementAndGet();
            OrderLine line = new OrderLine(lineID, product, quantity,
                new LinkedHashMap<>(extras), new LinkedHashMap<>(favorites));
            line.water = water;
            line.cutlery = cutlery;
            return line;
        }
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
        return builder(product)
            .id(id)
            .quantity(quantity)
            .extras(extras)
            .favorites(favorites)
            .water(water)
            .cutlery(cutlery)
            .build();
    }

    @Override public boolean equals(Object o) {
        return o instanceof OrderLine && ((OrderLine) o).id == id;
    }

    @Override public int hashCode() { return id; }
}
