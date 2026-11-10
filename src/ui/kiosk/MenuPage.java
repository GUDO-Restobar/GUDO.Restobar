package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import data.MenuItemDAO;
import util.*;

/**
 * The menu screen: category sidebar, the scrollable list of products and the
 * bottom bar with the running total.
 *
 * The page never talks to the other screens directly - it reports what the
 * customer tapped through its callbacks and KioskMain decides where to go.
 */
public class MenuPage extends JPanel {

    // ============================================================
    // FRONTEND - layout, sidebar, product grid and the bottom bar
    // ============================================================

    private static final String IMG = "public/assets/menu/";
    private static final String BANNER = "public/assets/banners/big-burger-banner.png";
    private static final String BAG = "public/assets/icons/order-bag.png";
    private static final String THUMB = "public/assets/scrollbar/scroll-thumb.png";
    private static final String TRACK = "public/assets/scrollbar/scroll-track.png";

    /** Sections shown in the sidebar and in the scrolling list, in order. */
    private static final String[] CATEGORY_NAMES  = {"Meals", "Snacks", "Drinks"};
    private static final String[] CATEGORY_IMAGES = {
        IMG + "quiburger.png", IMG + "4-pillar-fries.png", IMG + "jandro-cola.png"
    };

    private final Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");

    /** An item was tapped - hand the product to the Add Order page. */
    private final Consumer<Product> onProductSelected;
    /** Proceed to Checkout was pressed. */
    private final Runnable onCheckout;
    /** Start Over was pressed. */
    private final Runnable onStartOver;

    private final JPanel[] sections = new JPanel[CATEGORY_NAMES.length];
    private final List<ItemCard> itemCards = new ArrayList<>();

    /** The menu from the MENU_ITEM table, grouped by category. */
    private final Map<String, List<Product>> menu = loadMenu();

    private JScrollPane scrollPane;
    private JLabel totalLabel;

    public MenuPage(Consumer<Product> onProductSelected, Runnable onCheckout, Runnable onStartOver) {
        this.onProductSelected = onProductSelected != null ? onProductSelected : product -> {};
        this.onCheckout = onCheckout != null ? onCheckout : () -> {};
        this.onStartOver = onStartOver != null ? onStartOver : () -> {};

        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.WEST);

        // Start the list at the very top every time the page is built.
        SwingUtilities.invokeLater(() ->
            scrollPane.getViewport().setViewPosition(new Point(0, 0))
        );
    }

    /** Banner on top, the product list in the middle, the total bar at the bottom. */
    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.add(buildBanner(), BorderLayout.NORTH);
        content.add(buildMenuList(), BorderLayout.CENTER);
        content.add(buildBottomBar(), BorderLayout.SOUTH);
        return content;
    }

    /** The orange rail with the logo and the three category tabs. */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UiColors.ORANGE);
        sidebar.setPreferredSize(new Dimension(130, 1040));

        JPanel logo = new JPanel();
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(130, 150));
        logo.add(new JLabel(UiKit.icon("public\\assets\\logo\\logo-circle.png", 128, 128)));
        sidebar.add(logo, BorderLayout.NORTH);

        JPanel tabs = new JPanel();
        tabs.setOpaque(false);
        tabs.setLayout(new BoxLayout(tabs, BoxLayout.Y_AXIS));

        for (int i = 0; i < sections.length; i++) {
            final JPanel section = sections[i];
            CategoryTab tab = new CategoryTab(
                CATEGORY_NAMES[i], CATEGORY_IMAGES[i], bold, () -> scrollTo(section));
            tab.setAlignmentX(Component.CENTER_ALIGNMENT);
            tabs.add(Box.createVerticalStrut(18));
            tabs.add(tab);
        }
        sidebar.add(tabs, BorderLayout.CENTER);
        return sidebar;
    }

    /** The wide burger picture at the top of the list. */
    private JPanel buildBanner() {
        Image image = new ImageIcon(BANNER).getImage();
        JPanel banner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(image, 0, 0, getWidth(), getHeight(), this);
                g2.dispose();
            }
        };
        banner.setPreferredSize(new Dimension(0, 150));
        return banner;
    }

    /** The scrollable list holding one section (and grid) per category. */
    private JScrollPane buildMenuList() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(UiColors.SURFACE);

        for (int i = 0; i < sections.length; i++) {
            sections[i] = buildSection(
                CATEGORY_NAMES[i], menu.getOrDefault(CATEGORY_NAMES[i], new ArrayList<>()));
            list.add(sections[i]);
        }

        scrollPane = new JScrollPane(list);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(UiColors.SURFACE);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JScrollBar bar = scrollPane.getVerticalScrollBar();
        bar.setUnitIncrement(24);
        bar.setUI(new KioskScrollBarUI(THUMB, TRACK));
        bar.setPreferredSize(new Dimension(26, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 8));
        bar.setBackground(UiColors.SURFACE);
        return scrollPane;
    }

    /** One category: a heading plus a two-column grid of product cards. */
    private JPanel buildSection(String title, List<Product> products) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(UiColors.SURFACE);
        section.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel(title);
        heading.setFont(bold.deriveFont(24f));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        section.add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 20));
        grid.setOpaque(false);
        for (Product product : products) {
            ItemCard card = new ItemCard(product, bold, this::openProduct);
            itemCards.add(card);
            grid.add(card);
        }
        section.add(grid, BorderLayout.CENTER);
        return section;
    }

    /** Running total on the left, Start Over under it, checkout button on the right. */
    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiColors.SURFACE);
        bar.setPreferredSize(new Dimension(0, 135));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UiColors.BORDER_MID),
            BorderFactory.createEmptyBorder(0, 20, 0, 16)));
        bar.add(buildTotalArea(), BorderLayout.WEST);
        bar.add(buildCheckoutButton(), BorderLayout.EAST);
        return bar;
    }

    /** The peso total with the order-bag icon, and the Start Over button below it. */
    private JPanel buildTotalArea() {
        JPanel area = new JPanel();
        area.setLayout(new BoxLayout(area, BoxLayout.Y_AXIS));
        area.setOpaque(false);

        totalLabel = new JLabel("\u20B10.00", UiKit.icon(BAG, 56, 56), SwingConstants.LEFT);
        totalLabel.setFont(bold.deriveFont(26f));
        totalLabel.setForeground(UiColors.ORANGE_DARK);
        totalLabel.setIconTextGap(14);
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        KioskButton reset = new KioskButton("Start Over", null,
            UiColors.BACKGROUND, UiColors.HOVER_GREY_SOFT, 4, UiColors.BORDER);
        reset.setFont(bold.deriveFont(12f));
        reset.setForeground(UiColors.BROWN);
        reset.setPreferredSize(new Dimension(178, 34));
        reset.setMaximumSize(new Dimension(178, 34));
        reset.setAlignmentX(Component.LEFT_ALIGNMENT);
        reset.addActionListener(e -> startOver());

        area.add(Box.createVerticalGlue());
        area.add(totalLabel);
        area.add(Box.createVerticalStrut(10));
        area.add(reset);
        area.add(Box.createVerticalGlue());
        return area;
    }

    /** The big orange "Proceed to Checkout" button. */
    private JPanel buildCheckoutButton() {
        KioskButton button = new KioskButton(
            "<html><center>Proceed to<br>Checkout</center></html>",
            new CheckCircleIcon(40, UiColors.BACKGROUND),
            UiColors.ORANGE, UiColors.ORANGE_HOVER, 8, null);
        button.setFont(bold.deriveFont(17f));
        button.setForeground(UiColors.BACKGROUND);
        button.setPreferredSize(new Dimension(207, 94));
        button.addActionListener(e -> goToCheckout());

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(button);
        return wrapper;
    }

    // ------------------------------------------------------------
    // Reusable components used by this page
    // ------------------------------------------------------------

    /** Orange button with a soft drop shadow and a hover colour. */
    private static class KioskButton extends JButton {
        private final int shadow;
        private final Color outline;

        KioskButton(String text, Icon icon, Color base, Color hover, int shadow, Color outline) {
            super(text, icon);
            this.shadow = shadow;
            this.outline = outline;
            setBackground(base);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setIconTextGap(12);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(hover); }
                @Override public void mouseExited(MouseEvent e) { setBackground(base); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int h = getHeight() - shadow;
            for (int i = shadow; i > 0; i--) {
                g2.setColor(UiColors.shadow(4 + (shadow - i) * 3));
                g2.fillRoundRect(0, i, getWidth(), h, 10, 10);
            }
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), h, 10, 10);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, h - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A tick inside a circle, used as the checkout button's icon. */
    private static class CheckCircleIcon implements Icon {
        private final int size;
        private final Color color;

        CheckCircleIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 2, y + 2, size - 4, size - 4);
            g2.drawPolyline(
                new int[]{x + size * 28 / 100, x + size * 44 / 100, x + size * 72 / 100},
                new int[]{y + size * 52 / 100, y + size * 66 / 100, y + size * 36 / 100}, 3);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // ============================================================
    // BACKEND - running total, stock availability and page actions
    // ============================================================

    /** Keeps the bottom-bar total in step with the cart. Called by KioskMain. */
    public void setTotal(double total) {
        totalLabel.setText("\u20B1" + String.format("%.2f", total));
    }

    /**
     * Marks every product whose stock is used up (or is fully reserved by the
     * current cart) as UNAVAILABLE, and makes it unclickable.
     */
    public void refreshAvailability(Cart cart) {
        for (ItemCard card : itemCards) {
            card.setAvailable(Inventory.getAvailable(card.getProduct(), cart) > 0);
        }
    }

    /** A product card was tapped: let KioskMain build the Add Order page for it. */
    private void openProduct(Product product) {
        onProductSelected.accept(product);
    }

    /** Start Over on the bottom bar: KioskMain shows the confirmation page. */
    private void startOver() {
        onStartOver.run();
    }

    /** Proceed to Checkout: KioskMain shows the checkout page. */
    private void goToCheckout() {
        onCheckout.run();
    }

    /** Jumps the scroll pane to the top of the given category section. */
    private void scrollTo(JPanel section) {
        if (section != null && scrollPane != null) {
            scrollPane.getViewport().setViewPosition(new Point(0, section.getY()));
        }
    }

    /** Reads the whole menu from the MENU_ITEM table. */
    private static Map<String, List<Product>> loadMenu() {
        try {
            return MenuItemDAO.loadMenu();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the menu from the database.", e);
        }
    }
}
