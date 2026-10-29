package data;

import java.sql.*;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/**
 * Watches the database for changes so the Manager UI can update itself in real time.
 *
 * The Manager and the Kiosk are two separate programs that only share MySQL, so
 * the Manager cannot be "told" about a change. Instead this class runs one very
 * cheap query every {@link #POLL_SECONDS} seconds that produces a fingerprint of
 * the orders and of the stock. When a fingerprint changes, every listener of that
 * kind is called on the Swing thread, and only then is the full data reloaded.
 */
public final class DbWatcher {

    public static final int POLL_SECONDS = 2;

    /** Order rows: how many, the newest ID, and a checksum of each order's statuses. */
    private static final String ORDERS_FINGERPRINT =
        "SELECT COUNT(*), COALESCE(MAX(orderID), 0), "
      + "       COALESCE(SUM(CRC32(CONCAT_WS('|', orderID, orderStatus, paymentStatus, orderType))), 0) "
      + "FROM CUSTOMER_ORDER";

    /** Stock rows: a checksum of every item's quantity and availability. */
    private static final String STOCK_FINGERPRINT =
        "SELECT COUNT(*), "
      + "       COALESCE(SUM(CRC32(CONCAT_WS('|', itemID, stockQuantity, availabilityStatus))), 0) "
      + "FROM MENU_ITEM";

    private static final ScheduledExecutorService POLLER =
        Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "db-watcher");
            thread.setDaemon(true); // never keeps the app open on its own
            return thread;
        });

    private static final Channel ORDERS = new Channel(ORDERS_FINGERPRINT);
    private static final Channel STOCK = new Channel(STOCK_FINGERPRINT);

    private static ScheduledFuture<?> job;
    private static int subscribers;

    private DbWatcher() {}

    /** Calls {@code listener} (on the Swing thread) whenever an order is added or changes. */
    public static Subscription onOrdersChanged(Runnable listener) {
        return subscribe(ORDERS, listener);
    }

    /** Calls {@code listener} (on the Swing thread) whenever any stock value changes. */
    public static Subscription onStockChanged(Runnable listener) {
        return subscribe(STOCK, listener);
    }

    /** Handle that stops a listener. Call {@link #cancel()} when its screen goes away. */
    public static final class Subscription {
        private final Channel channel;
        private final Runnable listener;
        private boolean active = true;

        private Subscription(Channel channel, Runnable listener) {
            this.channel = channel;
            this.listener = listener;
        }

        public void cancel() {
            synchronized (DbWatcher.class) {
                if (!active) return;
                active = false;
                channel.listeners.remove(listener);
                if (--subscribers == 0 && job != null) {
                    job.cancel(false);
                    job = null;
                }
            }
        }
    }

    private static synchronized Subscription subscribe(Channel channel, Runnable listener) {
        channel.listeners.add(listener);
        subscribers++;
        if (job == null) {
            // Forget old fingerprints so the first poll only records a baseline.
            ORDERS.last = null;
            STOCK.last = null;
            job = POLLER.scheduleWithFixedDelay(DbWatcher::poll, POLL_SECONDS, POLL_SECONDS, TimeUnit.SECONDS);
        }
        return new Subscription(channel, listener);
    }

    private static void poll() {
        try (Connection conn = dbconnect.getConnection()) {
            ORDERS.check(conn);
            STOCK.check(conn);
        } catch (SQLException | RuntimeException e) {
            // The database may be briefly unreachable; just try again on the next poll.
        }
    }

    private static final class Channel {
        final String query;
        final java.util.List<Runnable> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
        volatile String last;

        Channel(String query) {
            this.query = query;
        }

        void check(Connection conn) throws SQLException {
            String now;
            try (Statement statement = conn.createStatement();
                 ResultSet row = statement.executeQuery(query)) {
                row.next();
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= row.getMetaData().getColumnCount(); i++) {
                    sb.append(row.getString(i)).append('|');
                }
                now = sb.toString();
            }
            String before = last;
            last = now;
            if (before != null && !Objects.equals(before, now)) {
                for (Runnable listener : listeners) {
                    SwingUtilities.invokeLater(listener);
                }
            }
        }
    }
}
