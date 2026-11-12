package util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * One line of the order: a product, how many of it, and its own identity.
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

    public double getSubtotal() { return product.getPrice() * quantity; }

    void setQuantity(int quantity) { this.quantity = quantity; }

    OrderLine copy() { return new OrderLine(id, product, quantity); }

    @Override public boolean equals(Object o) {
        return o instanceof OrderLine && ((OrderLine) o).id == id;
    }

    @Override public int hashCode() { return id; }
}