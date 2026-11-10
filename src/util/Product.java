package util;

/**
 * One item on the menu, as shown on the menu grid and in the order.
 *
 * Main purpose: an immutable value object (id, name, price, picture) that
 * every screen shares. Equality is by itemID only, so the same meal from
 * different screens is always the same key inside a {@link Cart}.
 */
public class Product {
    private final int itemID;
    private final String name;
    private final double price;
    private final String imagePath;

    public Product(int itemID, String name, double price, String imagePath) {
        this.itemID = itemID;
        this.name = name;
        this.price = price;
        this.imagePath = imagePath;
    }

    public int getItemID() {
        return itemID;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getImagePath() {
        return imagePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product)) {
            return false;
        }
        Product other = (Product) o;
        return itemID == other.itemID;
    }

    @Override
    public int hashCode() {
        return itemID;
    }
}