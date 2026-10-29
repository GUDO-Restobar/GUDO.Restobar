package ui.manager;

import java.awt.*;
import java.io.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import util.FontLoader;

public class mainView extends JFrame {

    private final static Color MAINWHITE = new Color(246, 246, 246);
    private final static Color MAINORANGE = Color.decode("#EC5E02");
    private final static ImageIcon LOGO = new ImageIcon("public\\assets\\logo\\gudo-white-logo.png");
    private final static Font BOLD = loadFont("Montserrat/static/Montserrat-Bold.ttf");

    public mainView() {
        setTitle("Gudo Restobar Manager");
        setSize(1920, 1080);
        setLayout(null);
        getContentPane().setBackground(MAINWHITE);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel sidebarPanel = sideBar();
        sidebarPanel.setBounds(0, 0, 475, 1080);

        JPanel viewRecordsPanel = viewRecords();
        viewRecordsPanel.setBounds(475, 0, 1445, 1080);

        add(sidebarPanel);
        add(viewRecordsPanel);
    }

    public static JPanel viewRecords() {
        int cornerRadius = 42;
        JPanel view = new JPanel();
        view.setLayout(null);    
        view.setBackground(MAINWHITE);
        view.setOpaque(true);
        view.setSize(1445, 1080);
            
        // Use BorderLayout so JScrollPane expands properly inside orderHistory
        JPanel orderHistory = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
                g2.setColor(MAINORANGE);
                g2.setStroke(new BasicStroke(5));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, cornerRadius, cornerRadius);
                
                g2.dispose();
            }
        };
        orderHistory.setOpaque(false);
        orderHistory.setBackground(MAINWHITE);
        orderHistory.setBounds(48, 177, 1349, 769); 

        // Define columns matching database output
        String[] columns = {"Order ID", "Order Date", "Order Time", "Order Type", "Total Amount", "Payment Status", "Order Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(tableModel);
        
        // Table styling
        table.setRowHeight(35);
        table.setFont(BOLD.deriveFont(14f));
        table.getTableHeader().setFont(BOLD.deriveFont(16f));
        table.getTableHeader().setBackground(MAINORANGE);
        table.getTableHeader().setForeground(MAINWHITE);

        // Load DB records into model
        loadOrderData(tableModel);

        // Wrap JTable in JScrollPane
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Margin inside panel

        orderHistory.add(scrollPane, BorderLayout.CENTER);

        JLabel label = new JLabel("Order History");
        label.setFont(BOLD.deriveFont(33f));
        label.setForeground(MAINORANGE);
        label.setBounds(48, 105, 427, 45);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setHorizontalAlignment(SwingConstants.LEFT);
            
        view.add(orderHistory);
        view.add(label);
        return view;
    }

    public static JPanel sideBar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(null);    
        sidebar.setBackground(MAINORANGE);
        sidebar.setSize(475, 1080);

        JLabel label = new JLabel("GUDO Restobar");
        label.setIcon(LOGO);
        label.setIconTextGap(-10);
        label.setFont(BOLD.deriveFont(30f));
        label.setForeground(MAINWHITE);
        label.setBounds(32, 40, 379, 106);

        JLabel label2 = new JLabel("Welcome, LORD GUDO");
        label2.setFont(BOLD.deriveFont(30f));
        label2.setForeground(MAINWHITE);
        label2.setBounds(23, 157, 427, 45);
        label2.setVerticalAlignment(SwingConstants.CENTER);
        label2.setHorizontalAlignment(SwingConstants.CENTER);

        sidebar.add(label);
        sidebar.add(label2);

        return sidebar;
    }

    private static Font loadFont(String file) {
        try {
            return FontLoader.loadFont("public/assets/fonts/" + file);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load font: " + file, e);
        }
    }

    private static void loadOrderData(DefaultTableModel model) {
        String url = "jdbc:mysql://localhost:3306/gudorestobardb"; 
        String user = "root";
        String password = "root1234";

        String query = 
            "SELECT orderID AS 'Customer ID', orderDate AS 'Order Date', orderTime AS 'Order Time', orderType AS 'Order Type', totalAmount AS 'Total Amount', paymentStatus AS 'Payment Status', orderStatus AS 'Order Status' FROM CUSTOMER_ORDER";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            model.setRowCount(0);

            while (rs.next()) {
                // Fetch columns using exact aliases or column index positions
                int id = rs.getInt("Customer ID");
                String date = rs.getString("Order Date");
                String time = rs.getString("Order Time");
                String type = rs.getString("Order Type");
                double total = rs.getDouble("Total Amount");
                String paymentStatus = rs.getString("Payment Status");
                String orderStatus = rs.getString("Order Status");

                model.addRow(new Object[]{id, date, time, type, "₱" + total, paymentStatus, orderStatus});
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
    }
}