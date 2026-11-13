package ui.kiosk;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import javax.swing.*;
import util.*;

/**
 * Base class for the two pages that build one meal of the order:
 * {@link AddOrder} (a new meal) and {@link EditOrder} (a meal already in the order).
 * (Inheritance)
 *
 * Template Method: this class owns the whole layout - header, meal card, extras,
 * Gudo's Favorites and the bottom buttons - and the fade-in. The subclasses only
 * supply what differs: the title, the save button label, what "save" does and the
 * button bar (Polymorphism).
 *
 * The page edits {@link #work}, a line of its own, so nothing in the real order
 * changes until the customer saves. Extras, favorites, water and cutlery belong to
 * this line only and add to its price. Every stepper asks the cart how much stock
 * is left, so the page can never take more than exists.
 */
abstract class QuantityPage extends JPanel {

    /** The elements fade in one after the other, top to bottom. */
    private static final int FADE_MILLIS = 150;
    private static final int STAGGER_MILLIS = 60;

    /** The line being built (a new one, or a copy of the one being edited). */
    protected final OrderLine work;
    protected final Cart cart;
    /** Runs after the real cart has been updated. */
    protected final Runnable onSaved;
    /** Runs when the customer backs out without changing anything. */
    private final Runnable onCancel;

    private final Product product;
    /** How the line looked when the page opened, to tell whether anything changed. */
    private final OrderLine original;
    /** What is offered under "Extras" and "Gudo's Favorites" (the menu decides). */
    private final List<Product> extraItems;
    private final List<Product> favoriteItems;
    private final Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
    private final Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");
    private final Font inter = UiKit.font("Inter/static/Inter_18pt-Regular.ttf");

    /** Every stepper on the page: they share the stock, so they refresh together. */
    private final List<QuantityStepper> steppers = new ArrayList<>();
    private final FadeInSequence entrance = new FadeInSequence(this, FADE_MILLIS, STAGGER_MILLIS);
    protected JButton saveButton;

    /** The two heading lines next to the logo. */
    protected abstract String[] titleLines();

    /** The start of the save button label ("Add to Order", "Save Changes to Order"). */
    protected abstract String saveButtonPrefix();

    /** Writes the finished line into the cart however this page wants. */
    protected abstract void commitOrder();

    /** The bottom row of buttons; it must set {@link #saveButton}. */
    protected abstract FadeablePanel buildButtons();

    protected QuantityPage(OrderLine work, Cart cart, List<Product> extraItems, List<Product> favoriteItems,
                           Runnable onSaved, Runnable onCancel) {
        this.work = work;
        this.original = work.copy();
        this.extraItems = extraItems;
        this.favoriteItems = favoriteItems;
        this.product = work.getProduct();
        this.cart = cart;
        this.onSaved = onSaved != null ? onSaved : () -> {};
        this.onCancel = onCancel != null ? onCancel : () -> {};

        setLayout(new BorderLayout(0, 14));
        setBackground(UiColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));

        List<FadeablePanel> parts = new ArrayList<>();   // in the order they fade in
        FadeablePanel header = buildHeader();
        FadeablePanel mealCard = buildMealCard();
        FadeablePanel extras = section("Extras", buildExtras());
        FadeablePanel favorites = section("Add Gudo’s Favorites with your Cravings", buildFavorites());
        FadeablePanel buttons = buildButtons();
        parts.addAll(List.of(header, mealCard, extras, favorites, buttons));

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(mealCard);
        details.add(Box.createVerticalStrut(16));
        details.add(extras);
        details.add(Box.createVerticalStrut(14));
        details.add(favorites);
        details.add(Box.createVerticalGlue());

        add(header, BorderLayout.NORTH);
        add(details, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        parts.forEach(part -> entrance.add(part::setFade));
        updateSaveButton();

        // Replay the fade-in every time the page is shown.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                entrance.start();
            } else {
                entrance.reset();
            }
        });
    }

    // ============================================================
    // FRONTEND - header, meal card, extras, favorites
    // ============================================================

    /** Logo plus the two-line title. */
    private FadeablePanel buildHeader() {
        FadeablePanel header = new FadeablePanel();
        header.setOpaque(false);
        header.setLayout(new BorderLayout(16, 0));
        header.setPreferredSize(new Dimension(0, 94));
        header.add(new JLabel(UiKit.icon("public/assets/logo/logo.png", 86, 86)), BorderLayout.WEST);

        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(Box.createVerticalGlue());
        for (String line : titleLines()) {
            title.add(label(line, bold, 29f, UiColors.ORANGE));
        }
        title.add(Box.createVerticalGlue());
        header.add(title, BorderLayout.CENTER);
        return header;
    }

    /** Picture, name, description, price and the meal's - / quantity / + controls. */
    private Card buildMealCard() {
        Card card = new Card(new BorderLayout(14, 0), 12, 12, 12, 14);
        card.add(new JLabel(UiKit.icon(product.getImagePath(), 150, 150)), BorderLayout.WEST);

        JTextArea name = textArea(product.getName(), bold.deriveFont(24f), UiColors.TEXT_DARK);
        boolean twoLines = getFontMetrics(name.getFont()).stringWidth(product.getName()) > 300;
        name.setRows(twoLines ? 2 : 1);

        JTextArea description = textArea(ProductInfo.description(product), inter.deriveFont(13f), UiColors.BROWN);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(label(UiKit.peso(product.getPrice()), bold, 22f, UiColors.ORANGE), BorderLayout.WEST);
        bottom.add(stepper(work.getQuantity(), 1, 24, 22f, product, work::setQuantity), BorderLayout.EAST);

        JPanel info = new JPanel(new BorderLayout(0, 6));
        info.setOpaque(false);
        info.add(name, BorderLayout.NORTH);
        info.add(description, BorderLayout.CENTER);
        info.add(bottom, BorderLayout.SOUTH);
        card.add(info, BorderLayout.CENTER);

        card.setPreferredSize(new Dimension(100, 186));
        return capHeight(card);
    }

    /** The extras in two columns, then the two free yes / no choices. */
    private JPanel buildExtras() {
        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setOpaque(false);
        for (Product extra : extraItems) {
            Card card = new Card(new BorderLayout(6, 0), 6, 12, 6, 10);
            card.add(nameAndNote(extra.getName(), UiKit.peso(extra.getPrice())), BorderLayout.WEST);
            card.add(stepper(work.getExtra(extra), 0, 17, 14f, extra, amount -> work.setExtra(extra, amount)),
                BorderLayout.EAST);
            card.setPreferredSize(new Dimension(0, 54));
            grid.add(card);
        }

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(grid);
        body.add(Box.createVerticalStrut(8));
        body.add(toggleCard("Extra Water", work.isWater(), work::setWater));
        body.add(Box.createVerticalStrut(8));
        body.add(toggleCard("Cutlery", work.isCutlery(), work::setCutlery));
        return body;
    }

    /** Gudo's Favorites in a sliding row. */
    private JComponent buildFavorites() {
        List<Card> cards = favoriteItems.stream().map(this::favoriteCard).collect(Collectors.toList());
        return new Carousel(cards, 12);
    }

    private Card favoriteCard(Product favorite) {
        Card card = new Card(new BorderLayout(0, 4), 10, 12, 10, 12);
        card.setPreferredSize(new Dimension(172, 196));

        JLabel picture = new JLabel(UiKit.icon(favorite.getImagePath(), 100, 100), SwingConstants.CENTER);
        JTextArea name = textArea(favorite.getName(), bold.deriveFont(14f), UiColors.TEXT_DARK);
        name.setPreferredSize(new Dimension(148, 38));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(label(UiKit.peso(favorite.getPrice()), bold, 14f, UiColors.ORANGE), BorderLayout.WEST);
        bottom.add(stepper(work.getFavorite(favorite), 0, 15, 13f, favorite,
            amount -> work.setFavorite(favorite, amount)), BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setOpaque(false);
        south.add(name, BorderLayout.NORTH);
        south.add(bottom, BorderLayout.SOUTH);

        card.add(picture, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private Card toggleCard(String title, boolean on, Consumer<Boolean> onToggle) {
        Card card = new Card(new BorderLayout(), 6, 12, 6, 12);
        card.add(nameAndNote(title, "Free"), BorderLayout.WEST);
        card.add(new ToggleSwitch(on, value -> { onToggle.accept(value); updateSaveButton(); }), BorderLayout.EAST);
        card.setPreferredSize(new Dimension(0, 50));
        return capHeight(card);
    }

    // ------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------

    /** A bold heading above its content; the section fades in as one piece. */
    private FadeablePanel section(String title, JComponent body) {
        FadeablePanel section = new FadeablePanel();
        section.setOpaque(false);
        section.setLayout(new BorderLayout(0, 8));
        section.add(label(title, bold, 19f, UiColors.TEXT_DARK), BorderLayout.NORTH);
        section.add(body, BorderLayout.CENTER);
        return capHeight(section);
    }

    /** A bold name over a small orange note (a price, or "Free"). */
    private JPanel nameAndNote(String name, String note) {
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Box.createVerticalGlue());
        text.add(label(name, bold, 14f, UiColors.TEXT_DARK));
        text.add(label(note, bold, 11f, UiColors.ORANGE));
        text.add(Box.createVerticalGlue());
        return text;
    }

    private static JLabel label(String text, Font font, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font.deriveFont(size));
        label.setForeground(color);
        return label;
    }

    private static JTextArea textArea(String text, Font font, Color color) {
        JTextArea area = new JTextArea(text);
        area.setFont(font);
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setMargin(new Insets(0, 0, 0, 0));
        return area;
    }

    /** Stops BoxLayout from stretching a row taller than it needs to be. */
    private static <T extends JComponent> T capHeight(T component) {
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    /** A bottom bar for the subclasses; it fades in with the rest of the page. */
    protected final FadeablePanel buttonBar(LayoutManager layout, int height) {
        FadeablePanel bar = new FadeablePanel();
        bar.setOpaque(false);
        bar.setLayout(layout);
        bar.setPreferredSize(new Dimension(0, height));
        return bar;
    }

    /** The rounded outline button with a soft shadow (Cancel Order, Go Back, Remove from Order). */
    protected final JButton createOutlineButton(String text, Color color) {
        JButton button = new RoundedButton(text, null, UiColors.SURFACE, UiColors.HOVER_GREY, color);
        button.setFont(regular.deriveFont(14f));
        button.setForeground(color);
        return button;
    }

    /** The rounded orange save button with a soft shadow; the label updates with the price. */
    protected final JButton createSaveButton() {
        JButton button = new RoundedButton(null, null, UiColors.ORANGE, UiColors.ORANGE_HOVER, null);
        button.setFont(bold.deriveFont(14f));
        button.setForeground(UiColors.WHITE);
        button.addActionListener(e -> saveOrder());
        return button;
    }

    /** A card with rounded corners, a thin outline and a soft shadow. */
    private static class Card extends FadeablePanel {
        private static final int ARC = 12;
        private static final int SHADOW = 3;

        Card(LayoutManager layout, int top, int left, int bottom, int right) {
            setLayout(layout);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(top, left, bottom + SHADOW, right));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight() - SHADOW;
            for (int i = SHADOW; i > 0; i--) {
                g2.setColor(UiColors.shadow(4 + (SHADOW - i) * 4));
                g2.fillRoundRect(0, i, getWidth() - 1, h, ARC, ARC);
            }
            g2.setColor(UiColors.BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth() - 1, h, ARC, ARC);
            g2.setColor(UiColors.overlay(UiColors.BROWN, 120));
            g2.drawRoundRect(0, 0, getWidth() - 2, h - 1, ARC, ARC);
            g2.dispose();
        }
    }

    // ============================================================
    // BACKEND - stock-aware steppers, saving and leaving the page
    // ============================================================

    /**
     * A - / + control for one product on this line. It may go up only while the
     * cart says stock is left; every change refreshes all steppers, because they
     * share the stock.
     */
    private QuantityStepper stepper(int start, int min, int size, float fontSize,
                                    Product item, IntConsumer onChange) {
        QuantityStepper stepper = new QuantityStepper(start, min, size, bold.deriveFont(fontSize),
            () -> cart.remaining(work, item) > 0,
            amount -> {
                onChange.accept(amount);
                steppers.forEach(QuantityStepper::refresh);
                updateSaveButton();
            });
        steppers.add(stepper);
        return stepper;
    }

    /** Whether the customer has changed anything since the page opened. */
    protected final boolean hasChanges() {
        return !work.sameContentAs(original);
    }

    /** Whether the save button may be pressed right now; EditOrder waits for a change. */
    protected boolean canSave() {
        return true;
    }

    /** Keeps the "• ₱..." label in step with the line, and the button on or off. */
    protected void updateSaveButton() {
        if (saveButton != null) {
            saveButton.setText(saveButtonPrefix() + " • " + UiKit.peso(work.getSubtotal()));
            saveButton.setEnabled(canSave());
        }
    }

    /** Commits the line to the real cart, then hands control back. */
    protected final void saveOrder() {
        commitOrder();
        onSaved.run();
    }

    /** Back out: discard the changes and let KioskMain pick the next page. */
    protected final void cancel() {
        onCancel.run();
    }
}
