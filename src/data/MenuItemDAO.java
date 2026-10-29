package data;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.Product;

/**
 * Reads the menu and the stock from the MENU_ITEM table.
 * The database is the single source of truth: names, prices, categories and stock.
 */
public final class MenuItemDAO {
    private static final String SELECT_MENU =
        "SELECT itemID, itemName, category, unitPrice, stockQuantity, availabilityStatus "
        + "FROM MENU_ITEM ORDER BY itemID";

    private static final String IMAGE_FOLDER = "public/assets/menu/";

    private MenuItemDAO() {}

    /** Every menu item as a Product, grouped by category ("Meals", "Snacks", "Drinks", "Extras"). */
    public static Map<String, List<Product>> loadMenu() throws SQLException {
        Map<String, List<Product>> menu = new LinkedHashMap<>();
        try (Connection conn = dbconnect.getConnection();
             PreparedStatement statement = conn.prepareStatement(SELECT_MENU);
             ResultSet rows = statement.executeQuery()) {

            while (rows.next()) {
                String name = rows.getString("itemName");
                Product product = new Product(
                    rows.getInt("itemID"),
                    name,
                    rows.getBigDecimal("unitPrice").doubleValue(),
                    imagePathFor(name));
                menu.computeIfAbsent(rows.getString("category"), key -> new ArrayList<>()).add(product);
            }
        }
        return menu;
    }

    /**
     * How many of each item can still be ordered, by itemID. An item the
     * manager marked "Not Available" counts as 0 even if it still has stock.
     */
    public static Map<Integer, Integer> loadStock() throws SQLException {
        Map<Integer, Integer> stock = new HashMap<>();
        try (Connection conn = dbconnect.getConnection();
             PreparedStatement statement = conn.prepareStatement(SELECT_MENU);
             ResultSet rows = statement.executeQuery()) {

            while (rows.next()) {
                boolean available = "Available".equalsIgnoreCase(rows.getString("availabilityStatus"));
                int quantity = available ? Math.max(0, rows.getInt("stockQuantity")) : 0;
                stock.put(rows.getInt("itemID"), quantity);
            }
        }
        return stock;
    }

    /**
     * The picture files are named after the item, e.g. "'Jandro Cola" is
     * public/assets/menu/jandro-cola.png and ".Swing Sisig" is .swing-sisig.png.
     */
    static String imagePathFor(String itemName) {
        String slug = itemName.trim().toLowerCase().replace("'", "").replace(' ', '-');
        return IMAGE_FOLDER + slug + ".png";
    }
}
