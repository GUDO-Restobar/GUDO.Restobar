package ui.kiosk;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import util.AddOn;
import util.Cart;
import util.FadeInSequence;
import util.FadeablePanel;
import util.Inventory;
import util.OrderLine;
import util.Product;
import util.UiColors;
import util.UiKit;

/**
 * Base class for the two pages that show one product with "- / quantity / +"
 * controls: {@link AddOrder} (add a meal to the order) and {@link EditOrder}
 * (change a meal that is already in the order). (Inheritance)
 *
 * The layout, the product card, the extras box, the Gudo's Favorites carousel
 * and the quantity rules live here once; the subclasses only supply what
 * differs between the two pages: the heading, the quantity limits, the save
 * button label and what "save" actually does (Polymorphism).
 *
 * Every element fades in one after the other when the page is shown, and the
 * favourites row glides sideways when its arrow is pressed.
 */
abstract class QuantityPage extends JPanel {

    // ============================================================
    // FRONTEND - header, product card, extras and favorites
    // ============================================================

    /** The pieces of the pattern the subclasses need to see. */
    protected final Product product;
    protected final Cart cart;
    /** The order line being edited; null when a new line is being added. */
    protected final OrderLine line;
    /** Runs after the real cart has been updated. */
    protected final Runnable onSaved;
    /** Runs when the customer backs out without changing anything. */
    private final Runnable onCancel;

    private final Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
    private final Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");

    /** The quantity shown between the minus and plus buttons. */
    protected int quantity = 1;
    /** The read-out between the minus and plus buttons. */
    protected JLabel quantityLabel;
    protected JButton saveButton;
    protected JButton plusButton;

    /** Every extra the customer may pick for this line (paid extras + free sides). */
    private final List<AddOn> extraChoices = new ArrayList<>();
    /** The three Gudo's Favorites offered under the meal. */
    private final List<AddOn> favoriteChoices = new ArrayList<>();

    // ------------------------------------------------------------
    // Layout sizes
    // ------------------------------------------------------------
    private static final int PRODUCT_CARD_HEIGHT = 186;
    private static final int EXTRA_ROW_HEIGHT = 38;
    private static final int CAROUSEL_HEIGHT = 176;
    private static final int FAVORITE_WIDTH = 190;
    private static final int FAVORITE_GAP = 10;
    /** The most of one extra or favorite the customer may pick. */
    private static final int MAX_ADD_ON = 20;

    /** Short description shown under each product name. */
    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
        Map.entry("Alejandro Balls",
            "Juicy meatballs simmered in a rich, savoury sauce with garlic and herbs."),
        Map.entry("Peanut Butterbonia Pancit Canton",
            "Stir-fried noodles tossed in a creamy peanut sauce with crisp vegetables."),
        Map.entry(".Swing Sisig",
            "Sizzling chopped pork with onions, chili and calamansi on a hot plate."),
        Map.entry("Quiburger",
            "A classic burger with a juicy beef patty, melted cheese, fresh vegetables, "
                + "crispy bacon and savoury sauce in a sesame bun."),
        Map.entry("Object Oriented Pasta",
            "Al dente pasta in a slow-cooked tomato sauce with parmesan and herbs."),
        Map.entry("Lechong Gudo",
            "Crispy roasted pork with crunchy skin, served with our signature liver sauce."),
        Map.entry("LinkedHash Brown",
            "Golden hash browns fried until crisp, lightly salted and served hot."),
        Map.entry("4-Pillar Fries",
            "Thick-cut fries with a crisp outside, seasoned with our house spice blend."),
        Map.entry("Chili con Claude",
            "Spiced beef chili with beans, melted cheese and a gentle kick of heat."),
        Map.entry("JNuggets",
            "Bite-sized chicken nuggets in a crunchy golden coating, with a dip."),
        Map.entry("Snhackathon Package",
            "A shareable snack bundle with wings, fries and our dipping sauces."),
        Map.entry("Jandro Cola",
            "Ice-cold cola with a refreshing fizz, the perfect pair for any meal."),
        Map.entry("San Gudo Light",
            "A light, crisp lager served chilled for easy sipping."),
        Map.entry("San Gudo",
            "A full-bodied beer with a smooth finish, served ice-cold."),
        Map.entry("Sam Smoothie",
            "A thick, fruity smoothie blended fresh with real fruit."),
        Map.entry("Java Milkshake",
            "A creamy milkshake blended with rich coffee and vanilla ice cream."));

    private static final String FALLBACK_DESCRIPTION =
        "A delicious Gudo’s Restobar favorite, freshly prepared for your cravings.";

    // ------------------------------------------------------------
    // Entrance animation and smooth carousel
    // ------------------------------------------------------------
    private final FadeInSequence entrance = new FadeInSequence(this, 160, 40);

    private JScrollPane carouselScroll;
    private JPanel carouselTrack;
    private ArrowButton carouselArrow;
    private Timer carouselTimer;
    /** Where the favorites row is gliding to. */
    private int carouselTarget;

    /** The most of this product the customer may have on this page. */
    protected abstract int maxQuantity();

    /** The start of the save button label ("Add to Order", "Save Changes to Order"). */
    protected abstract String saveButtonPrefix();

    /** Writes the chosen quantity and add-ons into the cart however this page wants. */
    protected abstract void commitOrder();

    /** The quantity the page starts at (a new item starts at 1, an edited one at its current amount). */
    protected int startingQuantity() {
        return 1;
    }

    /** The bottom row of buttons (Cancel / Save). */
    protected abstract JPanel buildButtons();

    /** The heading shown next to the logo, one entry per line. */
    protected abstract String[] pageTitleLines();

    protected QuantityPage(Product product, OrderLine line, Cart cart,
                           List<Product> extraMenu, List<Product> favoriteMenu,
                           Runnable onSaved, Runnable onCancel) {
        this.product = product;
        this.line = line;
        this.cart = cart;
        this.onSaved = onSaved != null ? onSaved : () -> {};
        this.onCancel = onCancel != null ? onCancel : () -> {};

        // Extras come from the menu's "Extras" category, followed by the two
        // things every meal can have for free.
        if (extraMenu != null) {
            for (Product extra : extraMenu) {
                extraChoices.add(AddOn.extra(extra.getName(), extra.getPrice()));
            }
        }
        extraChoices.add(AddOn.extra("Extra Water", 0));
        extraChoices.add(AddOn.extra("Cutlery", 0));

        if (favoriteMenu != null) {
            for (Product favorite : favoriteMenu) {
                favoriteChoices.add(AddOn.favorite(favorite));
            }
        }

        restoreAddOns();

        quantity = Math.max(1, Math.min(startingQuantity(), maxQuantity()));

        setLayout(new BorderLayout(0, 12));
        setBackground(UiColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 24, 18, 24));

        add(fadeWrap(buildHeader()), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(fadeWrap(buildButtons()), BorderLayout.SOUTH);

        updateSaveButton();
        updatePlusButton();

        // Replay the entrance every time the kiosk opens this page.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                entrance.start();
                SwingUtilities.invokeLater(this::resetCarousel);
            } else {
                entrance.reset();
                if (carouselTimer != null) {
                    carouselTimer.stop();
                }
            }
        });
        entrance.reset();
    }

    /** Puts the extras and favorites that are already on the line back on the page. */
    private void restoreAddOns() {
        if (line == null) {
            return;
        }
        for (AddOn saved : line.getExtras()) {
            for (AddOn choice : extraChoices) {
                if (choice.getName().equals(saved.getName())) {
                    choice.setQuantity(saved.getQuantity());
                }
            }
        }
        for (AddOn saved : line.getFavorites()) {
            for (AddOn choice : favoriteChoices) {
                if (choice.getName().equals(saved.getName())) {
                    choice.setQuantity(saved.getQuantity());
                }
            }
        }
    }

    /** Wraps an element so the entrance animation can fade it in. */
    private FadeablePanel fadeWrap(JComponent content) {
        FadeablePanel panel = new FadeablePanel();
        panel.setLayout(new BorderLayout());
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(content, BorderLayout.CENTER);
        entrance.add(panel::setFade);
        return panel;
    }

    // ------------------------------------------------------------
    // Header
    // ------------------------------------------------------------

    /** Logo plus the page title. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JLabel logo = new JLabel(UiKit.icon("public/assets/logo/logo.png", 86, 86));
        header.add(logo, BorderLayout.WEST);
        header.add(centered(buildTitle()), BorderLayout.CENTER);
        header.setPreferredSize(new Dimension(0, 88));
        return header;
    }

    /** The heading, one label per line so the Montserrat font is kept. */
    private JPanel buildTitle() {
        JPanel title = new JPanel();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.setOpaque(false);

        for (String text : pageTitleLines()) {
            JLabel label = new JLabel(text);
            label.setFont(bold.deriveFont(27f));
            label.setForeground(UiColors.ORANGE);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            title.add(label);
        }
        return title;
    }

    // ------------------------------------------------------------
    // Content: product card, extras, favorites
    // ------------------------------------------------------------

    /** The stacked sections between the header and the buttons. */
    private JComponent buildContent() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 12, 0);

        content.add(fadeWrap(buildProductCard()), gbc);
        content.add(fadeWrap(buildExtrasSection()), gbc);
        content.add(fadeWrap(sectionHeading("Add Gudo’s Favorites with your Cravings")), gbc);
        content.add(fadeWrap(buildCarousel()), gbc);

        // Soaks up the room below the last section.
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        content.add(Box.createGlue(), gbc);
        return content;
    }

    /** Picture, name, description, price and the - / quantity / + controls. */
    private JPanel buildProductCard() {
        JPanel card = new JPanel(new BorderLayout(12, 8));
        card.setBackground(UiColors.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiColors.BORDER),
            BorderFactory.createEmptyBorder(10, 10, 10, 12)));
        card.setPreferredSize(new Dimension(0, PRODUCT_CARD_HEIGHT));

        JLabel image = new JLabel(UiKit.icon(product.getImagePath(), 150, 150));
        image.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(image, BorderLayout.WEST);

        JPanel info = new JPanel(new BorderLayout(0, 4));
        info.setOpaque(false);

        JTextArea name = new JTextArea(product.getName());
        name.setFont(bold.deriveFont(20f));
        name.setForeground(UiColors.BROWN);
        name.setOpaque(false);
        name.setEditable(false);
        name.setFocusable(false);
        name.setLineWrap(true);
        name.setWrapStyleWord(true);
        name.setBorder(null);
        name.setMargin(new Insets(0, 0, 0, 0));
        name.setRows(2);

        JTextArea description = new JTextArea(
            DESCRIPTIONS.getOrDefault(product.getName(), FALLBACK_DESCRIPTION));
        description.setFont(regular.deriveFont(12.5f));
        description.setForeground(UiColors.BROWN);
        description.setOpaque(false);
        description.setEditable(false);
        description.setFocusable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(null);
        description.setMargin(new Insets(0, 0, 0, 0));

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);

        JLabel price = new JLabel(UiKit.peso(product.getPrice()));
        price.setFont(bold.deriveFont(19f));
        price.setForeground(UiColors.ORANGE);
        bottom.add(price, BorderLayout.WEST);
        bottom.add(buildQuantityControls(), BorderLayout.EAST);

        info.add(name, BorderLayout.NORTH);
        info.add(description, BorderLayout.CENTER);
        info.add(bottom, BorderLayout.SOUTH);
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    /** The minus button, the quantity read-out and the plus button. */
    private JPanel buildQuantityControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        controls.setOpaque(false);

        JButton minus = quantityButton("−");
        plusButton = quantityButton("+");

        quantityLabel = new JLabel(String.valueOf(quantity), SwingConstants.CENTER);
        quantityLabel.setFont(bold.deriveFont(18f));
        quantityLabel.setForeground(UiColors.ORANGE);
        quantityLabel.setPreferredSize(new Dimension(26, 28));

        minus.addActionListener(e -> changeQuantity(-1));
        plusButton.addActionListener(e -> changeQuantity(1));

        controls.add(minus);
        controls.add(quantityLabel);
        controls.add(plusButton);
        return controls;
    }

    /** A titled box holding one row per extra. */
    private JComponent buildExtrasSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.add(sectionHeading("Extras"));
        section.add(Box.createVerticalStrut(4));
        section.add(buildExtrasBox());
        return section;
    }

    private JPanel buildExtrasBox() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(UiColors.SURFACE);
        box.setBorder(BorderFactory.createLineBorder(UiColors.BORDER));

        for (int i = 0; i < extraChoices.size(); i++) {
            box.add(buildExtraRow(extraChoices.get(i), i == extraChoices.size() - 1));
        }
        return box;
    }

    /** One extra: its name and price on the left, a stepper or a switch on the right. */
    private JComponent buildExtraRow(AddOn extra, boolean last) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, EXTRA_ROW_HEIGHT));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, EXTRA_ROW_HEIGHT));
        row.setMinimumSize(new Dimension(0, EXTRA_ROW_HEIGHT));

        javax.swing.border.Border padding = BorderFactory.createEmptyBorder(3, 10, 3, 10);
        if (last) {
            row.setBorder(padding);
        } else {
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiColors.BORDER), padding));
        }

        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.setOpaque(false);

        JLabel name = new JLabel(extra.getName());
        name.setFont(bold.deriveFont(13f));
        name.setForeground(UiColors.BROWN);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel price = new JLabel(extra.isFree() ? "Free" : UiKit.peso(extra.getPrice()));
        price.setFont(bold.deriveFont(11.5f));
        price.setForeground(UiColors.ORANGE);
        price.setAlignmentX(Component.LEFT_ALIGNMENT);

        labels.add(name);
        labels.add(price);
        row.add(westCentered(labels), BorderLayout.CENTER);

        JComponent control = extra.isFree() ? new ToggleSwitch(extra) : new MiniStepper(extra);
        row.add(centered(control), BorderLayout.EAST);
        return row;
    }

    /** The horizontal row of Gudo's Favorites with its arrow. */
    private JComponent buildCarousel() {
        carouselTrack = new JPanel();
        carouselTrack.setLayout(new BoxLayout(carouselTrack, BoxLayout.X_AXIS));
        carouselTrack.setOpaque(false);

        for (AddOn favorite : favoriteChoices) {
            carouselTrack.add(buildFavoriteCard(favorite));
            carouselTrack.add(Box.createHorizontalStrut(FAVORITE_GAP));
        }

        carouselScroll = new JScrollPane(carouselTrack);
        carouselScroll.setBorder(null);
        carouselScroll.setOpaque(false);
        carouselScroll.getViewport().setOpaque(false);
        carouselScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        carouselScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        carouselScroll.setWheelScrollingEnabled(false);
        carouselScroll.addMouseWheelListener(e -> glideCarousel(e.getWheelRotation() * 40));
        carouselScroll.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                updateArrow();
            }
        });
        // The scroll bar only knows its real range once the viewport has been
        // laid out, so the arrow's state is refreshed from there as well.
        carouselScroll.getViewport().addChangeListener(e -> updateArrow());

        carouselArrow = new ArrowButton();
        carouselArrow.addActionListener(e ->
            glideCarousel(carouselArrow.getDirection() * (FAVORITE_WIDTH + FAVORITE_GAP)));

        // The arrow floats over the right edge of the row, like the design.
        // A layered pane is used so the arrow is painted - and clicked - on
        // top of the scroll pane instead of ending up underneath it.
        JLayeredPane carousel = new JLayeredPane() {
            @Override public void doLayout() {
                carouselScroll.setBounds(0, 0, getWidth(), getHeight());
                int size = carouselArrow.getPreferredSize().width;
                carouselArrow.setBounds(getWidth() - size, (getHeight() - size) / 2, size, size);
            }
        };
        carousel.setOpaque(false);
        carousel.setPreferredSize(new Dimension(0, CAROUSEL_HEIGHT));
        carousel.add(carouselScroll, Integer.valueOf(JLayeredPane.DEFAULT_LAYER));
        carousel.add(carouselArrow, Integer.valueOf(JLayeredPane.PALETTE_LAYER));
        return carousel;
    }

    /** Picture, name, price and a stepper for one favorite. */
    private JComponent buildFavoriteCard(AddOn favorite) {
        Product item = favorite.getProduct();

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UiColors.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiColors.BORDER),
            BorderFactory.createEmptyBorder(8, 8, 6, 8)));

        int cardHeight = CAROUSEL_HEIGHT - 4;
        card.setPreferredSize(new Dimension(FAVORITE_WIDTH, cardHeight));
        card.setMinimumSize(new Dimension(FAVORITE_WIDTH, cardHeight));
        card.setMaximumSize(new Dimension(FAVORITE_WIDTH, cardHeight));

        JLabel image = new JLabel(UiKit.icon(item.getImagePath(), 82, 82));
        image.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(image);
        card.add(Box.createVerticalStrut(2));

        JTextArea name = new JTextArea(item.getName());
        name.setFont(bold.deriveFont(11.5f));
        name.setForeground(UiColors.BROWN);
        name.setOpaque(false);
        name.setEditable(false);
        name.setFocusable(false);
        name.setLineWrap(true);
        name.setWrapStyleWord(true);
        name.setBorder(null);
        name.setMargin(new Insets(0, 0, 0, 0));
        name.setRows(2);
        name.setMaximumSize(new Dimension(FAVORITE_WIDTH - 16, 32));
        card.add(name);

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.setOpaque(false);
        bottom.setMaximumSize(new Dimension(FAVORITE_WIDTH - 16, 24));

        JLabel price = new JLabel(UiKit.peso(item.getPrice()));
        price.setFont(bold.deriveFont(13f));
        price.setForeground(UiColors.ORANGE);
        bottom.add(price, BorderLayout.WEST);
        bottom.add(new MiniStepper(favorite), BorderLayout.EAST);

        card.add(bottom);
        return card;
    }

    /** A bold brown heading above a section. */
    private JLabel sectionHeading(String text) {
        JLabel heading = new JLabel(text);
        heading.setFont(bold.deriveFont(18f));
        heading.setForeground(UiColors.BROWN);
        heading.setPreferredSize(new Dimension(0, 24));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        return heading;
    }

    // ------------------------------------------------------------
    // Carousel scrolling
    // ------------------------------------------------------------

    /** Glides the favorites row toward the target; negative steps scroll back. */
    private void glideCarousel(int delta) {
        if (carouselScroll == null) {
            return;
        }
        JScrollBar bar = carouselScroll.getHorizontalScrollBar();
        int max = Math.max(0, bar.getMaximum() - bar.getVisibleAmount());
        carouselTarget = Math.max(0, Math.min(max, carouselTarget + delta));

        if (carouselTimer == null) {
            carouselTimer = new Timer(16, e -> {
                JScrollBar scrollBar = carouselScroll.getHorizontalScrollBar();
                int value = scrollBar.getValue();
                int distance = carouselTarget - value;
                if (Math.abs(distance) <= 1) {
                    scrollBar.setValue(carouselTarget);
                    carouselTimer.stop();
                    updateArrow();
                    return;
                }
                int step = (int) Math.round(distance * 0.18);
                if (step == 0) {
                    step = distance > 0 ? 1 : -1;
                }
                scrollBar.setValue(value + step);
            });
        }
        if (!carouselTimer.isRunning()) {
            carouselTimer.start();
        }
    }

    private void resetCarousel() {
        if (carouselScroll == null) {
            return;
        }
        if (carouselTimer != null) {
            carouselTimer.stop();
        }
        carouselTarget = 0;
        carouselScroll.getHorizontalScrollBar().setValue(0);
        updateArrow();
    }

    /** Shows the arrow only when there is something to scroll, and turns it around at the ends. */
    private void updateArrow() {
        if (carouselArrow == null || carouselScroll == null) {
            return;
        }
        JScrollBar bar = carouselScroll.getHorizontalScrollBar();
        int max = Math.max(0, bar.getMaximum() - bar.getVisibleAmount());
        carouselArrow.setVisible(max > 0);
        carouselArrow.setEnabled(max > 0);
        if (bar.getValue() >= max - 1) {
            carouselArrow.setDirection(-1);
        } else if (bar.getValue() <= 1) {
            carouselArrow.setDirection(1);
        }
    }

    // ============================================================
    // REUSABLE COMPONENTS
    // ============================================================

    /** Puts a component in the middle of a transparent wrapper (vertical centring). */
    private static JComponent centered(JComponent content) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(content);
        return wrapper;
    }

    /** Stretches a component across the row, keeping its content on the left. */
    private static JComponent westCentered(JComponent content) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wrapper.add(content, gbc);
        return wrapper;
    }

    /** A small orange square holding a white - or +. */
    protected final JButton quantityButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        button.setForeground(UiColors.WHITE);
        button.setBackground(UiColors.ORANGE);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setPreferredSize(new Dimension(32, 32));
        button.setMinimumSize(new Dimension(32, 32));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** The plain bordered button that backs out of the page. */
    protected final JButton createCancelButton(String text) {
        JButton button = new JButton(text);
        button.setFont(regular.deriveFont(14f));
        button.setForeground(UiColors.BROWN);
        button.setBackground(UiColors.BACKGROUND);
        button.setBorder(BorderFactory.createLineBorder(UiColors.BORDER));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** The borderless orange save button; the label shows the running line total. */
    protected final JButton createSaveButton() {
        JButton button = new JButton();
        button.setFont(bold.deriveFont(14f));
        button.setForeground(UiColors.WHITE);
        button.setBackground(UiColors.ORANGE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        UiKit.hover(button, UiColors.ORANGE, UiColors.ORANGE_HOVER);
        button.addActionListener(e -> saveOrder());
        return button;
    }

    // ============================================================
    // BACKEND - quantity rules, extras, saving and leaving the page
    // ============================================================

    /** Moves the quantity by -1 / +1, keeping it inside 1 .. max. */
    protected void changeQuantity(int change) {
        quantity = Math.min(maxQuantity(), Math.max(1, quantity + change));
        quantityLabel.setText(String.valueOf(quantity));
        updateSaveButton();
        updatePlusButton();
    }

    /** Greys and un-disables the + button once the quantity has reached the limit. */
    protected void updatePlusButton() {
        if (plusButton == null) {
            return;
        }
        boolean canAdd = quantity < maxQuantity();
        plusButton.setEnabled(canAdd);
        plusButton.setBackground(canAdd ? UiColors.ORANGE : UiColors.DISABLED);
        plusButton.setCursor(Cursor.getPredefinedCursor(
            canAdd ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /** Keeps the "• ₱..." label in step with the quantity and the chosen add-ons. */
    protected void updateSaveButton() {
        if (saveButton != null) {
            saveButton.setText(saveButtonPrefix() + " • " + UiKit.peso(totalPrice()));
        }
    }

    /** The meal at the current quantity plus every extra and favorite picked. */
    protected final double totalPrice() {
        double total = product.getPrice() * quantity;
        for (AddOn extra : extraChoices) {
            total += extra.getSubtotal();
        }
        for (AddOn favorite : favoriteChoices) {
            total += favorite.getSubtotal();
        }
        return total;
    }

    /** The extras that are switched on, ready to be saved on the line. */
    protected final List<AddOn> selectedExtras() {
        List<AddOn> chosen = new ArrayList<>();
        for (AddOn extra : extraChoices) {
            if (extra.getQuantity() > 0) {
                chosen.add(extra);
            }
        }
        return chosen;
    }

    /** The favorites that are switched on, ready to be saved on the line. */
    protected final List<AddOn> selectedFavorites() {
        List<AddOn> chosen = new ArrayList<>();
        for (AddOn favorite : favoriteChoices) {
            if (favorite.getQuantity() > 0) {
                chosen.add(favorite);
            }
        }
        return chosen;
    }

    /** Commits the choice to the real cart, then hands control back. */
    protected final void saveOrder() {
        commitOrder();
        onSaved.run();
    }

    /** Back out: discard the choice and let KioskMain pick the next page. */
    protected final void cancel() {
        onCancel.run();
    }

    // ============================================================
    // REUSABLE COMPONENTS - small controls used by the add-ons
    // ============================================================

    /** The little round - / quantity / + control used by extras and favorites. */
    private class MiniStepper extends JPanel {
        private final AddOn addOn;
        private final JLabel value;
        private final RoundButton minus;

        MiniStepper(AddOn addOn) {
            this.addOn = addOn;
            setLayout(new FlowLayout(FlowLayout.RIGHT, 7, 0));
            setOpaque(false);

            minus = new RoundButton(false);
            RoundButton plus = new RoundButton(true);
            value = new JLabel("0", SwingConstants.CENTER);
            value.setFont(bold.deriveFont(12.5f));
            value.setPreferredSize(new Dimension(16, 16));

            minus.addActionListener(e -> change(-1));
            plus.addActionListener(e -> change(1));

            add(minus);
            add(value);
            add(plus);
            refresh();
        }

        private void change(int delta) {
            addOn.setQuantity(Math.max(0, Math.min(MAX_ADD_ON, addOn.getQuantity() + delta)));
            refresh();
            updateSaveButton();
        }

        private void refresh() {
            value.setText(String.valueOf(addOn.getQuantity()));
            value.setForeground(addOn.getQuantity() > 0 ? UiColors.ORANGE : UiColors.TEXT_HINT);
            minus.setEnabled(addOn.getQuantity() > 0);
        }
    }

    /** The on / off switch used by the free extras. */
    private class ToggleSwitch extends JComponent {
        private static final int WIDTH = 44;
        private static final int HEIGHT = 24;

        private final AddOn addOn;

        ToggleSwitch(AddOn addOn) {
            this.addOn = addOn;
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    addOn.setQuantity(addOn.getQuantity() > 0 ? 0 : 1);
                    repaint();
                    updateSaveButton();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean on = addOn.getQuantity() > 0;
            g2.setColor(on ? UiColors.ORANGE_TINT : UiColors.overlay(UiColors.BROWN, 40));
            g2.fillRoundRect(0, 0, WIDTH, HEIGHT, HEIGHT, HEIGHT);

            int knob = HEIGHT - 6;
            int x = on ? WIDTH - knob - 3 : 3;
            g2.setColor(on ? UiColors.ORANGE : UiColors.BACKGROUND);
            g2.fillOval(x, 3, knob, knob);
            g2.dispose();
        }
    }

    /** A small orange circle holding a white minus or plus. */
    private static class RoundButton extends JButton {
        private static final int SIZE = 18;

        private final boolean plus;

        RoundButton(boolean plus) {
            this.plus = plus;
            setPreferredSize(new Dimension(SIZE, SIZE));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(isEnabled() ? UiColors.ORANGE : UiColors.DISABLED);
            g2.fillOval(0, 0, SIZE, SIZE);

            double mid = SIZE / 2.0;
            g2.setColor(UiColors.WHITE);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new java.awt.geom.Line2D.Double(5, mid, SIZE - 5, mid));
            if (plus) {
                g2.draw(new java.awt.geom.Line2D.Double(mid, 5, mid, SIZE - 5));
            }
            g2.dispose();
        }
    }

    /** The round orange arrow that glides the favorites row sideways. */
    private static class ArrowButton extends JButton {
        private static final int SIZE = 34;

        /** 1 = points right, -1 = points left. */
        private int direction = 1;

        ArrowButton() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        int getDirection() {
            return direction;
        }

        void setDirection(int direction) {
            this.direction = direction < 0 ? -1 : 1;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(isEnabled() ? UiColors.ORANGE : UiColors.DISABLED);
            g2.fillOval(0, 0, SIZE, SIZE);

            double mid = SIZE / 2.0;
            Path2D chevron = new Path2D.Double();
            if (direction > 0) {
                chevron.moveTo(mid - 4, mid - 6);
                chevron.lineTo(mid + 3, mid);
                chevron.lineTo(mid - 4, mid + 6);
            } else {
                chevron.moveTo(mid + 4, mid - 6);
                chevron.lineTo(mid - 3, mid);
                chevron.lineTo(mid + 4, mid + 6);
            }
            g2.setColor(UiColors.WHITE);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(chevron);
            g2.dispose();
        }
    }
}
