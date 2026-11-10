import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import ui.manager.EditStockItems;
import static ui.manager.ManagerTheme.*;
import ui.manager.OrderHistory;
import ui.manager.SalesSummary;
import util.UiColors;

/**
 * Manager window. The LEFT side is the sidebar (logo + tabs) and lives here.
 * The RIGHT side is a CardLayout: each tab shows its own panel class
 * ({@link OrderHistory}, {@link EditStockItems}). Clicking a tab only switches the card.
 */
public class ManagerMain extends JFrame {

    private static final String TAB_HISTORY = "Order History";
    private static final String TAB_STOCK = "Edit Stock Items";
    private static final String TAB_SALES = "Sales Summary";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final Map<String, JButton> tabButtons = new LinkedHashMap<>();

    public ManagerMain() {
        setTitle("Gudo Restobar Manager");
        setSize(1340, 1040);
        setLayout(null);
        getContentPane().setBackground(UiColors.BACKGROUND);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        content.add(new OrderHistory(), TAB_HISTORY);
        content.add(new EditStockItems(), TAB_STOCK);
        content.add(new SalesSummary(), TAB_SALES);
        content.setBounds(331, 0, 1008, 1040);

        JPanel sidebar = buildSidebar();
        sidebar.setBounds(0, 0, 332, 1040);

        add(sidebar);
        add(content);

        showTab(TAB_HISTORY);
    }

    /** Shows the panel that belongs to the tab and highlights that tab in the sidebar. */
    private void showTab(String tab) {
        cardLayout.show(content, tab);
        for (Map.Entry<String, JButton> entry : tabButtons.entrySet()) {
            boolean active = entry.getKey().equals(tab);
            entry.getValue().setBackground(active ? UiColors.BACKGROUND : UiColors.ORANGE);
            entry.getValue().setForeground(active ? UiColors.ORANGE : UiColors.BACKGROUND);
        }
    }

    // ------------------------------------------------------------------ sidebar

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(null);
        sidebar.setBackground(UiColors.ORANGE);

        JLabel logo = new JLabel(new ImageIcon(LOGO.getImage().getScaledInstance(209, 219, Image.SCALE_SMOOTH)));
        logo.setBounds(61, 25, 209, 219);

        JLabel welcome = new JLabel("Welcome, LORD GUDO", SwingConstants.CENTER);
        welcome.setFont(BOLD.deriveFont(24f));
        welcome.setForeground(UiColors.BACKGROUND);
        welcome.setBounds(17, 262, 298, 31);

        sidebar.add(logo);
        sidebar.add(welcome);

        String[] tabs = {TAB_HISTORY, TAB_STOCK, TAB_SALES};
        for (int i = 0; i < tabs.length; i++) {
            String name = tabs[i];
            JButton button = tabButton(name);
            button.setBounds(0, 404 + i * 62, 332, 62);
            button.addActionListener(e -> showTab(name));
            tabButtons.put(name, button);
            sidebar.add(button);
        }
        return sidebar;
    }

    private static JButton tabButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BOLD.deriveFont(18f));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ManagerMain().setVisible(true));
    }
}
