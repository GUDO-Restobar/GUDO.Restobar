package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import javax.swing.*;
import util.*;

/**
 * Asks the customer how they want to receive the order: Dine-in or Takeout.
 *
 * The choice is stored in {@link OrderSession} and the page reports back to
 * KioskMain, which then shows the menu. Drawn on
 * {@link util.ConfirmationBackground}, so it carries the same orange corner
 * ribbons as the confirmation pages.
 *
 * Whenever the kiosk opens this page, every element fades in one after the
 * other - top ribbon, logo, title, sub-heading, Dine-in card with its
 * pointing hand, Takeout card with its pointing hand, back button, bottom
 * ribbon - so the page is fully visible in about 0.3 s.
 */
public class OrderOption extends ConfirmationBackground {

    // ============================================================
    // FRONTEND - heading, order-type cards and the back button
    // ============================================================

    /** Runs once an order type has been chosen. */
    private final Runnable onOrderTypeSelected;
    /** Runs when the customer wants to return to the landing page. */
    private final Runnable onBack;

    // ------------------------------------------------------------
    // Entrance animation - the staggered fade-in of the whole page
    // ------------------------------------------------------------

    /** The page elements in the order they fade in. */
    private static final int TOP_LEFT_RIBBON = 0;
    private static final int LOGO = 1;
    private static final int TITLE = 2;
    private static final int SUB_HEADING = 3;
    private static final int DINE_IN_CARD = 4;   // fades in with the left hand
    private static final int TAKEOUT_CARD = 5;  // fades in with the right hand
    private static final int BACK_BUTTON = 6;
    private static final int BOTTOM_RIGHT_RIBBON = 7;
    private static final int FADE_STEPS = 8;

    /** How long one element takes to go from invisible to fully shown. */
    private static final int FADE_MILLIS = 125;
    /** How much later each element starts fading than the one before it. */
    private static final int STAGGER_MILLIS = 25;
    // (FADE_STEPS - 1) x 25 ms stagger + 125 ms fade = 300 ms for the whole page.

    /** How far an order card floats up while the mouse is over it. */
    private static final int CARD_RISE_PX = 15;

    /**
     * How dark the back-button picture gets while the mouse is over it.
     * 38/255 over the orange picture is the same step as the landing page's
     * Start Order button (#EC5E02 -> #C94F00, about 15% darker).
     */
    private static final int BACK_HOVER_DARKEN_ALPHA = 38;

    /** Fade wrappers around the elements; filled in while the page is built. */
    private FadingPanel logoFade;
    private FadingPanel titleFade;
    private FadingPanel subHeadingFade;
    private RisingPanel dineInCardFade;
    private RisingPanel leftHandFade;
    private RisingPanel takeoutCardFade;
    private RisingPanel rightHandFade;
    private FadingPanel backButtonFade;

    /** Drives the entrance animation while this page is on screen. */
    private final Timer fadeInTimer;
    /** When the running fade-in started, in nanoseconds. */
    private long fadeStartNanos;

    public OrderOption(Runnable onOrderTypeSelected, Runnable onBack) {
        this.onOrderTypeSelected = onOrderTypeSelected != null ? onOrderTypeSelected : () -> {};
        this.onBack = onBack != null ? onBack : () -> {};

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiColors.BACKGROUND);

        add(Box.createRigidArea(new Dimension(0, 30)));
        add(buildHeading());
        add(Box.createRigidArea(new Dimension(0, 30)));
        add(buildOptionCards());
        add(Box.createRigidArea(new Dimension(0, -150)));
        add(buildHands());
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(buildBackBar());

        // Nothing is visible until the page fades in.
        fadeInTimer = new Timer(10, e -> animateFadeIn());
        hideFadeSteps();

        // Run the fade-in from the beginning every time the kiosk opens this
        // page. Hiding it puts it straight back to the blank starting state,
        // so the next visit always begins on the plain background instead of
        // flashing the fully faded-in page for a frame first.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                startFadeIn();
            }
            @Override public void componentHidden(java.awt.event.ComponentEvent e) {
                fadeInTimer.stop();
                hideFadeSteps();
                dineInCardFade.settle();    // never come back with a card floating
                takeoutCardFade.settle();
            }
        });
    }

    /** Logo, the two headline lines and the sub-heading. */
    private JPanel buildHeading() {
        Font montserratBold = UiKit.font("Inter/static/Montserrat-Bold.ttf")
            .deriveFont(Font.PLAIN, 32f);
        Font interLight = UiKit.font("Inter/static/Inter_18pt-Light.ttf")
            .deriveFont(Font.PLAIN, 16f);

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        logoFade = new FadingPanel(buildLogo(202));
        heading.add(logoFade);

        JPanel title = new JPanel();
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.setOpaque(false);
        title.add(buildHeadline("Gudo Has Answered", montserratBold));
        title.add(buildHeadline("Your Cravings", montserratBold));
        titleFade = new FadingPanel(title);
        heading.add(titleFade);

        heading.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel subHeading = new JLabel("Choose how you shall receive your blessings.");
        subHeading.setFont(interLight);
        subHeading.setAlignmentX(Component.CENTER_ALIGNMENT);
        subHeadingFade = new FadingPanel(subHeading);
        heading.add(subHeadingFade);

        return heading;
    }

    /** The two big picture cards: Dine-in and Takeout. */
    private JPanel buildOptionCards() {
        // The wrappers are 2 x CARD_RISE_PX taller than the cards (room to
        // float up), so the row gap shrinks by the same amount and the cards
        // still rest exactly where they always did.
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 0));
        center.setOpaque(false);
        center.setAlignmentX(Component.CENTER_ALIGNMENT);
        dineInCardFade = new RisingPanel(
            buildOrderCard("public/assets/icons/import_dinein.png", "Dine-in"), CARD_RISE_PX);
        takeoutCardFade = new RisingPanel(
            buildOrderCard("public/assets/icons/export_takeout.png", "Takeout"), CARD_RISE_PX);
        center.add(dineInCardFade);
        center.add(takeoutCardFade);
        return center;
    }

    /**
     * The pair of pointing hands sitting under the cards. Each hand is a
     * follower of its card, so it rises and settles together with it.
     */
    private JPanel buildHands() {
        JPanel handsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        handsPanel.setOpaque(false);
        handsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftHandFade = dineInCardFade.follower(
            buildScaledLabel("public/assets/icons/left_hand.png", 270, 280));
        rightHandFade = takeoutCardFade.follower(
            buildScaledLabel("public/assets/icons/right_hand.png", 270, 280));
        handsPanel.add(leftHandFade);
        handsPanel.add(rightHandFade);
        return handsPanel;
    }

    /** The bar holding the single Back button (transparent so the corner ribbon shows). */
    private JPanel buildBackBar() {
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));
        backButtonFade = new FadingPanel(buildBackButton());
        bar.add(backButtonFade);
        return bar;
    }

    // ------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------

    /** One large picture card that also carries the order type it selects. */
    private JButton buildOrderCard(String imagePath, String orderType) {
        JButton button = new JButton(UiKit.icon(imagePath, 208, 269));
        button.setPreferredSize(new Dimension(208, 269));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> chooseOrderType(orderType));
        return button;
    }

    /** The image-only Back button used at the bottom of the page. */
    private JButton buildBackButton() {
        ImageIcon normal = UiKit.icon("public/assets/icons/button.png", 329, 49);
        ImageIcon hover = darken(normal);

        // The button is only its picture, so the transparent corners of the
        // PNG stay clear and the corner ribbon shows through. While the mouse
        // is over it the picture is swapped for the darkened copy, which is
        // the same effect as the Start Order button's hover colour.
        JButton button = new JButton(normal) {
            @Override protected void paintComponent(Graphics g) {
                Icon icon = getModel().isRollover() ? hover : normal;
                icon.paintIcon(this, g,
                    (getWidth() - icon.getIconWidth()) / 2,
                    (getHeight() - icon.getIconHeight()) / 2);
            }
        };
        button.setPreferredSize(new Dimension(329, 49));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> goBack());
        return button;
    }

    /**
     * A darkened copy of a picture. The veil is composited "source atop" onto
     * a transparent copy, so only the drawn pixels get darker - the clear
     * corners of the original stay fully transparent.
     */
    private static ImageIcon darken(ImageIcon icon) {
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        java.awt.image.BufferedImage copy = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = copy.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.setComposite(AlphaComposite.SrcAtop);
        // Pure black (not the theme's brown "black") so the picture takes the
        // exact same darkening step as the Start Order button.
        g.setColor(new Color(0, 0, 0, BACK_HOVER_DARKEN_ALPHA));
        g.fillRect(0, 0, width, height);
        g.dispose();

        return new ImageIcon(copy);
    }

    private JLabel buildHeadline(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(UiColors.ORANGE);
        return label;
    }

    private JLabel buildLogo(int size) {
        JLabel logo = new JLabel(UiKit.icon("public/assets/logo/logo.png", size, size));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        return logo;
    }

    private static JLabel buildScaledLabel(String path, int width, int height) {
        return new JLabel(UiKit.icon(path, width, height));
    }

    // ============================================================
    // ENTRANCE ANIMATION - staggered fade-in of every element
    // ============================================================

    /** Hides every element, ready to fade in. */
    private void hideFadeSteps() {
        for (int step = 0; step < FADE_STEPS; step++) {
            applyFadeAlpha(step, 0f);
        }
    }

    /** Restarts the fade-in: every element from invisible to fully shown. */
    private void startFadeIn() {
        hideFadeSteps();
        fadeStartNanos = System.nanoTime();
        fadeInTimer.restart();
    }

    /** One animation tick: moves each element along its own fade. */
    private void animateFadeIn() {
        long elapsedMillis = (System.nanoTime() - fadeStartNanos) / 1_000_000L;

        for (int step = 0; step < FADE_STEPS; step++) {
            applyFadeAlpha(step, fadeFraction(elapsedMillis - step * STAGGER_MILLIS));
        }
        repaint();

        if (elapsedMillis >= (FADE_STEPS - 1) * STAGGER_MILLIS + FADE_MILLIS) {
            fadeInTimer.stop();
        }
    }

    /**
     * 0 while an element is still waiting for its turn, then a quadratic
     * ease-out to 1 over {@link #FADE_MILLIS}: elements appear quickly and
     * settle softly instead of moving at a constant speed.
     */
    private static float fadeFraction(long millisSinceStart) {
        if (millisSinceStart <= 0) {
            return 0f;
        }
        if (millisSinceStart >= FADE_MILLIS) {
            return 1f;
        }
        float progress = millisSinceStart / (float) FADE_MILLIS;
        return 1f - (1f - progress) * (1f - progress);
    }

    /** Puts one fade-in element at the given visibility (0 = hidden, 1 = shown). */
    private void applyFadeAlpha(int step, float alpha) {
        switch (step) {
            case TOP_LEFT_RIBBON -> topLeftRibbonAlpha = alpha;
            case LOGO -> logoFade.setAlpha(alpha);
            case TITLE -> titleFade.setAlpha(alpha);
            case SUB_HEADING -> subHeadingFade.setAlpha(alpha);
            case DINE_IN_CARD -> {
                dineInCardFade.setAlpha(alpha);
                leftHandFade.setAlpha(alpha);
            }
            case TAKEOUT_CARD -> {
                takeoutCardFade.setAlpha(alpha);
                rightHandFade.setAlpha(alpha);
            }
            case BACK_BUTTON -> backButtonFade.setAlpha(alpha);
            case BOTTOM_RIGHT_RIBBON -> bottomRightRibbonAlpha = alpha;
        }
    }

    // ============================================================
    // BACKEND - remember the choice, then hand over to KioskMain
    // ============================================================

    /** Stores Dine-in / Takeout for the whole order, then shows the menu. */
    private void chooseOrderType(String orderType) {
        OrderSession.getInstance().setOrderType(orderType);
        onOrderTypeSelected.run();
    }

    /** Returns to the landing page. */
    private void goBack() {
        onBack.run();
    }
}
