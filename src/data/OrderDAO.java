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

    // ---------------------------------------------------------------- DELETE

    /**
     * Permanently deletes orders (the DELETE part of the project).
     *
     * PAYMENT and ORDER_DETAIL rows point to CUSTOMER_ORDER with foreign keys,
     * so they are removed first, then the order itself. All in one transaction:
     * either everything for every selected order is gone, or nothing changed.
     * Stock is NOT given back - the food was already made/sold; this is only a
     * clean-up of the records.
     *
     * @return how many orders were deleted
     */
    public static int deleteOrders(java.util.Collection<Integer> orderIDs) throws SQLException {
        if (orderIDs == null || orderIDs.isEmpty()) return 0;

        String[] statements = {
            "DELETE FROM PAYMENT WHERE orderID = ?",
            "DELETE FROM ORDER_DETAIL WHERE orderID = ?",
            "DELETE FROM CUSTOMER_ORDER WHERE orderID = ?"
        };

        try (Connection conn = dbconnect.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int deleted = 0;
                for (int i = 0; i < statements.length; i++) {
                    try (PreparedStatement ps = conn.prepareStatement(statements[i])) {
                        for (int id : orderIDs) {
                            ps.setInt(1, id);
                            ps.addBatch();
                        }
                        int[] counts = ps.executeBatch();
                        if (i == statements.length - 1) {
                            for (int c : counts) deleted += (c == Statement.SUCCESS_NO_INFO ? 1 : c);
                        }
                    }
                }
                conn.commit();
                return deleted;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // --------------------------------------------------------------- RECEIPT

    /** Everything the manager's "digital receipt" shows for one order. */
    public static final class ReceiptData {
        public int orderID;
        public String date, time, orderType, paymentStatus, orderStatus;
        public String method;                       // Cash / Cashless, or null if no payment row
        public BigDecimal total = BigDecimal.ZERO;
        public final java.util.List<Line> lines = new java.util.ArrayList<>();

        public static final class Line {
            public String name;
            public int quantity;
            public BigDecimal price;                // price of one item when it was ordered
            public boolean water, utensils;
            public BigDecimal subTotal() { return price.multiply(BigDecimal.valueOf(quantity)); }
        }
    }

    /** Loads one order with its items. Returns null if the order no longer exists. */
    public static ReceiptData loadReceipt(int orderID) throws SQLException {
        ReceiptData data = new ReceiptData();
        data.orderID = orderID;
        try (Connection conn = dbconnect.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT orderDate, orderTime, orderType, paymentStatus, orderStatus "
                  + "FROM CUSTOMER_ORDER WHERE orderID = ?")) {
                ps.setInt(1, orderID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    data.date = rs.getString("orderDate");
                    data.time = rs.getString("orderTime");
                    data.orderType = rs.getString("orderType");
                    data.paymentStatus = rs.getString("paymentStatus");
                    data.orderStatus = rs.getString("orderStatus");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT method FROM PAYMENT WHERE orderID = ? ORDER BY paymentID LIMIT 1")) {
                ps.setInt(1, orderID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) data.method = rs.getString("method");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT m.itemName, d.quantity, d.price, d.wantWater, d.wantUtensils "
                  + "FROM ORDER_DETAIL d JOIN MENU_ITEM m ON m.itemID = d.itemID "
                  + "WHERE d.orderID = ? ORDER BY d.detailID")) {
                ps.setInt(1, orderID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ReceiptData.Line line = new ReceiptData.Line();
                        line.name = rs.getString("itemName");
                        line.quantity = rs.getInt("quantity");
                        line.price = rs.getBigDecimal("price");
                        line.water = rs.getBoolean("wantWater");
                        line.utensils = rs.getBoolean("wantUtensils");
                        data.lines.add(line);
                        data.total = data.total.add(line.subTotal());
                    }
                }
            }
        }
        return data;
    }

    // ---------------------------------------------------------- STATUS UPDATE

    /** The values CUSTOMER_ORDER.orderStatus accepts (same as its CHECK constraint). */
    public static final String[] ORDER_STATUSES = {"Preparing", "Served", "Completed"};

    /** Changes one order's status. Returns false if the order no longer exists. */
    public static boolean updateOrderStatus(int orderID, String status) throws SQLException {
        boolean valid = false;
        for (String allowed : ORDER_STATUSES) if (allowed.equals(status)) valid = true;
        if (!valid) throw new IllegalArgumentException("Unknown order status: " + status);

        try (Connection conn = dbconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE CUSTOMER_ORDER SET orderStatus = ? WHERE orderID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, orderID);
            return ps.executeUpdate() > 0;
        }
    }
}