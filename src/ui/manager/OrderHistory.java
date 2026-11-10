package ui.manager;

import static ui.manager.ManagerTheme.*;

import data.DbWatcher;
import data.OrderDAO;
import data.dbconnect;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import util.UiColors;

/** The "Order History" screen (right-hand card of {@code ManagerMain}). */
public class OrderHistory extends JPanel {

    private static final Color ROW_LINE = new Color(232, 232, 232);
    private static final Color TEXT_GRAY = new Color(90, 90, 90);

    private static final int ROWS_PER_PAGE = 15;
    private static final int COL_STATUS = 6;   // "Order Status" column (dropdown)

    /**
     * The receipt card is drawn on top of the table. Swing normally repaints a single child
     * without redrawing siblings that overlap it, which let the table paint over the receipt
     * whenever the mouse moved. Turning "optimized drawing" off makes it redraw them in order.
     */
    @Override
    public boolean isOptimizedDrawingEnabled() { return false; }

    /** Order History panel: heading, the orders table, pagination, delete button and receipt. */
    public OrderHistory() {
        JPanel view = this;
        setLayout(null);
        setBackground(UiColors.BACKGROUND);
        setOpaque(true);

        // Plain container for the table (no outline), as large as the page allows.
        JPanel orderHistory = new JPanel(new BorderLayout());
        orderHistory.setOpaque(false);
        orderHistory.setBounds(33, 110, 942, 800);

        String[] columns = {"Order ID", "Order Date", "Order Time", "Order Type", "Total Amount", "Payment Status", "Order Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == COL_STATUS; }
        };
        JTable table = new JTable(tableModel);
        styleTable(table);

        List<Object[]> allRows = new ArrayList<>(loadOrderData());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(UiColors.SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiColors.BORDER_ROW));
        orderHistory.add(scrollPane, BorderLayout.CENTER);

        JLabel label = new JLabel("ORDER HISTORY");
        label.setFont(BOLD.deriveFont(33f));
        label.setForeground(UiColors.ORANGE);
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

            // Remember which orders are selected, so a live refresh doesn't clear the selection.
            java.util.Set<Object> selectedIDs = new java.util.HashSet<>();
            for (int viewRow : table.getSelectedRows()) selectedIDs.add(table.getValueAt(viewRow, 0));

            tableModel.setRowCount(0);
            int from = (currentPage[0] - 1) * ROWS_PER_PAGE;
            int to = Math.min(allRows.size(), from + ROWS_PER_PAGE);
            for (int i = from; i < to; i++) tableModel.addRow(allRows.get(i));

            for (int r = 0; r < tableModel.getRowCount(); r++) {
                if (selectedIDs.contains(tableModel.getValueAt(r, 0))) table.addRowSelectionInterval(r, r);
            }

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

        // ---- Order Status dropdown: picking a new value saves it to the database right away.
        boolean[] revert = {false};
        tableModel.addTableModelListener(e -> {
            if (revert[0] || e.getType() != javax.swing.event.TableModelEvent.UPDATE
                    || e.getColumn() != COL_STATUS || e.getFirstRow() < 0) return;
            int row = e.getFirstRow();
            int orderID = (Integer) tableModel.getValueAt(row, 0);
            String newStatus = String.valueOf(tableModel.getValueAt(row, COL_STATUS));

            // the status this order had before the change (from our copy of the data)
            Object[] stored = null;
            for (Object[] r : allRows) if (((Integer) r[0]) == orderID) stored = r;
            if (stored == null || newStatus.equals(stored[COL_STATUS])) return;
            Object[] storedRow = stored;
            String oldStatus = String.valueOf(storedRow[COL_STATUS]);

            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception {
                    return OrderDAO.updateOrderStatus(orderID, newStatus);
                }
                @Override protected void done() {
                    try {
                        if (get()) { storedRow[COL_STATUS] = newStatus; return; }   // saved
                        JOptionPane.showMessageDialog(view, "This order no longer exists.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(view, "Could not update the status: " + ex.getMessage(),
                            "Update failed", JOptionPane.ERROR_MESSAGE);
                    }
                    // not saved: put the old value back on screen
                    revert[0] = true;
                    for (int r = 0; r < tableModel.getRowCount(); r++) {
                        if (((Integer) tableModel.getValueAt(r, 0)) == orderID) tableModel.setValueAt(oldStatus, r, COL_STATUS);
                    }
                    revert[0] = false;
                }
            }.execute();
        });

        // Real time: whenever the kiosk saves an order (or an order's status changes),
        // reload the rows and redraw the current page. Only runs while this view is on screen.
        DbWatcher.Subscription[] watch = new DbWatcher.Subscription[1];
        view.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED) == 0) return;
            if (view.isDisplayable() && watch[0] == null) {
                watch[0] = DbWatcher.onOrdersChanged(() -> {
                    List<Object[]> latest = loadOrderData(false);
                    if (latest == null) return;
                    allRows.clear();
                    allRows.addAll(latest);
                    refresh[0].run();
                });
            } else if (!view.isDisplayable() && watch[0] != null) {
                watch[0].cancel();
                watch[0] = null;
            }
        });

        // ---- Digital receipt: click an Order ID to open it over the table, arrow (or Esc) closes it.
        JPanel[] receipt = new JPanel[1];
        int[] receiptOrder = {-1};
        Runnable closeReceipt = () -> {
            if (receipt[0] != null) {
                view.remove(receipt[0]);
                receipt[0] = null;
                receiptOrder[0] = -1;
                view.revalidate();
                view.repaint();
            }
        };
        java.util.function.IntConsumer openReceipt = id -> {
            closeReceipt.run();
            receipt[0] = buildReceipt(id, closeReceipt);
            receipt[0].setBounds(420, 110, 400, 800);
            receiptOrder[0] = id;
            view.add(receipt[0], 0);          // index 0 = drawn on top of everything else
            view.revalidate();
            view.repaint();
        };
        installOrderLinks(table, openReceipt);
        view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "closeReceipt");
        view.getActionMap().put("closeReceipt", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { closeReceipt.run(); }
        });

        // ---- DELETE: select one or more rows, then press the button (or the Delete key).
        JButton deleteButton = new JButton("Delete Selected Order");
        deleteButton.setFont(BOLD.deriveFont(12f));
        deleteButton.setBackground(UiColors.DANGER);
        deleteButton.setForeground(UiColors.WHITE);
        deleteButton.setOpaque(true);
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.setBounds(33, 920, 215, 40);
        deleteButton.setEnabled(false);

        Runnable deleteSelected = () -> {
            int[] viewRows = table.getSelectedRows();
            if (viewRows.length == 0) return;

            List<Integer> ids = new ArrayList<>();
            for (int viewRow : viewRows) ids.add((Integer) table.getValueAt(viewRow, 0));

            String what = ids.size() == 1 ? "order #" + ids.get(0) : ids.size() + " orders " + ids;
            int answer = JOptionPane.showConfirmDialog(view,
                "Permanently delete " + what + "?\nThis also removes its items and payment record and cannot be undone.",
                "Delete Order", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;

            try {
                OrderDAO.deleteOrders(ids);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(view, "Could not delete: " + ex.getMessage(),
                    "Delete failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (ids.contains(receiptOrder[0])) closeReceipt.run();   // its receipt no longer exists

            List<Object[]> latest = loadOrderData(false);
            if (latest != null) {
                allRows.clear();
                allRows.addAll(latest);
            }
            refresh[0].run();
        };
        deleteButton.addActionListener(e -> deleteSelected.run());
        table.getSelectionModel().addListSelectionListener(e ->
            deleteButton.setEnabled(table.getSelectedRowCount() > 0));
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"), "deleteOrder");
        table.getActionMap().put("deleteOrder", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { deleteSelected.run(); }
        });

        view.add(deleteButton);
        view.add(orderHistory);
        view.add(label);
        view.add(pagination);
    }

    // ------------------------------------------------------------ table style

    /** Light-gray header, white rows with thin dividers, orange bold "link" in the ID column. */
    private static void styleTable(JTable table) {
        table.setRowHeight(50);
        table.setFont(REGULAR.deriveFont(13f));
        table.setBackground(UiColors.SURFACE);
        table.setShowGrid(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(UiColors.SELECTION);
        table.setSelectionForeground(UiColors.BLACK);

        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(0, 44));
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setFont(BOLD.deriveFont(13f));
                l.setForeground(UiColors.BACKGROUND);
                l.setBackground(UiColors.ORANGE);
                l.setOpaque(true);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 8));
                return l;
            }
        });

        int[] hoverRow = {-1};
        javax.swing.table.DefaultTableCellRenderer cells = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, r, c);
                l.setBackground(sel ? t.getSelectionBackground() : UiColors.SURFACE);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UiColors.BORDER_ROW),
                    BorderFactory.createEmptyBorder(0, 16, 0, 8)));
                if (c == 0) {                                   // Order ID = link
                    String text = "#" + v;
                    l.setText(r == hoverRow[0] ? "<html><u>" + text + "</u></html>" : text);
                    l.setFont(BOLD.deriveFont(13f));
                    l.setForeground(UiColors.ORANGE);
                } else if (c == 4) {                            // Total Amount
                    l.setFont(BOLD.deriveFont(13f));
                    l.setForeground(UiColors.BLACK);
                } else {
                    l.setFont(REGULAR.deriveFont(13f));
                    l.setForeground(UiColors.TEXT_MUTED);
                }
                return l;
            }
        };
        table.setDefaultRenderer(Object.class, cells);
        table.setDefaultRenderer(Integer.class, cells);

        // Order Status: coloured text with a little dropdown arrow; click it to pick another status.
        javax.swing.table.TableCellRenderer statusRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel base = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, r, c);
                return statusLabel(String.valueOf(v), sel ? t.getSelectionBackground() : UiColors.SURFACE);
            }
        };
        JComboBox<String> statusBox = new JComboBox<>(OrderDAO.ORDER_STATUSES);
        statusBox.setFont(BOLD.deriveFont(13f));
        statusBox.setBackground(UiColors.SURFACE);
        statusBox.setForeground(UiColors.ORANGE);
        statusBox.setFocusable(false);
        statusBox.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 4));
        statusBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
                JLabel x = (JLabel) super.getListCellRendererComponent(l, v, i, sel, false);
                x.setFont(BOLD.deriveFont(13f));
                x.setForeground(statusColor(String.valueOf(v)));
                x.setBackground(sel ? UiColors.SELECTION : UiColors.SURFACE);
                x.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return x;
            }
        });
        javax.swing.table.TableColumn statusColumn = table.getColumnModel().getColumn(COL_STATUS);
        statusColumn.setCellRenderer(statusRenderer);
        statusColumn.setCellEditor(new DefaultCellEditor(statusBox));

        // underline + hand cursor while hovering an Order ID
        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                boolean overLink = row >= 0 && table.columnAtPoint(e.getPoint()) == 0;
                int newHover = overLink ? row : -1;
                table.setCursor(Cursor.getPredefinedCursor(overLink ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                if (newHover != hoverRow[0]) { hoverRow[0] = newHover; table.repaint(); }
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (hoverRow[0] != -1) { hoverRow[0] = -1; table.repaint(); }
            }
        });
    }

    private static Color statusColor(String status) {
        switch (status) {
            case "Completed":
            case "Served":    return UiColors.SUCCESS;
            default:          return UiColors.ORANGE;             // Preparing
        }
    }

    /** Status text in its colour, with a small drop-down triangle on the right. */
    private static JLabel statusLabel(String status, Color background) {
        JLabel l = new JLabel(status) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getForeground());
                int x = getWidth() - 22, y = getHeight() / 2 - 2;
                g2.fillPolygon(new int[]{x, x + 9, x + 4}, new int[]{y, y, y + 5}, 3);
                g2.dispose();
            }
        };
        l.setOpaque(true);
        l.setBackground(background);
        l.setForeground(statusColor(status));
        l.setFont(BOLD.deriveFont(13f));
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UiColors.BORDER_ROW),
            BorderFactory.createEmptyBorder(0, 16, 0, 8)));
        return l;
    }

    /** Clicking a cell in the Order ID column opens that order's receipt. */
    private static void installOrderLinks(JTable table, java.util.function.IntConsumer onOpen) {
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && table.columnAtPoint(e.getPoint()) == 0) {
                    onOpen.accept((Integer) table.getValueAt(row, 0));
                }
            }
        });
    }

    // ---------------------------------------------------------- digital receipt

    /** The receipt card: back arrow, logo, "Order #N", scrollable items, total bill. */
    private static JPanel buildReceipt(int orderID, Runnable onClose) {
        int radius = 34;
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UiColors.BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                g2.setColor(UiColors.ORANGE);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, radius, radius);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 22, 20, 22));

        // top: arrow (left) + centered logo
        JLabel logo = new JLabel(new ImageIcon(LOGO.getImage().getScaledInstance(88, 88, Image.SCALE_SMOOTH)));
        JPanel top = new JPanel(null) {
            @Override public void doLayout() {
                logo.setBounds((getWidth() - 88) / 2, 4, 88, 88);   // centered on the card
            }
        };
        top.setOpaque(false);
        top.setPreferredSize(new Dimension(0, 100));
        JButton back = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UiColors.ORANGE);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cy = getHeight() / 2;
                g2.drawLine(6, cy, getWidth() - 6, cy);                 // shaft
                g2.drawLine(6, cy, 15, cy - 9);                          // head
                g2.drawLine(6, cy, 15, cy + 9);
                g2.dispose();
            }
        };
        back.setBounds(0, 0, 36, 30);
        back.setToolTipText("Close receipt");
        back.setContentAreaFilled(false);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> onClose.run());
        top.add(back);
        top.add(logo);

        // title + meta
        JLabel title = new JLabel("Order #" + orderID);
        title.setFont(BOLD.deriveFont(17f));
        title.setForeground(UiColors.ORANGE);
        JLabel meta = new JLabel("Loading...");
        meta.setFont(REGULAR.deriveFont(12f));
        meta.setForeground(UiColors.TEXT_MUTED);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(meta);

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(heading, BorderLayout.CENTER);

        // items (receipt-like, scrolls when there are many)
        JPanel items = new JPanel();
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBackground(UiColors.SURFACE);
        items.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        JScrollPane scroll = new JScrollPane(items,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UiColors.SURFACE);
        scroll.setBorder(BorderFactory.createLineBorder(UiColors.BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // bottom: total bill
        JLabel totalLabel = new JLabel("Total bill  ₱0.00", SwingConstants.RIGHT);
        totalLabel.setFont(BOLD.deriveFont(18f));
        totalLabel.setForeground(UiColors.BLACK);

        card.add(north, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(totalLabel, BorderLayout.SOUTH);

        // load in the background so the table never freezes
        new SwingWorker<OrderDAO.ReceiptData, Void>() {
            @Override protected OrderDAO.ReceiptData doInBackground() throws Exception {
                return OrderDAO.loadReceipt(orderID);
            }
            @Override protected void done() {
                try {
                    OrderDAO.ReceiptData data = get();
                    if (data == null) { meta.setText("This order no longer exists."); return; }
                    String type = data.orderType + (data.method != null ? "  •  " + data.method : "");
                    meta.setText("<html>" + data.date + "  " + data.time + "<br>" + type
                        + "<br>" + data.paymentStatus + "  •  " + data.orderStatus + "</html>");
                    for (OrderDAO.ReceiptData.Line line : data.lines) items.add(receiptLine(line));
                    if (data.lines.isEmpty()) {
                        JLabel none = new JLabel("No items recorded for this order.");
                        none.setFont(REGULAR.deriveFont(12f));
                        items.add(none);
                    }
                    totalLabel.setText("Total bill  ₱" + String.format("%,.2f", data.total));
                    items.revalidate();
                    items.repaint();
                } catch (Exception ex) {
                    meta.setText("Could not load this order.");
                    ex.printStackTrace();
                }
            }
        }.execute();
        return card;
    }

    /** One receipt row: "2 x Name" with a small gray line below, subtotal on the right. */
    private static JPanel receiptLine(OrderDAO.ReceiptData.Line line) {
        StringBuilder detail = new StringBuilder(String.format("₱%,.2f each", line.price));
        if (line.water) detail.append("  •  Water");
        if (line.utensils) detail.append("  •  Utensils");

        JLabel name = new JLabel("<html><body style='width:210px'><b>" + line.quantity + " ×</b>  "
            + line.name.replace("&", "&amp;").replace("<", "&lt;")
            + "<br><span style='color:#777777;font-size:10px'>" + detail + "</span></body></html>");
        name.setFont(REGULAR.deriveFont(13f));
        JLabel sub = new JLabel(String.format("₱%,.2f", line.subTotal()));
        sub.setFont(BOLD.deriveFont(13f));

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(UiColors.SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UiColors.BORDER_ROW),
            BorderFactory.createEmptyBorder(8, 0, 8, 0)));
        row.add(name, BorderLayout.CENTER);
        row.add(sub, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JButton pageButton(String text, boolean selected, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(BOLD.deriveFont(11f));
        b.setMargin(new Insets(2, 6, 2, 6));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(selected ? UiColors.ORANGE : UiColors.BACKGROUND);
        b.setForeground(selected ? UiColors.BACKGROUND : UiColors.ORANGE);
        b.addActionListener(e -> action.run());
        return b;
    }

    /**
     * Loads every order. CUSTOMER_ORDER.totalAmount is never filled in by the
     * inserts, so the total falls back to the sum of the order's line items.
     */
    private static List<Object[]> loadOrderData() {
        List<Object[]> rows = loadOrderData(true);
        return rows != null ? rows : new ArrayList<>();
    }

    /** @param showErrors false for background refreshes, which just return null on failure. */
    private static List<Object[]> loadOrderData(boolean showErrors) {
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
            if (!showErrors) return null;
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
        return rows;
    }
}
