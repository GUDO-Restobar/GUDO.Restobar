package util;

import java.util.*;

public class Cart {
    private final Map<Product, Integer> items = new LinkedHashMap<>();

    public void add(Product product) {
        items.merge(product, 1, Integer::sum);
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
    }

}
