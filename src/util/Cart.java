package util;

import java.util.*;

/**
 * The customer's order: a list of order lines.
 *
 * Main purpose: the single source of truth for what is being bought. Every
 * screen reads the total from here and every screen mutates it through these
 * methods, so the menu, the cart and the checkout page always agree.
 *
 * Adding a meal always creates a NEW line, even if the meal is already in
 * the order, so each line carries its own extras and favorites. Stock is shared
 * by every line, add-ons included.
 *
 * Listeners fire on every change so the screens can redraw themselves.
 */
public class Cart implements Prototype<Cart> {
    private final List<OrderLine> lines = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    /** Adds a finished line. The cart keeps its own copy, so adding the same line twice does nothing. */
    public void add(OrderLine line) {
        if (!lines.contains(line)) {
            lines.add(line.copy());
            fireChanged();
        }
    }

    /** Replaces the line that has the same id with the edited copy. */
    public void update(OrderLine edited) {
        int index = lines.indexOf(edited);
        if (index >= 0) {
            lines.set(index, edited.copy());
            fireChanged();
        }
    }

    /**
     * How many more pieces of a product the working line may still take:
     * the stock, minus what every other line uses, minus what the working
     * line already uses. The working line need not be in the cart yet.
     */
    public int remaining(OrderLine working, Product product) {
        int used = working.countOf(product);
        for (OrderLine line : lines) {
            if (!line.equals(working)) {
                used += line.countOf(product);
            }
        }
        return Inventory.getStock(product) - used;
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

    /** How many pieces of this product are in the order, across all its lines and add-ons. */
    public int quantityOf(Product product) {
        return lines.stream().mapToInt(line -> line.countOf(product)).sum();
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
     * ran out. Earlier lines get the stock first.
     */
    public void trimToStock() {
        Map<Product, Integer> left = new HashMap<>();
        boolean changed = false;
        for (Iterator<OrderLine> it = lines.iterator(); it.hasNext(); ) {
            OrderLine line = it.next();
            changed |= line.trimTo(left);
            if (line.getQuantity() == 0) {
                it.remove();
            }
        }
        if (changed) {
            fireChanged();
        }
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