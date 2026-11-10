package ui.manager;

import static ui.manager.ManagerTheme.*;

import data.DbWatcher;
import data.dbconnect;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import util.UiColors;

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

    /** Stock below this number counts as "low" in the KPI card. */
    private static final int REORDER_LEVEL = 5;

    private enum Param { NONE, NUMBER, MONTH }

    /** One business question. "?" in the SQL are filled from the parameter control. */
    private static final class Query {
        final String question, sql;
        final Param param;
        final String paramLabel;
        final int paramDefault, paramMin, paramMax;

        Query(String question, String sql, Param param, String paramLabel, int paramDefault, int paramMin, int paramMax) {
            this.question = question;
            this.sql = sql;
            this.param = param;
            this.paramLabel = paramLabel;
            this.paramDefault = paramDefault;
            this.paramMin = paramMin;
            this.paramMax = paramMax;
        }

        Query(String question, String sql) {
            this(question, sql, Param.NONE, null, 0, 0, 0);
        }
    }

    private static final Query[] QUERIES = {
        // 1 - WHERE
        new Query("Which menu items are low on stock?",
            "SELECT itemID AS 'Item ID', itemName AS 'Item', category AS 'Category', stockQuantity AS 'In Stock' "
          + "FROM MENU_ITEM WHERE stockQuantity < ? ORDER BY stockQuantity, itemID",
            Param.NUMBER, "Reorder level (below)", REORDER_LEVEL, 0, 1000),

        // 2 - JOIN + COUNT + SUM + GROUP BY (+ WHERE on the month)
        new Query("How much did we earn each day this month?",
            "SELECT o.orderDate AS 'Date', COUNT(DISTINCT o.orderID) AS 'Orders', SUM(d.subTotal) AS 'Total Sales' "
          + "FROM CUSTOMER_ORDER o JOIN ORDER_DETAIL d ON d.orderID = o.orderID "
          + "WHERE YEAR(o.orderDate) = ? AND MONTH(o.orderDate) = ? "
          + "GROUP BY o.orderDate ORDER BY o.orderDate",
            Param.MONTH, "Month", 0, 0, 0),

        // 3 - JOIN + SUM + GROUP BY + ORDER BY + LIMIT
        new Query("What are the five best-selling items?",
            "SELECT m.itemName AS 'Item', m.category AS 'Category', SUM(d.quantity) AS 'Units Sold', "
          + "SUM(d.subTotal) AS 'Total Sales' "
          + "FROM ORDER_DETAIL d JOIN MENU_ITEM m ON m.itemID = d.itemID "
          + "GROUP BY m.itemID, m.itemName, m.category "
          + "ORDER BY SUM(d.quantity) DESC, m.itemName LIMIT 5"),

        // 4 - Subquery
        new Query("Which menu items have never been ordered?",
            "SELECT m.itemID AS 'Item ID', m.itemName AS 'Item', m.category AS 'Category', m.unitPrice AS 'Price' "
          + "FROM MENU_ITEM m WHERE m.itemID NOT IN (SELECT d.itemID FROM ORDER_DETAIL d) "
          + "ORDER BY m.itemID"),

        // 5 - JOIN + SUM + GROUP BY
        new Query("Which category generated the highest sales?",
            "SELECT m.category AS 'Category', SUM(d.subTotal) AS 'Total Sales' "
          + "FROM ORDER_DETAIL d JOIN MENU_ITEM m ON m.itemID = d.itemID "
          + "GROUP BY m.category ORDER BY SUM(d.subTotal) DESC"),

        // 6 - JOIN + COUNT + SUM + GROUP BY
        new Query("Dine-in or takeout: which brings in more?",
            "SELECT o.orderType AS 'Order Type', COUNT(DISTINCT o.orderID) AS 'Orders', "
          + "SUM(d.subTotal) AS 'Total Sales' "
          + "FROM CUSTOMER_ORDER o JOIN ORDER_DETAIL d ON d.orderID = o.orderID "
          + "GROUP BY o.orderType ORDER BY SUM(d.subTotal) DESC"),

        // 7 - JOIN + COUNT + GROUP BY
        new Query("Which payment method is used most?",
            "SELECT p.method AS 'Payment Method', COUNT(*) AS 'Orders' "
          + "FROM CUSTOMER_ORDER o JOIN PAYMENT p ON p.orderID = o.orderID "
          + "GROUP BY p.method ORDER BY COUNT(*) DESC"),

        // 8 - JOIN + WHERE
        new Query("Which orders are unpaid or not yet completed?",
            "SELECT o.orderID AS 'Order ID', o.orderDate AS 'Date', o.orderType AS 'Type', "
          + "o.paymentStatus AS 'Payment Status', o.orderStatus AS 'Order Status', p.amount AS 'Amount' "
          + "FROM CUSTOMER_ORDER o JOIN PAYMENT p ON p.orderID = o.orderID "
          + "WHERE o.paymentStatus <> 'Paid' OR o.orderStatus <> 'Completed' "
          + "ORDER BY o.orderID DESC"),

        // 9 - Subquery + AVG + HAVING
        new Query("Which orders are bigger than the average order?",
            "SELECT o.orderID AS 'Order ID', o.orderDate AS 'Date', o.orderType AS 'Type', "
          + "SUM(d.subTotal) AS 'Order Total' "
          + "FROM CUSTOMER_ORDER o JOIN ORDER_DETAIL d ON d.orderID = o.orderID "
          + "GROUP BY o.orderID, o.orderDate, o.orderType "
          + "HAVING SUM(d.subTotal) > (SELECT AVG(t.total) FROM "
          + "(SELECT SUM(subTotal) AS total FROM ORDER_DETAIL GROUP BY orderID) t) "
          + "ORDER BY SUM(d.subTotal) DESC"),

        // 10 - GROUP BY + HAVING
        new Query("Which items were ordered more than N times?",
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
        setBackground(UiColors.BACKGROUND);

        // ---------------------------------------------------------------- header
        JLabel heading = new JLabel("SALES SUMMARY");
        heading.setFont(BOLD.deriveFont(33f));
        heading.setForeground(UiColors.ORANGE);
        heading.setBounds(33, 26, 600, 44);
        add(heading);

        JLabel subtitle = new JLabel("GUDO.Restobar • Business Overview");
        subtitle.setFont(REGULAR.deriveFont(12f));
        subtitle.setForeground(TEXT_GRAY);
        subtitle.setBounds(35, 72, 500, 18);
        add(subtitle);

        // ---------------------------------------------------------------- KPI cards
        addKpi(33,  "Total Sales",         kpiSales,   "Paid transactions",    UiColors.ORANGE);
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
        resultTitle.setForeground(UiColors.ORANGE);
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
        styleScrollBar(resultScroll, Color.WHITE);    // shared kiosk-style scroll bar (ManagerTheme)
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
        Dashboard dashBoard = new Dashboard();
        try (Connection conn = dbconnect.getConnection(); Statement st = conn.createStatement()) {
            try (ResultSet resultSet = st.executeQuery(
                    "SELECT COALESCE(SUM(amount), 0) FROM PAYMENT WHERE transactionStatus = 'Successful'")) {
                if (resultSet.next()) dashBoard.totalSales = resultSet.getDouble(1);
            }
            try (ResultSet resultSet = st.executeQuery("SELECT COUNT(*) FROM CUSTOMER_ORDER")) {
                if (resultSet.next()) dashBoard.orders = resultSet.getInt(1);
            }
            try (ResultSet resultSet = st.executeQuery(
                    "SELECT COALESCE(AVG(x.total), 0) FROM "
                  + "(SELECT SUM(subTotal) AS total FROM ORDER_DETAIL GROUP BY orderID) x")) {
                if (resultSet.next()) dashBoard.averageOrder = resultSet.getDouble(1);
            }
            try (ResultSet resultSet = st.executeQuery(
                    "SELECT COUNT(*) FROM MENU_ITEM WHERE stockQuantity < " + REORDER_LEVEL)) {
                if (resultSet.next()) dashBoard.lowStock = resultSet.getInt(1);
            }
        }
        return dashBoard;
    }

    // ------------------------------------------------------------------ running a question

    /** A different question was picked: set up its parameter control, then run it. */
    private void selectQuery() {
        int row = questionList.getSelectedIndex();
        if (row < 0) return;
        Query query = QUERIES[row];

        paramLabel.setText(query.param == Param.NONE ? "" : query.paramLabel);
        numberSpinner.setVisible(query.param == Param.NUMBER);
        monthBox.setVisible(query.param == Param.MONTH);
        paramLabel.setVisible(query.param != Param.NONE);
        if (query.param == Param.NUMBER) {
            // Only whole numbers inside a sensible range are accepted; anything else is rejected by the spinner.
            numberSpinner.setModel(new SpinnerNumberModel(query.paramDefault, query.paramMin, query.paramMax, 1));
        }
        if (query.param == Param.MONTH) monthBox.setSelectedIndex(0);
        paramBar.revalidate();
        paramBar.repaint();

        resultTitle.setText("Q" + (row + 1) + ".  " + query.question);
        runSelected();
    }

    private void runSelected() {
        int row = questionList.getSelectedIndex();
        if (row < 0) return;
        Query query = QUERIES[row];
        int run = ++runCounter;

        // read the controls on the Swing thread, query on a background thread
        int number = ((Number) numberSpinner.getValue()).intValue();
        YearMonth month = (YearMonth) monthBox.getSelectedItem();

        new SwingWorker<Result, Void>() {
            @Override protected Result doInBackground() throws Exception {
                return runQuery(query, number, month);
            }
            @Override protected void done() {
                if (run != runCounter) return;            // a newer query is already running
                try {
                    show(get(), query);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    resultModel.setDataVector(new Object[0][0], new Object[0]);
                    status.setForeground(RED);
                    status.setText("Could not run this query: " + rootMessage(ex));
                }
            }
        }.execute();
    }

    private static Result runQuery(Query query, int number, YearMonth month) throws SQLException {
        Result result = new Result();
        try (Connection conn = dbconnect.getConnection();
             PreparedStatement preppedStmt = conn.prepareStatement(query.sql)) {
            if (query.param == Param.NUMBER) {
                preppedStmt.setInt(1, number);
            } else if (query.param == Param.MONTH) {
                preppedStmt.setInt(1, month.getYear());
                preppedStmt.setInt(2, month.getMonthValue());
            }
            try (ResultSet resultSet = preppedStmt.executeQuery()) {
                ResultSetMetaData rsMetaData = resultSet.getMetaData();
                int num = rsMetaData.getColumnCount();
                for (int i = 1; i <= num; i++) result.columns.add(rsMetaData.getColumnLabel(i));
                while (resultSet.next()) {
                    Object[] row = new Object[num];
                    for (int i = 1; i <= num; i++) {
                        Object obj = resultSet.getObject(i);
                        row[i - 1] = isMoney(result.columns.get(i - 1)) && obj instanceof Number
                            ? money(((Number) obj).doubleValue())
                            : (obj instanceof BigDecimal ? obj.toString() : obj);
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

    private static String money(double amount) {
        return "₱" + String.format("%,.2f", amount);
    }

    private static String formatMonth(YearMonth yrMon) {
        return yrMon == null ? "" : yrMon.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
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
                JLabel headerLabel = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                headerLabel.setFont(BOLD.deriveFont(12f));
                headerLabel.setForeground(UiColors.BACKGROUND);
                headerLabel.setBackground(UiColors.ORANGE);
                headerLabel.setOpaque(true);
                headerLabel.setHorizontalAlignment(SwingConstants.LEFT);
                headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
                return headerLabel;
            }
        });

        DefaultTableCellRenderer cells = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel cellsLabel = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                cellsLabel.setBackground(Color.WHITE);
                cellsLabel.setHorizontalAlignment(SwingConstants.LEFT);
                cellsLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ROW_LINE),
                    BorderFactory.createEmptyBorder(0, 14, 0, 8)));
                boolean money = isMoney(t.getColumnName(c));
                cellsLabel.setFont((money ? BOLD : REGULAR).deriveFont(13f));
                cellsLabel.setForeground(money ? Color.BLACK : new Color(70, 70, 70));
                return cellsLabel;
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
            Graphics2D graphics = (Graphics2D) g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            graphics.setColor(ROW_LINE);
            graphics.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            graphics.dispose();
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
                BorderFactory.createMatteBorder(0, selected ? 4 : 0, 1, 0, selected ? UiColors.ORANGE : ROW_LINE),
                BorderFactory.createEmptyBorder(0, selected ? 10 : 14, 0, 12)));

            JLabel number = new JLabel(String.valueOf(index + 1), SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D graphics = (Graphics2D) g.create();
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics.setColor(UiColors.ORANGE);
                    graphics.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                    graphics.dispose();
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