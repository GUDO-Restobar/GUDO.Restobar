package util;

import java.util.*;

/**
 * The customer's order: a list of order lines.
 *
 * Main purpose: the single source of truth for what is being bought. Every
 * screen reads the total from here and every screen mutates it through these
 * methods, so the menu, the cart and the checkout page always agree.
 *
 * Adding a product always creates a NEW line, even if the product is already
 * in the order, so each line can later carry its own extras. Stock is shared
 * by all lines of the same product.
 *
 * Listeners fire on every change so the screens can redraw themselves.
 */
public class Cart implements Prototype<Cart> {
    private final List<OrderLine> lines = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    /** Adds a new line without extras, capped by the stock not yet used by other lines. */
    public OrderLine add(Product product, int quantity) {
        return add(product, quantity, List.of(), List.of());
    }

    /**
     * Adds a new line with its own extras and favorites, capped by the stock
     * not yet used by other lines. Returns the new line, or null when there is
     * no stock left.
     */
    public OrderLine add(Product product, int quantity,
                         List<AddOn> extras, List<AddOn> favorites) {
        int capped = Math.min(quantity, Inventory.getAvailable(product, this));
        if (capped <= 0) {
            return null;
        }
        OrderLine line = new OrderLine(product, capped);
        line.setAddOns(extras, favorites);
        lines.add(line);
        fireChanged();
        return line;
    }

    /** Changes a line's quantity and add-ons in one step; 0 (or less) removes it. */
    public void updateLine(OrderLine line, int quantity,
                           List<AddOn> extras, List<AddOn> favorites) {
        int index = lines.indexOf(line);
        if (index < 0) {
            return;
        }
        int capped = Math.min(quantity, maxQuantity(line));
        if (capped <= 0) {
            lines.remove(index);
        } else {
            OrderLine target = lines.get(index);
            target.setQuantity(capped);
            target.setAddOns(extras, favorites);
        }
        fireChanged();
    }

    /** Sets a line's quantity; 0 (or less) removes it. Never above its share of the stock. */
    public void setQuantity(OrderLine line, int quantity) {
        int capped = Math.min(quantity, maxQuantity(line));
        if (capped <= 0) {
            lines.remove(line);
        } else {
            lines.get(lines.indexOf(line)).setQuantity(capped);
        }
        fireChanged();
    }

    public void remove(OrderLine line) {
        setQuantity(line, 0);
    }

    /** The most this line can hold: the stock minus what the other lines of the product use. */
    public int maxQuantity(OrderLine line) {
        int others = quantityOf(line.getProduct()) - line.getQuantity();
        return Inventory.getStock(line.getProduct()) - others;
    }

    /** An unmodifiable view of the lines, in the order they were added. */
    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** How many pieces of this product are in the order, across all its lines. */
    public int quantityOf(Product product) {
        return lines.stream()
            .filter(line -> line.getProduct().equals(product))
            .mapToInt(OrderLine::getQuantity)
            .sum();
    }

    /** The running total of every line. */
    public double getTotal() {
        return lines.stream().mapToDouble(OrderLine::getSubtotal).sum();
    }

    /** Throws the whole order away (used by Start Over). */
    public void clear() {
        lines.clear();
        fireChanged();
    }

    /**
     * Prototype: returns an independent copy of this cart. Lines are copied
     * (they keep their identity); listeners are deliberately NOT copied, so
     * editing the copy never updates the screens watching the real cart.
     */
    @Override
    public Cart copy() {
        Cart clone = new Cart();
        lines.forEach(line -> clone.lines.add(line.copy()));
        return clone;
    }

    /**
     * Lowers quantities to what is in stock right now and drops lines that
     * ran out. Earlier lines of a product get the stock first.
     */
    public void trimToStock() {
        Map<Product, Integer> remaining = new HashMap<>();
        boolean changed = false;
        for (Iterator<OrderLine> it = lines.iterator(); it.hasNext(); ) {
            OrderLine line = it.next();
            int left = remaining.computeIfAbsent(line.getProduct(), Inventory::getStock);
            if (left <= 0) {
                it.remove();
                changed = true;
                continue;
            }
            if (line.getQuantity() > left) {
                line.setQuantity(left);
                changed = true;
            }
            remaining.put(line.getProduct(), left - line.getQuantity());
        }
        if (changed) {
            fireChanged();
        }
    }

    /** Commits an edited copy back into this cart. */
    public void replaceWith(Cart source) {
        lines.clear();
        source.lines.forEach(line -> lines.add(line.copy()));
        fireChanged();
    }

    /** Runs the given action every time the contents of this cart change. */
    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void fireChanged() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }
}