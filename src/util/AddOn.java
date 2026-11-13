package util;

/**
 * Something added on top of one order line: a paid extra, a free side such as
 * water or cutlery, or a Gudo's Favorite.
 *
 * Main purpose: keeps the add-ons with the line they belong to, so the same
 * meal can appear twice in the order with different extras. A line's subtotal
 * and the cart total include their prices.
 */
public class AddOn {

    /** The menu item behind a favorite; null for a free side. */
    private final Product product;
    private final String name;
    private final double price;
    private int quantity;

    private AddOn(Product product, String name, double price, int quantity) {
        this.product = product;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    /** A paid extra or a free side, e.g. {@code extra("Cutlery", 0)}. Starts unpicked. */
    public static AddOn extra(String name, double price) {
        return new AddOn(null, name, price, 0);
    }

    /** A Gudo's Favorite taken straight from the menu. Starts unpicked. */
    public static AddOn favorite(Product product) {
        return new AddOn(product, product.getName(), product.getPrice(), 0);
    }

    public Product getProduct() { return product; }

    public String getName() { return name; }

    public double getPrice() { return price; }

    /** Free sides (water, cutlery) are switched on and off instead of counted. */
    public boolean isFree() { return price <= 0; }

    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = Math.max(0, quantity); }

    public double getSubtotal() { return price * quantity; }

    /** The text the checkout card shows: "Extra Rice x2", or just "Cutlery" when free. */
    public String getLabel() {
        return isFree() ? name : name + " x" + quantity;
    }

    public AddOn copy() {
        return new AddOn(product, name, price, quantity);
    }
}
