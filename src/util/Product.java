package util;


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