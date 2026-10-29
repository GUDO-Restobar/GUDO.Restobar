package ui.manager;

import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import data.dbconnect;
import util.FontLoader;

/**
 * Manager window showing the Order History tab.
 * The "Edit Stock Items" tab in the sidebar opens {@link EditStockItems}, which
 * switches between both tabs with a CardLayout.
 */
public class mainView extends JFrame {

    static final Color MAINWHITE = new Color(246, 246, 246);
    static final Color MAINORANGE = Color.decode("#EC5E02");
    static final ImageIcon LOGO = new ImageIcon("public\\assets\\logo\\gudo-white-logo.png");
    static final Font BOLD = loadFont("Montserrat/static/Montserrat-Bold.ttf");

    static final String TAB_HISTORY = "Order History";
    static final String TAB_STOCK = "Edit Stock Items";

    private static final int ROWS_PER_PAGE = 17;

    public mainView() {
        setTitle("Gudo Restobar Manager");
        setSize(1340, 1040);
        setLayout(null);
        getContentPane().setBackground(MAINWHITE);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Clicking "Edit Stock Items" opens the card-layout window on that tab.
        JPanel sidebarPanel = sideBar(TAB_HISTORY, tab -> {
            if (TAB_STOCK.equals(tab)) {
                EditStockItems view = new EditStockItems();
                view.showTab(TAB_STOCK);
                view.setLocationRelativeTo(this);
                view.setVisible(true);
                dispose();
            }
        });
        sidebarPanel.setBounds(0, 0, 332, 1040);

        JPanel viewRecordsPanel = viewRecords();
        viewRecordsPanel.setBounds(331, 0, 1008, 1040);

        add(sidebarPanel);
        add(viewRecordsPanel);
    }

    /** Order History content: heading, rounded panel containing the table, pagination. */
    public static JPanel viewRecords() {
        int cornerRadius = 42;
        JPanel view = new JPanel();
        view.setLayout(null);
        view.setBackground(MAINWHITE);
        view.setOpaque(true);

        // Rounded orange-outlined panel; the table lives inside it.
        JPanel orderHistory = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
                g2.setColor(MAINORANGE);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, cornerRadius, cornerRadius);
                g2.dispose();
            }
        };
        orderHistory.setOpaque(false);
        orderHistory.setBackground(MAINWHITE);
        orderHistory.setBounds(33, 167, 942, 740);

        String[] columns = {"Order ID", "Order Date", "Order Time", "Order Type", "Total Amount", "Payment Status", "Order Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(35);
        table.setFont(BOLD.deriveFont(14f));
        table.setBackground(MAINWHITE);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(BOLD.deriveFont(16f));
        table.getTableHeader().setBackground(MAINORANGE);
        table.getTableHeader().setForeground(MAINWHITE);
        table.getTableHeader().setReorderingAllowed(false);

        List<Object[]> allRows = loadOrderData();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(MAINWHITE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        orderHistory.add(scrollPane, BorderLayout.CENTER);

        JLabel label = new JLabel("ORDER HISTORY");
        label.setFont(BOLD.deriveFont(33f));
        label.setForeground(MAINORANGE);
        label.setBounds(33, 42, 600, 44);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setHorizontalAlignment(SwingConstants.LEFT);

        // Pagination under the panel: <  1 2 3 4 5  >
        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        pagination.setOpaque(false);
        pagination.setBounds(33, 925, 942, 30);
        int[] currentPage = {1};
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            int totalPages = Math.max(1, (int) Math.ceil(allRows.size() / (double) ROWS_PER_PAGE));
            currentPage[0] = Math.min(Math.max(1, currentPage[0]), totalPages);

            tableModel.setRowCount(0);
            int from = (currentPage[0] - 1) * ROWS_PER_PAGE;
            int to = Math.min(allRows.size(), from + ROWS_PER_PAGE);
            for (int i = from; i < to; i++) tableModel.addRow(allRows.get(i));

            pagination.removeAll();
            pagination.add(pageButton("<", false, () -> { currentPage[0]--; refresh[0].run(); }));
            int start = Math.max(1, Math.min(currentPage[0] - 2, totalPages - 4));
            int end = Math.min(totalPages, start + 4);
            for (int p = start; p <= end; p++) {
                final int page = p;
                pagination.add(pageButton(String.valueOf(p), p == currentPage[0],
                    () -> { currentPage[0] = page; refresh[0].run(); }));
            }
            pagination.add(pageButton(">", false, () -> { currentPage[0]++; refresh[0].run(); }));
            pagination.revalidate();
            pagination.repaint();
        };
        refresh[0].run();

        view.add(orderHistory);
        view.add(label);
        view.add(pagination);
        return view;
    }

    private static JButton pageButton(String text, boolean selected, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(BOLD.deriveFont(11f));
        b.setMargin(new Insets(2, 6, 2, 6));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(selected ? MAINORANGE : MAINWHITE);
        b.setForeground(selected ? MAINWHITE : MAINORANGE);
        b.addActionListener(e -> action.run());
        return b;
    }

    public static JPanel sideBar() {
        return sideBar(TAB_HISTORY, null);
    }

    /** Sidebar with logo, welcome text and tabs. The active tab is drawn white. */
    public static JPanel sideBar(String activeTab, Consumer<String> onTabSelected) {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(null);
        sidebar.setBackground(MAINORANGE);
        sidebar.setSize(332, 1040);

        JLabel label = new JLabel();
        label.setBounds(61, 25, 209, 219);
        Image scaledLogo = LOGO.getImage().getScaledInstance(209, 219, Image.SCALE_SMOOTH);
        label.setIcon(new ImageIcon(scaledLogo));

        JLabel label2 = new JLabel("Welcome, LORD GUDO");
        label2.setFont(BOLD.deriveFont(24f));
        label2.setForeground(MAINWHITE);
        label2.setBounds(17, 262, 298, 31);
        label2.setVerticalAlignment(SwingConstants.CENTER);
        label2.setHorizontalAlignment(SwingConstants.CENTER);

        sidebar.add(label);
        sidebar.add(label2);

        String[] tabs = {TAB_HISTORY, TAB_STOCK};
        for (int i = 0; i < tabs.length; i++) {
            JButton tab = tabButton(tabs[i], tabs[i].equals(activeTab));
            tab.setBounds(0, 404 + i * 62, 332, 62);
            final String name = tabs[i];
            tab.addActionListener(e -> {
                if (onTabSelected != null) onTabSelected.accept(name);
            });
            sidebar.add(tab);
        }

        return sidebar;
    }

    private static JButton tabButton(String text, boolean active) {
        JButton b = new JButton(text);
        b.setFont(BOLD.deriveFont(18f));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(active ? MAINWHITE : MAINORANGE);
        b.setForeground(active ? MAINORANGE : MAINWHITE);
        return b;
    }

    static Font loadFont(String file) {
        try {
            return FontLoader.loadFont("public/assets/fonts/" + file);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load font: " + file, e);
        }
    }

    /**
     * Loads every order. CUSTOMER_ORDER.totalAmount is never filled in by the
     * inserts, so the total falls back to the sum of the order's line items.
     */
    private static List<Object[]> loadOrderData() {
        List<Object[]> rows = new ArrayList<>();

        String query =
            "SELECT o.orderID, o.orderDate, o.orderTime, o.orderType, "
          + "       COALESCE(o.totalAmount, SUM(d.subTotal), 0) AS total, "
          + "       o.paymentStatus, o.orderStatus "
          + "FROM CUSTOMER_ORDER o "
          + "LEFT JOIN ORDER_DETAIL d ON d.orderID = o.orderID "
          + "GROUP BY o.orderID, o.orderDate, o.orderTime, o.orderType, "
          + "         o.totalAmount, o.paymentStatus, o.orderStatus "
          + "ORDER BY o.orderID DESC";

        try (Connection conn = dbconnect.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt("orderID"),
                    rs.getString("orderDate"),
                    rs.getString("orderTime"),
                    rs.getString("orderType"),
                    "\u20B1" + String.format("%,.2f", rs.getDouble("total")),
                    rs.getString("paymentStatus"),
                    rs.getString("orderStatus")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
        return rows;
    }
}