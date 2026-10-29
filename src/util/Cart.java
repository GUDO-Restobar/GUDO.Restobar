package util;

import java.util.*;

/**
 * The customer's order. Doubles as a Prototype: copy() returns an
 * independent cart that can be edited freely without touching this one.
 */
public class Cart implements Prototype<Cart> {
    private final Map<Product, Integer> items = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public void add(Product product) {
        items.merge(product, 1, Integer::sum);
        fireChanged();
    }

    public void remove(Product product) {
        Integer quantity = items.get(product);

        if (quantity == null) {
            return;
        }

        if (quantity > 1) {
            items.put(product, quantity - 1);
        } else {
            items.remove(product);
        }
        fireChanged();
    }

    public void setQuantity(Product product, int quantity) {
        if (quantity <= 0) {
            items.remove(product);
        } else {
            items.put(product, quantity);
        }
        fireChanged();
    }

    public Map<Product, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public double getTotal(){
        double total = 0.0;

        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            total += product.getPrice() * quantity;
        }

        return total;
    }

    public void clear() {
        items.clear();
        fireChanged();
    }

    /**
     * Prototype: returns an independent copy of this cart. Products are
     * immutable, so they can be shared safely; only the item map is copied.
     * Listeners are deliberately NOT copied, so editing the copy never
     * updates the screens that are watching the real cart.
     */
    @Override
    public Cart copy() {
        Cart clone = new Cart();
        clone.items.putAll(items);
        return clone;
    }

    /** Commits an edited copy back into this cart. */
    public void replaceWith(Cart source) {
        items.clear();
        items.putAll(source.items);
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
