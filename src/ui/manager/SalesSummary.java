package ui.manager;

import static ui.manager.ManagerTheme.*;

import data.DbWatcher;
import data.dbconnect;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import util.KioskScrollBarUI;

/**
 * "Sales Summary" screen, laid out like a small dashboard:
 *
 *   - four KPI cards (total sales, orders, average order value, low-stock items)
 *   - "Business Insights": the 10 business questions on the left; click one and its
 *     SQL query runs and the answer appears on the right.
 *
 * Each Query still records the SQL feature it demonstrates (WHERE, JOIN, GROUP BY, HAVING,
 * subquery, ORDER BY, AVG, ...) for the project's query table, but the screen doesn't show it.
 * Everything refreshes by itself when orders or stock change.
 */
public class SalesSummary extends JPanel {

    private static final Color ROW_LINE = new Color(232, 232, 232);
    private static final Color TEXT_GRAY = new Color(110, 110, 110);
    private static final Color GREEN = new Color(46, 125, 50);
    private static final Color RED = new Color(198, 40, 40);
    private static final String THUMB = "public/assets/scrollbar/scroll-thumb.png";
    private static final String TRACK = "public/assets/scrollbar/scroll-track.png";

    /** Stock below this number counts as "low" in the KPI card. */
    private static final int REORDER_LEVEL = 5;

    private enum Param { NONE, NUMBER, MONTH }

    /** One business question. "?" in the SQL are filled from the parameter control. */
    private static final class Query {
        final String question, feature, sql;
        final Param param;
        final String paramLabel;
        final int paramDefault, paramMin, paramMax;

        Query(String question, String feature, String sql,
              Param param, String paramLabel, int paramDefault, int paramMin, int paramMax) {
            this.question = question;
            this.feature = feature;
            this.sql = sql;
            this.param = param;
            this.paramLabel = paramLabel;
            this.paramDefault = paramDefault;
            this.paramMin = paramMin;
            this.paramMax = paramMax;
        }

        Query(String question, String feature, String sql) {
            this(question, feature, sql, Param.NONE, null, 0, 0, 0);
        }
    }

    private static final Query[] QUERIES = {
        // 1 - WHERE
        new Query("Which menu items are low on stock?", "WHERE",
            "SELECT itemID AS 'Item ID', itemName AS 'Item', category AS 'Category', stockQuantity AS 'In Stock' "
          + "FROM MENU_ITEM WHERE stockQuantity < ? ORDER BY stockQuantity, itemID",
            Param.NUMBER, "Reorder level (below)", REORDER_LEVEL, 0, 1000),

        // 2 - JOIN + COUNT + SUM + GROUP BY (+ WHERE on the month)
        new Query("How much did we earn each day this month?", "JOIN + SUM + GROUP BY",
            "SELECT o.orderDate AS 'Date', COUNT(DISTINCT o.orderID) AS 'Orders', SUM(d.subTotal) AS 'Total Sales' "
          + "FROM CUSTOMER_ORDER o JOIN ORDER_DETAIL d ON d.orderID = o.orderID "
          + "WHERE YEAR(o.orderDate) = ? AND MONTH(o.orderDate) = ? "
          + "GROUP BY o.orderDate ORDER BY o.orderDate",
            Param.MONTH, "Month", 0, 0, 0),

        // 3 - JOIN + SUM + GROUP BY + ORDER BY + LIMIT
        new Query("What are the five best-selling items?", "JOIN + SUM + GROUP BY + ORDER BY + LIMIT",
            "SELECT m.itemName AS 'Item', m.category AS 'Category', SUM(d.quantity) AS 'Units Sold', "
          + "SUM(d.subTotal) AS 'Total Sales' "
          + "FROM ORDER_DETAIL d JOIN MENU_ITEM m ON m.itemID = d.itemID "
          + "GROUP BY m.itemID, m.itemName, m.category "
          + "ORDER BY SUM(d.quantity) DESC, m.itemName LIMIT 5"),

        // 4 - Subquery
        new Query("Which menu items have never been ordered?", "Subquery",
            "SELECT m.itemID AS 'Item ID', m.itemName AS 'Item', m.category AS 'Category', m.unitPrice AS 'Price' "
          + "FROM MENU_ITEM m WHERE m.itemID NOT IN (SELECT d.itemID FROM ORDER_DETAIL d) "
          + "ORDER BY m.itemID"),

        // 5 - JOIN + SUM + GROUP BY
        new Query("Which category generated the highest sales?", "JOIN + SUM + GROUP BY",
            "SELECT m.category AS 'Category', SUM(d.subTotal) AS 'Total Sales' "
          + "FROM ORDER_DETAIL d JOIN MENU_ITEM m ON m.itemID = d.itemID "
          + "GROUP BY m.category ORDER BY SUM(d.subTotal) DESC"),

        // 6 - JOIN + COUNT + SUM + GROUP BY
        new Query("Dine-in or takeout: which brings in more?", "JOIN + COUNT + SUM + GROUP BY",
            "SELECT o.orderType AS 'Order Type', COUNT(DISTINCT o.orderID) AS 'Orders', "
          + "SUM(d.subTotal) AS 'Total Sales' "
          + "FROM CUSTOMER_ORDER o JOIN ORDER_DETAIL d ON d.orderID = o.orderID "
          + "GROUP BY o.orderType ORDER BY SUM(d.subTotal) DESC"),

        // 7 - JOIN + COUNT + GROUP BY
        new Query("Which payment method is used most?", "JOIN + COUNT + GROUP BY",
            "SELECT p.method AS 'Payment Method', COUNT(*) AS 'Orders' "
          + "FROM CUSTOMER_ORDER o JOIN PAYMENT p ON p.orderID = o.orderID "
          + "GROUP BY p.method ORDER BY COUNT(*) DESC"),

        // 8 - JOIN + WHERE
        new Query("Which orders are unpaid or not yet completed?", "JOIN + WHERE",
            "SELECT o.orderID AS 'Order ID', o.orderDate AS 'Date', o.orderType AS 'Type', "
          + "o.paymentStatus AS 'Payment Status', o.orderStatus AS 'Order Status', p.amount AS 'Amount' "
          + "FROM CUSTOMER_ORDER o JOIN PAYMENT p ON p.orderID = o.orderID "
          + "WHERE o.paymentStatus <> 'Paid' OR o.orderStatus <> 'Completed' "
          + "ORDER BY o.orderID DESC"),

        // 9 - Subquery + AVG + HAVING
        new Query("Which orders are bigger than the average order?", "Subquery + AVG + HAVING",
            "SELECT o.orderID AS 'Order ID', o.orderDate AS 'Date', o.orderType AS 'Type', "
          + "SUM(d.subTotal) AS 'Order Total' "
          + "FROM CUSTOMER_ORDER o JOIN ORDER_DETAIL d ON d.orderID = o.orderID "
          + "GROUP BY o.orderID, o.orderDate, o.orderType "
          + "HAVING SUM(d.subTotal) > (SELECT AVG(t.total) FROM "
          + "(SELECT SUM(subTotal) AS total FROM ORDER_DETAIL GROUP BY orderID) t) "
          + "ORDER BY SUM(d.subTotal) DESC"),

        // 10 - GROUP BY + HAVING
        new Query("Which items were ordered more than N times?", "JOIN + GROUP BY + HAVING",
            "SELECT m.itemName AS 'Item', m.category AS 'Category', SUM(d.quantity) AS 'Times Ordered' "
          + "FROM ORDER_DETAIL d JOIN MENU_ITEM m ON m.itemID = d.itemID "
          + "GROUP BY m.itemID, m.itemName, m.category HAVING SUM(d.quantity) > ? "
          + "ORDER BY SUM(d.quantity) DESC, m.itemName",
            Param.NUMBER, "More than (times)", 3, 0, 10000),
    };

    /** Column names + rows of one finished query. */
    private static final class Result {
        final List<String> columns = new ArrayList<>();
        final List<Object[]> rows = new ArrayList<>();
    }

    /** Numbers for the KPI cards. */
    private static final class Dashboard {
        double totalSales, averageOrder;
        int orders, lowStock;
    }

    // ---- widgets
    private final JLabel kpiSales = new JLabel("-"), kpiOrders = new JLabel("-"),
                         kpiAverage = new JLabel("-"), kpiLow = new JLabel("-");

    private final JList<Query> questionList = new JList<>(QUERIES);
    private final DefaultTableModel resultModel = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel resultTitle = new JLabel();
    private final JLabel status = new JLabel();
    private final JPanel paramBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel paramLabel = new JLabel();
    private final JSpinner numberSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 1000, 1));
    private final JComboBox<YearMonth> monthBox = new JComboBox<>();

    private int runCounter;                       // ignores answers that arrive after a newer click
    private DbWatcher.Subscription ordersWatch, stockWatch;

    public SalesSummary() {
        setLayout(null);
        setBackground(MAINWHITE);

        // ---------------------------------------------------------------- header
        JLabel heading = new JLabel("SALES SUMMARY");
        heading.setFont(BOLD.deriveFont(33f));
        heading.setForeground(MAINORANGE);
        heading.setBounds(33, 26, 600, 44);
        add(heading);

        JLabel subtitle = new JLabel("GUDO.Restobar • Business Overview");
        subtitle.setFont(REGULAR.deriveFont(12f));
        subtitle.setForeground(TEXT_GRAY);
        subtitle.setBounds(35, 72, 500, 18);
        add(subtitle);

        // ---------------------------------------------------------------- KPI cards
        addKpi(33,  "Total Sales",         kpiSales,   "Paid transactions",    MAINORANGE);
        addKpi(273, "Total Orders",        kpiOrders,  "All orders",           Color.BLACK);
        addKpi(513, "Average Order Value", kpiAverage, "Per order",            Color.BLACK);
        addKpi(753, "Low-stock Items",     kpiLow,     "Below " + REORDER_LEVEL + " in stock", RED);

        // ---------------------------------------------------------------- business insights
        JLabel insights = new JLabel("Business Insights");
        insights.setFont(BOLD.deriveFont(16f));
        insights.setBounds(33, 214, 400, 24);
        add(insights);

        questionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionList.setFixedCellHeight(62);
        questionList.setCellRenderer(new QuestionRenderer());
        questionList.setFocusable(false);
        questionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selectQuery();
        });
        JScrollPane listScroll = new JScrollPane(questionList,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setBorder(BorderFactory.createLineBorder(ROW_LINE));
        listScroll.getViewport().setBackground(Color.WHITE);
        listScroll.setBounds(33, 244, 400, 622);       // 10 rows x 62 + borders
        add(listScroll);

        // ---- right: the answer
        resultTitle.setFont(BOLD.deriveFont(15f));
        resultTitle.setForeground(MAINORANGE);
        resultTitle.setBounds(451, 244, 524, 24);
        add(resultTitle);

        status.setFont(REGULAR.deriveFont(12f));
        status.setForeground(TEXT_GRAY);
        status.setBounds(451, 269, 524, 18);
        add(status);

        paramBar.setOpaque(false);
        paramBar.setBounds(443, 290, 532, 34);
        paramLabel.setFont(BOLD.deriveFont(12f));
        paramLabel.setForeground(TEXT_GRAY);
        numberSpinner.setFont(BOLD.deriveFont(13f));
        numberSpinner.setPreferredSize(new Dimension(80, 28));
        monthBox.setFont(BOLD.deriveFont(13f));
        monthBox.setFocusable(false);
        monthBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, formatMonth((YearMonth) v), i, s, f);
            }
        });
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) monthBox.addItem(now.minusMonths(i));   // this month first
        paramBar.add(paramLabel);
        paramBar.add(numberSpinner);
        paramBar.add(monthBox);
        add(paramBar);
        numberSpinner.addChangeListener(e -> runSelected());
        monthBox.addActionListener(e -> runSelected());

        JTable resultTable = new JTable(resultModel);
        styleTable(resultTable);
        JScrollPane resultScroll = new JScrollPane(resultTable);
        resultScroll.getViewport().setBackground(Color.WHITE);
        resultScroll.setBorder(BorderFactory.createLineBorder(ROW_LINE));
        JScrollBar bar = resultScroll.getVerticalScrollBar();
        bar.setUnitIncrement(24);
        bar.setUI(new KioskScrollBarUI(THUMB, TRACK));
        bar.setPreferredSize(new Dimension(26, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 8));
        bar.setBackground(Color.WHITE);
        resultScroll.setBounds(451, 330, 524, 536);
        add(resultScroll);

        // Real time: refresh the cards and the open answer when orders or stock change.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED) == 0) return;
            if (isDisplayable() && ordersWatch == null) {
                ordersWatch = DbWatcher.onOrdersChanged(this::refreshAll);
                stockWatch = DbWatcher.onStockChanged(this::refreshAll);
            } else if (!isDisplayable() && ordersWatch != null) {
                ordersWatch.cancel();
                stockWatch.cancel();
                ordersWatch = null;
                stockWatch = null;
            }
        });

        refreshDashboard();
        questionList.setSelectedIndex(0);
    }

    private void refreshAll() {
        refreshDashboard();
        runSelected();
    }

    // ------------------------------------------------------------------ dashboard (KPI cards)

    private void addKpi(int x, String title, JLabel value, String caption, Color valueColor) {
        Card card = new Card();
        card.setLayout(null);
        card.setBounds(x, 104, 222, 92);

        JLabel t = new JLabel(title);
        t.setFont(REGULAR.deriveFont(12f));
        t.setForeground(TEXT_GRAY);
        t.setBounds(18, 10, 190, 16);
        value.setFont(BOLD.deriveFont(26f));
        value.setForeground(valueColor);
        value.setBounds(18, 28, 190, 36);
        JLabel c = new JLabel(caption);
        c.setFont(REGULAR.deriveFont(11f));
        c.setForeground(TEXT_GRAY);
        c.setBounds(18, 66, 190, 16);

        card.add(t);
        card.add(value);
        card.add(c);
        add(card);
    }

    private void refreshDashboard() {
        new SwingWorker<Dashboard, Void>() {
            @Override protected Dashboard doInBackground() throws Exception {
                return loadDashboard();
            }
            @Override protected void done() {
                try {
                    Dashboard d = get();
                    kpiSales.setText(money(d.totalSales));
                    kpiOrders.setText(String.valueOf(d.orders));
                    kpiAverage.setText(money(d.averageOrder));
                    kpiLow.setText(String.valueOf(d.lowStock));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private static Dashboard loadDashboard() throws SQLException {
        Dashboard d = new Dashboard();
        try (Connection conn = dbconnect.getConnection(); Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT COALESCE(SUM(amount), 0) FROM PAYMENT WHERE transactionStatus = 'Successful'")) {
                if (rs.next()) d.totalSales = rs.getDouble(1);
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM CUSTOMER_ORDER")) {
                if (rs.next()) d.orders = rs.getInt(1);
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT COALESCE(AVG(x.total), 0) FROM "
                  + "(SELECT SUM(subTotal) AS total FROM ORDER_DETAIL GROUP BY orderID) x")) {
                if (rs.next()) d.averageOrder = rs.getDouble(1);
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM MENU_ITEM WHERE stockQuantity < " + REORDER_LEVEL)) {
                if (rs.next()) d.lowStock = rs.getInt(1);
            }
        }
        return d;
    }

    // ------------------------------------------------------------------ running a question

    /** A different question was picked: set up its parameter control, then run it. */
    private void selectQuery() {
        int row = questionList.getSelectedIndex();
        if (row < 0) return;
        Query q = QUERIES[row];

        paramLabel.setText(q.param == Param.NONE ? "" : q.paramLabel);
        numberSpinner.setVisible(q.param == Param.NUMBER);
        monthBox.setVisible(q.param == Param.MONTH);
        paramLabel.setVisible(q.param != Param.NONE);
        if (q.param == Param.NUMBER) {
            // Only whole numbers inside a sensible range are accepted; anything else is rejected by the spinner.
            numberSpinner.setModel(new SpinnerNumberModel(q.paramDefault, q.paramMin, q.paramMax, 1));
        }
        if (q.param == Param.MONTH) monthBox.setSelectedIndex(0);
        paramBar.revalidate();
        paramBar.repaint();

        resultTitle.setText("Q" + (row + 1) + ".  " + q.question);
        runSelected();
    }

    private void runSelected() {
        int row = questionList.getSelectedIndex();
        if (row < 0) return;
        Query q = QUERIES[row];
        int run = ++runCounter;

        // read the controls on the Swing thread, query on a background thread
        int number = ((Number) numberSpinner.getValue()).intValue();
        YearMonth month = (YearMonth) monthBox.getSelectedItem();

        new SwingWorker<Result, Void>() {
            @Override protected Result doInBackground() throws Exception {
                return runQuery(q, number, month);
            }
            @Override protected void done() {
                if (run != runCounter) return;            // a newer query is already running
                try {
                    show(get(), q);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    resultModel.setDataVector(new Object[0][0], new Object[0]);
                    status.setForeground(RED);
                    status.setText("Could not run this query: " + rootMessage(ex));
                }
            }
        }.execute();
    }

    private static Result runQuery(Query q, int number, YearMonth month) throws SQLException {
        Result result = new Result();
        try (Connection conn = dbconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(q.sql)) {
            if (q.param == Param.NUMBER) {
                ps.setInt(1, number);
            } else if (q.param == Param.MONTH) {
                ps.setInt(1, month.getYear());
                ps.setInt(2, month.getMonthValue());
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                for (int i = 1; i <= n; i++) result.columns.add(md.getColumnLabel(i));
                while (rs.next()) {
                    Object[] row = new Object[n];
                    for (int i = 1; i <= n; i++) {
                        Object v = rs.getObject(i);
                        row[i - 1] = isMoney(result.columns.get(i - 1)) && v instanceof Number
                            ? money(((Number) v).doubleValue())
                            : (v instanceof BigDecimal ? v.toString() : v);
                    }
                    result.rows.add(row);
                }
            }
        }
        return result;
    }

    private void show(Result result, Query q) {
        resultModel.setDataVector(result.rows.toArray(new Object[0][]), result.columns.toArray());
        status.setForeground(TEXT_GRAY);
        if (result.rows.isEmpty()) {
            status.setText("No rows match" + (q.param == Param.NONE ? "." : " – try changing the value above."));
        } else {
            status.setText(result.rows.size() + (result.rows.size() == 1 ? " row" : " rows"));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static boolean isMoney(String label) {
        return label.contains("Amount") || label.contains("Sales") || label.contains("Price") || label.contains("Total");
    }

    private static String money(double v) {
        return "₱" + String.format("%,.2f", v);
    }

    private static String formatMonth(YearMonth ym) {
        return ym == null ? "" : ym.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage();
    }

    /** Orange header, white rows with thin dividers - same look as the Order History table. */
    private static void styleTable(JTable table) {
        table.setRowHeight(36);
        table.setFont(REGULAR.deriveFont(13f));
        table.setBackground(Color.WHITE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);

        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 40));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setFont(BOLD.deriveFont(12f));
                l.setForeground(MAINWHITE);
                l.setBackground(MAINORANGE);
                l.setOpaque(true);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
                return l;
            }
        });

        DefaultTableCellRenderer cells = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setBackground(Color.WHITE);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ROW_LINE),
                    BorderFactory.createEmptyBorder(0, 14, 0, 8)));
                boolean money = isMoney(t.getColumnName(c));
                l.setFont((money ? BOLD : REGULAR).deriveFont(13f));
                l.setForeground(money ? Color.BLACK : new Color(70, 70, 70));
                return l;
            }
        };
        table.setDefaultRenderer(Object.class, cells);
        table.setDefaultRenderer(Integer.class, cells);
        table.setDefaultRenderer(Long.class, cells);
    }

    // ------------------------------------------------------------------ small components

    /** White rounded card with a thin border. */
    private static class Card extends JPanel {
        Card() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(ROW_LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.dispose();
        }
    }

    /** Row of the question list: number badge, question, SQL feature. */
    private static class QuestionRenderer implements ListCellRenderer<Query> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Query> list, Query q, int index,
                                                      boolean selected, boolean focus) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setBackground(selected ? new Color(253, 235, 222) : Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, selected ? 4 : 0, 1, 0, selected ? MAINORANGE : ROW_LINE),
                BorderFactory.createEmptyBorder(0, selected ? 10 : 14, 0, 12)));

            JLabel number = new JLabel(String.valueOf(index + 1), SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(MAINORANGE);
                    g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            number.setFont(BOLD.deriveFont(12f));
            number.setForeground(Color.WHITE);
            number.setPreferredSize(new Dimension(26, 26));
            JPanel numberHolder = new JPanel(new GridBagLayout());
            numberHolder.setOpaque(false);
            numberHolder.add(number);

            JLabel text = new JLabel("<html><body style='width:290px'><b>" + q.question + "</b></body></html>");
            text.setFont(REGULAR.deriveFont(13f));

            row.add(numberHolder, BorderLayout.WEST);
            row.add(text, BorderLayout.CENTER);
            return row;
        }
    }
}