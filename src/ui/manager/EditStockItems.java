package ui.manager;

import java.awt.*;
import data.DbWatcher;
import data.dbconnect;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Same window as {@link mainView}, but the content area is a CardLayout that
 * switches between the "Order History" and "Edit Stock Items" tabs.
 */
public class EditStockItems extends JFrame {

    private static final Color MAINWHITE = mainView.MAINWHITE;
    private static final Color MAINORANGE = mainView.MAINORANGE;
    private static final Font BOLD = mainView.BOLD;
    private static final String IMG = "public/assets/menu/";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel sidebarHolder = new JPanel(new BorderLayout());

    private final List<StockItem> stockItems = new ArrayList<>();
    private final List<StockCard> stockCards = new ArrayList<>();
    private DbWatcher.Subscription stockWatch;

    public EditStockItems() {
        setTitle("Gudo Restobar Manager");
        setSize(1340, 1040);
        setLayout(null);
        getContentPane().setBackground(MAINWHITE);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        loadStockItems();

        contentPanel.add(mainView.viewRecords(), mainView.TAB_HISTORY);
        contentPanel.add(editStockItems(), mainView.TAB_STOCK);
        contentPanel.setBounds(331, 0, 1008, 1040);

        sidebarHolder.setBounds(0, 0, 332, 1040);

        add(sidebarHolder);
        add(contentPanel);

        showTab(mainView.TAB_HISTORY);

        // Real time: the kiosk reduces the stock whenever an order is paid, so keep the
        // counts on screen in step with the database for as long as this window is open.
        contentPanel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.DISPLAYABILITY_CHANGED) == 0) return;
            if (contentPanel.isDisplayable() && stockWatch == null) {
                stockWatch = DbWatcher.onStockChanged(this::syncStockFromDatabase);
            } else if (!contentPanel.isDisplayable() && stockWatch != null) {
                stockWatch.cancel();
                stockWatch = null;
            }
        });
    }

    /**
     * Pulls the latest stock from MySQL into the open cards.
     * An item the manager is in the middle of editing (not applied yet) keeps its
     * pending number; only its "In stock" figure is updated.
     */
    private void syncStockFromDatabase() {
        java.util.Map<Integer, Integer> latest = new java.util.HashMap<>();
        try (Connection conn = dbconnect.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT itemID, stockQuantity FROM MENU_ITEM")) {
            while (rs.next()) latest.put(rs.getInt(1), rs.getInt(2));
        } catch (SQLException e) {
            return; // try again on the next change
        }
        for (StockItem item : stockItems) {
            Integer db = latest.get(item.itemID);
            if (db == null) continue;
            boolean hasPendingEdit = item.stock != item.committed;
            item.committed = db;
            if (!hasPendingEdit) item.stock = db;
        }
        for (StockCard card : stockCards) card.refresh();
    }

    /** Switches the card and redraws the sidebar so the matching tab is highlighted. */
    public void showTab(String tab) {
        sidebarHolder.removeAll();
        sidebarHolder.add(mainView.sideBar(tab, this::showTab), BorderLayout.CENTER);
        sidebarHolder.revalidate();
        sidebarHolder.repaint();
        cardLayout.show(contentPanel, tab);
    }

    // ---------------------------------------------------------------- stock

    /** Loads every menu item and its stock from MENU_ITEM. */
    private void loadStockItems() {
        String query = "SELECT itemID, itemName, category, unitPrice, stockQuantity "
                     + "FROM MENU_ITEM ORDER BY itemID";
        try (Connection conn = dbconnect.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String name = rs.getString("itemName");
                stockItems.add(new StockItem(
                    rs.getInt("itemID"), rs.getString("category"), name,
                    rs.getDouble("unitPrice"), imagePathFor(name),
                    rs.getInt("stockQuantity")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
    }

    /** Menu images are named after the item: "'Jandro Cola" -> jandro-cola.png */
    private static String imagePathFor(String name) {
        String slug = name.toLowerCase().replace("'", "").replace(' ', '-');
        return IMG + slug + ".png";
    }

    /** Writes every changed stock count in one transaction. */
    private boolean saveStock() {
        String sql = "UPDATE MENU_ITEM SET stockQuantity = ?, "
                   + "availabilityStatus = ? WHERE itemID = ?";
        try (Connection conn = dbconnect.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (StockItem item : stockItems) {
                    if (item.stock == item.committed) continue;
                    ps.setInt(1, item.stock);
                    ps.setString(2, item.stock > 0 ? "Available" : "Not Available");
                    ps.setInt(3, item.itemID);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not save stock: " + e.getMessage());
            return false;
        }
    }

    private JPanel editStockItems() {
        int cornerRadius = 42;
        JPanel view = new JPanel(null);
        view.setBackground(MAINWHITE);

        JLabel title = new JLabel("EDIT STOCK ITEMS");
        title.setFont(BOLD.deriveFont(33f));
        title.setForeground(MAINORANGE);
        title.setBounds(33, 42, 600, 44);
        view.add(title);

        JPanel panel = new JPanel(new BorderLayout()) {
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
        panel.setOpaque(false);
        panel.setBackground(MAINWHITE);
        panel.setBounds(33, 167, 942, 740);

        // Sections (Meals / Snacks / Drinks), each a 2-column grid of item cards.
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(MAINWHITE);
        list.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));

        List<StockCard> cards = stockCards;
        String current = null;
        JPanel grid = null;
        for (StockItem item : stockItems) {
            if (!item.category.equals(current)) {
                current = item.category;
                JLabel heading = new JLabel(current);
                heading.setFont(BOLD.deriveFont(17f));
                heading.setAlignmentX(Component.LEFT_ALIGNMENT);
                heading.setBorder(BorderFactory.createEmptyBorder(8, 2, 6, 0));
                list.add(heading);

                grid = new JPanel(new GridLayout(0, 2, 14, 12));
                grid.setBackground(MAINWHITE);
                grid.setAlignmentX(Component.LEFT_ALIGNMENT);
                list.add(grid);
            }
            StockCard card = new StockCard(item);
            cards.add(card);
            grid.add(card);
        }
        // Keep a section's grid from stretching vertically.
        for (Component c : list.getComponents()) {
            if (c instanceof JPanel) {
                c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            }
        }

        JScrollPane scroll = new JScrollPane(list,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(MAINWHITE);
        scroll.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, BorderLayout.CENTER);
        view.add(panel);

        // Apply / Discard
        JButton apply = actionButton("Apply Changes", MAINORANGE, MAINWHITE);
        apply.setBounds(533, 925, 215, 40);
        apply.addActionListener(e -> {
            if (!saveStock()) return;
            for (StockItem item : stockItems) item.committed = item.stock;
            for (StockCard card : cards) card.refresh();
            JOptionPane.showMessageDialog(this, "Stock changes applied.");
        });
        JButton discard = actionButton("Discard Changes", MAINWHITE, Color.decode("#C62828"));
        discard.setBounds(760, 925, 215, 40);
        discard.addActionListener(e -> {
            for (StockItem item : stockItems) item.stock = item.committed;
            for (StockCard card : cards) card.refresh();
        });
        view.add(apply);
        view.add(discard);

        return view;
    }

    private static JButton actionButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(BOLD.deriveFont(12f));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 225)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------------------------------------------------------------- model

    private static class StockItem {
        final int itemID;
        final String category, name, imagePath;
        final double price;
        int stock, committed;

        StockItem(int itemID, String category, String name, double price, String imagePath, int stock) {
            this.itemID = itemID;
            this.category = category;
            this.name = name;
            this.price = price;
            this.imagePath = imagePath;
            this.stock = stock;
            this.committed = stock;
        }
    }

    /** One item card: image, name, price, [-] count [+], "In stock: n". */
    private static class StockCard extends JPanel {
        private final StockItem item;
        private final JLabel count = new JLabel();
        private final JLabel inStock = new JLabel();
        private final JLabel name, price;
        private final JButton minus = circleButton("\u2212");
        private final JButton plus = circleButton("+");

        StockCard(StockItem item) {
            this.item = item;
            setLayout(null);
            setBackground(MAINWHITE);
            setBorder(BorderFactory.createLineBorder(new Color(215, 215, 215)));
            setPreferredSize(new Dimension(400, 68));

            JLabel image = new JLabel();
            image.setBounds(8, 12, 44, 44);
            ImageIcon icon = new ImageIcon(item.imagePath);
            if (icon.getIconWidth() > 0) {
                image.setIcon(new ImageIcon(icon.getImage().getScaledInstance(44, 44, Image.SCALE_SMOOTH)));
            }
            add(image);

            name = new JLabel(item.name);
            name.setFont(BOLD.deriveFont(11f));
            add(name);

            price = new JLabel("\u20B1" + String.format("%.2f", item.price));
            price.setFont(BOLD.deriveFont(12f));
            price.setForeground(MAINORANGE);
            add(price);

            minus.addActionListener(e -> change(-1));
            add(minus);

            count.setFont(BOLD.deriveFont(20f));
            count.setHorizontalAlignment(SwingConstants.CENTER);
            add(count);

            plus.addActionListener(e -> change(1));
            add(plus);

            inStock.setFont(BOLD.deriveFont(10f));
            inStock.setHorizontalAlignment(SwingConstants.RIGHT);
            add(inStock);

            refresh();
        }

        private void change(int delta) {
            item.stock = Math.max(0, item.stock + delta);
            refresh();
        }

        void refresh() {
            count.setText(String.valueOf(item.stock));
            inStock.setText("In stock: " + item.committed);
        }

        /**
         * Positions the controls from the card's right edge. Bounds are set
         * absolutely each time (never shifted relative to their old position),
         * so repeated layout passes can't make the buttons drift.
         */
        /** Positions are recomputed from the card width every pass (never shifted incrementally). */
        @Override
        public void doLayout() {
            int w = getWidth();
            plus.setBounds(w - 34, 10, 24, 24);
            count.setBounds(w - 92, 8, 56, 28);
            minus.setBounds(w - 120, 10, 24, 24);
            inStock.setBounds(w - 130, 42, 120, 16);
            name.setBounds(60, 12, Math.max(60, w - 190), 18);
            price.setBounds(60, 32, 120, 18);
        }

        private static JButton circleButton(String text) {
            JButton b = new JButton(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(MAINORANGE);
                    g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            b.setFont(BOLD.deriveFont(15f));
            b.setForeground(MAINWHITE);
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EditStockItems().setVisible(true));
    }
}