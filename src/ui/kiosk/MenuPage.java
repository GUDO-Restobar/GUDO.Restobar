package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import java.awt.event.HierarchyEvent;
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
    private static final String[] CATEGORY_NAMES  = {"Meals", "Snacks", "Drinks", "Extras"};
    private static final String[] CATEGORY_IMAGES = {
        IMG + "quiburger.png", IMG + "4-pillar-fries.png", IMG + "jandro-cola.png", IMG + "extra-rice.png"
    };

    private final Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");

    /** An item was tapped - hand the product to the Add Order page. */
    private final Consumer<Product> onProductSelected;
    /** Proceed to Checkout was pressed. */
    private final Runnable onCheckout;
    /** Start Over was pressed. */
    private final Runnable onStartOver;

    private final JPanel[] sections = new JPanel[CATEGORY_NAMES.length];
    private final CategoryTab[] tabs = new CategoryTab[CATEGORY_NAMES.length];
    private final List<ItemCard> itemCards = new ArrayList<>();

    /** The menu from the MENU_ITEM table, grouped by category. */
    private final Map<String, List<Product>> menu = loadMenu();

    private JScrollPane scrollPane;
    private FadeLabel totalLabel;
    /** The scroll bar's UI, faded in at the end of the entrance animation. */
    private KioskScrollBarUI scrollBarUI;

    /** Runs the fade-in that plays every time the page is shown. */
    private Timer introTimer;
    /** True between being shown and the first frame that is really painted. */
    private boolean introArmed;
    /** How much of the entrance timeline has actually been shown, in ms. */
    private int introElapsed;

    /** Drives the smooth mouse-wheel scrolling. */
    private Timer scrollTimer;
    /** Where the list is currently scrolling to. */
    private int scrollTarget;

    /** Font size of the running total; it shrinks for very large amounts. */
    private static final float TOTAL_FONT_SIZE = 26f;
    /** The width the total may use, leaving a gap before the checkout button. */
    private static final int TOTAL_MAX_WIDTH = 185;

    /** Entrance fade: each element fades for FADE_MILLIS, cards start every CARD_STAGGER. */
    private static final int FADE_MILLIS = 200;
    /** The furthest one frame may advance the fade, so a stalled UI cannot skip it. */
    private static final int MAX_FRAME_STEP = 40;
    private static final int CARDS_START = 100;
    private static final int CARD_STAGGER = 30;

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

        // Replays the entrance fade every time the page really becomes visible
        // (CardLayout switches, window shown) and refreshes the focused category.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                updateActiveCategory();
                playIntro();
            } else {
                introArmed = false;         // the next visit starts from the beginning
                if (introTimer != null) {
                    introTimer.stop();
                }
                resetIntro();
            }
        });

        resetIntro();               // nothing animated is visible before the first show
    }

    // ------------------------------------------------------------
    // Menu data shared with the Add / Edit Order pages
    // ------------------------------------------------------------

    /** The Extras-category items the order pages offer as paid add-ons. */
    public List<Product> getExtras() {
        return menu.getOrDefault("Extras", List.of());
    }

    /** The three Gudo's Favorites shown under every meal. */
    public List<Product> getFavorites() {
        String[] names = {"Quiburger", "LinkedHash Brown", "Java Milkshake"};
        List<Product> picked = new ArrayList<>();
        for (String name : names) {
            for (List<Product> products : menu.values()) {
                for (Product product : products) {
                    if (product.getName().equals(name)) {
                        picked.add(product);
                    }
                }
            }
        }
        return picked;
    }

    /** Banner on top, the product list in the middle, the total bar at the bottom. */
    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.add(buildBanner(), BorderLayout.NORTH);
        content.add(buildMenuList(), BorderLayout.CENTER);
        content.add(buildBottomBar(), BorderLayout.SOUTH);
        return content;
    }

    /** The orange rail with the logo and the category tabs. */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UiColors.ORANGE);
        sidebar.setPreferredSize(new Dimension(130, 1040));

        JPanel logo = new JPanel();
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(130, 150));
        logo.setBorder(BorderFactory.createEmptyBorder(14, 15, 10, 15));   // margin from the orange rail
        logo.add(new JLabel(UiKit.icon("public\\assets\\logo\\logo-circle.png", 100, 100)));
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
            this.tabs[i] = tab;
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
        scrollBarUI = new KioskScrollBarUI(THUMB, TRACK);
        bar.setUI(scrollBarUI);
        bar.setPreferredSize(new Dimension(26, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 8));
        bar.setBackground(UiColors.SURFACE);

        // Keep the sidebar in step with whatever section is being looked at.
        scrollPane.getViewport().addChangeListener(e -> updateActiveCategory());
        enableSmoothScrolling();
        return scrollPane;
    }

    /** Highlights the category tab of the section currently at the top of the list. */
    private void updateActiveCategory() {
        int viewY = scrollPane.getViewport().getViewPosition().y;
        int active = 0;
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] != null && sections[i].getY() <= viewY + 10) {
                active = i;
            }
        }
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] != null) {
                tabs[i].setActive(i == active);
            }
        }
    }

    /**
     * Replaces the default jumpy wheel scrolling with a short glide:
     * the list eases toward the target position at ~30 frames per second.
     */
    private void enableSmoothScrolling() {
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        scrollPane.setWheelScrollingEnabled(false);

        scrollPane.addMouseWheelListener(e -> {
            int max = bar.getMaximum() - bar.getVisibleAmount();
            int step = e.getWheelRotation() * bar.getUnitIncrement() * 3;
            scrollTarget = Math.max(0, Math.min(max, scrollTarget + step));

            if (scrollTimer == null) {
                scrollTimer = new Timer(16, t -> {          // ~60 frames per second
                    int value = bar.getValue();
                    int next = value + (int) Math.round((scrollTarget - value) * 0.16);
                    if (Math.abs(scrollTarget - next) <= 1) {
                        next = scrollTarget;
                        scrollTimer.stop();
                    }
                    bar.setValue(next);
                });
            }
            if (!scrollTimer.isRunning()) {
                scrollTimer.start();
            }
        });

        // Dragging the scrollbar thumb takes over from the glide.
        bar.addAdjustmentListener(e -> {
            if (e.getValueIsAdjusting()) {
                if (scrollTimer != null) {
                    scrollTimer.stop();
                }
                scrollTarget = bar.getValue();
            } else if (scrollTimer == null || !scrollTimer.isRunning()) {
                scrollTarget = bar.getValue();
            }
        });
    }

    // ------------------------------------------------------------
    // Entrance animation
    // ------------------------------------------------------------

    /**
     * Fades the page in every time it is shown, in order: category tabs,
     * product cards from the top down, the scroll bar, then the running
     * total (its bag icon and amount together).
     *
     * Everything else - banner, headings, logo, Start Over, Proceed - is
     * visible immediately.
     *
     * The clock starts on the first painted frame (see {@link #paint}), not
     * here: opening the page also reloads stock from the database, and a slow
     * query would otherwise let the whole animation elapse before the page is
     * ever drawn - making it appear in one jump.
     */
    private void playIntro() {
        if (introTimer != null) {
            introTimer.stop();
        }
        resetIntro();                  // nothing animated is visible yet
        introArmed = true;
        repaint();
    }

    /** Starts the timeline once the page is really on screen. */
    private void startIntro() {
        int rows = (itemCards.size() + 1) / 2;
        int cardsEnd = CARDS_START + Math.max(0, rows - 1) * CARD_STAGGER + FADE_MILLIS;
        int scrollStart = cardsEnd + 20;
        int totalStart = scrollStart + FADE_MILLIS + 20;
        int end = totalStart + FADE_MILLIS;

        introElapsed = 0;
        // System.nanoTime, because currentTimeMillis only ticks every ~16 ms
        // on Windows - too coarse for a smooth timeline.
        final long[] lastTick = {System.nanoTime()};

        // 12 ms, not 16: Windows rounds a 16 ms Swing timer up to two ~15.6 ms
        // system ticks, which halves the frame rate.
        introTimer = new Timer(12, e -> {
            long now = System.nanoTime();
            int delta = (int) ((now - lastTick[0]) / 1_000_000);
            lastTick[0] = now;
            // A stalled event queue (stock reload, garbage collection) may take
            // far longer than a frame; never let that jump skip the fade.
            introElapsed += Math.min(delta, MAX_FRAME_STEP);
            applyIntroFrame(introElapsed, scrollStart, totalStart);
            if (introElapsed >= end) {
                ((Timer) e.getSource()).stop();
            }
        });
        introTimer.start();
    }

    @Override
    public void paint(Graphics g) {
        if (introArmed) {
            introArmed = false;
            startIntro();
        }
        super.paint(g);
    }

    /** Hides every animated element, so the next visit starts at zero. */
    private void resetIntro() {
        for (CategoryTab tab : tabs) {
            if (tab != null) {
                tab.setFade(0f);
            }
        }
        for (ItemCard card : itemCards) {
            card.setFade(0f);
        }
        if (scrollBarUI != null) {
            scrollBarUI.setFade(0f);
        }
        totalLabel.setFade(0f);
    }

    /** Puts one frame of the entrance animation on screen. */
    private void applyIntroFrame(int elapsed, int scrollStart, int totalStart) {
        float tabsFade = fade(elapsed, 0, FADE_MILLIS);
        for (CategoryTab tab : tabs) {
            if (tab != null) {
                tab.setFade(tabsFade);
            }
        }

        for (int i = 0; i < itemCards.size(); i++) {
            int rowStart = CARDS_START + (i / 2) * CARD_STAGGER;   // two cards per row
            itemCards.get(i).setFade(fade(elapsed, rowStart, FADE_MILLIS));
        }

        if (scrollBarUI != null) {
            scrollBarUI.setFade(fade(elapsed, scrollStart, FADE_MILLIS));
        }

        // One label holds both the bag icon and the amount, so the pair fades as a unit.
        totalLabel.setFade(fade(elapsed, totalStart, FADE_MILLIS));
    }

    /** Eased 0..1 progress of a fade that begins at {@code start}. */
    private static float fade(int elapsed, int start, int duration) {
        float p = (elapsed - start) / (float) duration;
        if (p <= 0f) {
            return 0f;
        }
        if (p >= 1f) {
            return 1f;
        }
        return p * p * (3f - 2f * p);       // smoothstep: gentle start and end
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
        // A fixed slot: however long the amount gets, the total can never grow
        // over the checkout button on the right.
        area.setPreferredSize(new Dimension(TOTAL_MAX_WIDTH, 134));

        totalLabel = new FadeLabel("\u20B10.00", UiKit.icon(BAG, 56, 56), SwingConstants.LEFT);
        totalLabel.setFont(bold.deriveFont(TOTAL_FONT_SIZE));
        totalLabel.setForeground(UiColors.ORANGE_DARK);
        totalLabel.setIconTextGap(14);
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        KioskButton reset = new KioskButton("Start Over", null,
            UiColors.BACKGROUND, UiColors.HOVER_GREY_SOFT, 4,
            UiColors.overlay(UiColors.BORDER, 84));    // 0.33 border weight
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

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        // Bottom edges of the two buttons line up (13 px clears the drop shadow).
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 13, 0));
        wrapper.add(button, BorderLayout.SOUTH);
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
                g2.setColor(UiColors.shadow(6 + (shadow - i) * 4));
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
        fitTotalFont();
    }

    /**
     * Shrinks the total's font when the amount is very large, so the running
     * total always stays inside its area and never runs under the checkout
     * button.
     */
    private void fitTotalFont() {
        int available = TOTAL_MAX_WIDTH
            - totalLabel.getIcon().getIconWidth() - totalLabel.getIconTextGap() - 4;
        FontMetrics fm = totalLabel.getFontMetrics(bold.deriveFont(TOTAL_FONT_SIZE));
        int textWidth = fm.stringWidth(totalLabel.getText());
        float size = TOTAL_FONT_SIZE;
        if (textWidth > available && textWidth > 0) {
            size = Math.max(15f, TOTAL_FONT_SIZE * available / textWidth);
        }
        totalLabel.setFont(bold.deriveFont(size));
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
            if (scrollTimer != null) {
                scrollTimer.stop();     // a click wins over a wheel glide in progress
            }
            scrollTarget = section.getY();
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
