package data;

import java.math.BigDecimal;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import util.Cart;
import util.Product;

/**
 * Saves a finished order: CUSTOMER_ORDER, ORDER_DETAIL and PAYMENT rows, and
 * takes the ordered quantities out of MENU_ITEM.stockQuantity.
 *
 * Everything happens in ONE transaction, so an order is either saved completely
 * (and the stock is reduced) or not at all.
 */
public final class OrderDAO {

    /** Thrown when an item ran out of stock while the order was being placed. */
    public static class OutOfStockException extends Exception {
        private final String itemName;

        public OutOfStockException(String itemName) {
            super(itemName + " is no longer available in the requested quantity.");
            this.itemName = itemName;
        }

        public String getItemName() {
            return itemName;
        }
    }

    // The status is read before the stock is changed (MySQL applies SET from left to right),
    // so an item that hits 0 is flagged 'Not Available' in the same statement.
    private static final String DECREASE_STOCK =
        "UPDATE MENU_ITEM "
        + "SET availabilityStatus = CASE WHEN stockQuantity - ? <= 0 THEN 'Not Available' ELSE availabilityStatus END, "
        + "    stockQuantity = stockQuantity - ? "
        + "WHERE itemID = ? AND availabilityStatus = 'Available' AND stockQuantity >= ?";

    private static final String INSERT_ORDER =
        "INSERT INTO CUSTOMER_ORDER (orderDate, orderTime, orderType, totalAmount, paymentStatus, orderStatus) "
        + "VALUES (CURDATE(), CURTIME(), ?, ?, 'Paid', 'Preparing')";

    private static final String INSERT_DETAIL =
        "INSERT INTO ORDER_DETAIL (orderID, itemID, quantity, price) VALUES (?, ?, ?, ?)";

    private static final String INSERT_PAYMENT =
        "INSERT INTO PAYMENT (orderID, method, transactionStatus, amount, paymentDate) "
        + "VALUES (?, ?, 'Successful', ?, CURDATE())";

    private OrderDAO() {}

    /**
     * @param orderType     "Dine-in" or "Takeout"
     * @param paymentMethod "Cash" or "Cashless"
     * @return the new orderID
     */
    public static int saveOrder(Cart cart, String orderType, String paymentMethod)
            throws SQLException, OutOfStockException {

        Map<Product, Integer> items = new LinkedHashMap<>(cart.getItems());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("The order is empty.");
        }

        String type = "Takeout".equalsIgnoreCase(orderType) || "Take-out".equalsIgnoreCase(orderType)
            ? "Takeout" : "Dine-in";
        String method = "Cashless".equalsIgnoreCase(paymentMethod) ? "Cashless" : "Cash";

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            total = total.add(BigDecimal.valueOf(entry.getKey().getPrice())
                .multiply(BigDecimal.valueOf(entry.getValue())));
        }

        try (Connection conn = dbconnect.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1) Take the items out of stock. Fails if someone else bought them first.
                try (PreparedStatement stock = conn.prepareStatement(DECREASE_STOCK)) {
                    for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                        int quantity = entry.getValue();
                        stock.setInt(1, quantity);
                        stock.setInt(2, quantity);
                        stock.setInt(3, entry.getKey().getItemID());
                        stock.setInt(4, quantity);
                        if (stock.executeUpdate() == 0) {
                            throw new OutOfStockException(entry.getKey().getName());
                        }
                    }
                }

                // 2) The order itself.
                int orderID;
                try (PreparedStatement order =
                         conn.prepareStatement(INSERT_ORDER, Statement.RETURN_GENERATED_KEYS)) {
                    order.setString(1, type);
                    order.setBigDecimal(2, total);
                    order.executeUpdate();
                    try (ResultSet keys = order.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("The database did not return an order ID.");
                        }
                        orderID = keys.getInt(1);
                    }
                }

                // 3) One ORDER_DETAIL row per item (subTotal is calculated by MySQL).
                try (PreparedStatement detail = conn.prepareStatement(INSERT_DETAIL)) {
                    for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                        detail.setInt(1, orderID);
                        detail.setInt(2, entry.getKey().getItemID());
                        detail.setInt(3, entry.getValue());
                        detail.setBigDecimal(4, BigDecimal.valueOf(entry.getKey().getPrice()));
                        detail.addBatch();
                    }
                    detail.executeBatch();
                }

                // 4) The payment.
                try (PreparedStatement payment = conn.prepareStatement(INSERT_PAYMENT)) {
                    payment.setInt(1, orderID);
                    payment.setString(2, method);
                    payment.setBigDecimal(3, total);
                    payment.executeUpdate();
                }

                conn.commit();
                return orderID;
            } catch (SQLException | OutOfStockException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
