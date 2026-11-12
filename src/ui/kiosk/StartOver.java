package ui.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import util.ConfirmationBackground;
import util.FadingPanel;
import util.UiColors;
import util.UiKit;
import util.VideoPanel;

/**
 * "Are you sure?" prompt shown when the customer wants to throw the whole
 * order away.
 *
 * Yes clears the cart and returns to the landing page; No simply goes back
 * to whichever page the prompt was opened from (KioskMain decides).
 *
 * Whenever the kiosk opens this page, the top ribbon, logo, heading, subtitle
 * and buttons fade in one after the other while the bag animation plays in the
 * middle. The animation freezes on its finished picture and stays there until
 * the customer leaves the page.
 */
public class StartOver extends ConfirmationBackground {

    // ============================================================
    // FRONTEND - logo, message, animation and the Start Over / Go Back buttons
    // ============================================================

    /** Yes: throw the order away and start again. */
    private final Runnable onStartOver;
    /** No: return to the previous page with the order untouched. */
    private final Runnable onGoBack;

    // ------------------------------------------------------------
    // Entrance animation - the staggered fade-in of the page
    // ------------------------------------------------------------

    /** The page elements in the order they fade in. */
    private static final int TOP_LEFT_RIBBON = 0;
    private static final int LOGO = 1;
    private static final int HEADING = 2;
    private static final int SUBTITLE = 3;
    private static final int BUTTONS = 4;
    private static final int FADE_STEPS = 5;

    /** How long one element takes to go from invisible to fully shown. */
    private static final int FADE_MILLIS = 125;
    /** How much later each element starts fading than the one before it. */
    private static final int STAGGER_MILLIS = 25;
    // (FADE_STEPS - 1) x 25 ms stagger + 125 ms fade = 225 ms for the page.

    /** Size of the bag animation in the middle of the page. */
    private static final int ANIMATION_SIZE = 320;

    /** Fade wrappers around the elements; filled in while the page is built. */
    private FadingPanel logoFade;
    private FadingPanel headingFade;
    private FadingPanel subtitleFade;
    private FadingPanel buttonsFade;

    /** Drives the entrance animation while this page is on screen. */
    private final Timer fadeInTimer;
    /** When the running fade-in started, in nanoseconds. */
    private long fadeStartNanos;

    /** Plays start-over.mp4 whenever the page is shown. */
    private final VideoPanel animation;

    public StartOver(Runnable onStartOver, Runnable onGoBack) {
        this.onStartOver = onStartOver != null ? onStartOver : () -> {};
        this.onGoBack = onGoBack != null ? onGoBack : () -> {};

        setLayout(new BorderLayout());
        setBackground(UiColors.BACKGROUND);

        Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
        Font regular = UiKit.font("Montserrat/static/Montserrat-Regular.ttf");

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(100, 26, 0, 26));

        logoFade = new FadingPanel(buildLogo(150));
        content.add(logoFade);
        content.add(Box.createVerticalStrut(14));

        headingFade = new FadingPanel(buildHeading(bold.deriveFont(30f)));
        content.add(headingFade);
        content.add(Box.createVerticalStrut(8));

        subtitleFade = new FadingPanel(buildSubtitle(regular.deriveFont(15f)));
        content.add(subtitleFade);
        content.add(Box.createVerticalStrut(18));

        // The bag animation starts on a blank frame and draws itself, so it
        // needs no fade wrapper - it simply replays each time the page opens.
        animation = new VideoPanel("public/assets/animations/start-over.mp4",
            ANIMATION_SIZE, ANIMATION_SIZE);
        animation.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(animation);

        content.add(Box.createVerticalStrut(30));
        buttonsFade = new FadingPanel(buildButtons(bold, regular));
        content.add(buttonsFade);
        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);

        // Nothing is visible until the page fades in.
        fadeInTimer = new Timer(10, e -> animateFadeIn());
        hideFadeSteps();

        // Replay the whole entrance every time the kiosk opens this page.
        // Hiding it puts it straight back to the blank starting state, so the
        // next visit never flashes the fully faded-in page for a frame first.
        // A hierarchy listener is used (not componentShown): it also fires the
        // first time the page is displayed, when it is already marked visible.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                startFadeIn();
                animation.play();
            } else {
                fadeInTimer.stop();
                hideFadeSteps();
                animation.stop();
            }
        });
    }

    // ------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------

    private JLabel buildLogo(int size) {
        JLabel logo = new JLabel(UiKit.icon("public/assets/logo/logo.png", size, size));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        return logo;
    }

    /**
     * The two headline lines. Each line is its own label: an HTML label would
     * ignore the Montserrat font and draw the heading in the default UI font.
     */
    private JPanel buildHeading(Font font) {
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.add(buildHeadline("Every Craving Deserves", font));
        heading.add(buildHeadline("a Second Chance", font));
        return heading;
    }

    private JLabel buildHeadline(String text, Font font) {
        JLabel line = new JLabel(text);
        line.setFont(font);
        line.setForeground(UiColors.ORANGE);
        line.setAlignmentX(Component.CENTER_ALIGNMENT);
        line.setHorizontalAlignment(SwingConstants.CENTER);
        return line;
    }

    private JLabel buildSubtitle(Font font) {
        JLabel subtitle = new JLabel("Change your mind? Start your order anew");
        subtitle.setFont(font);
        subtitle.setForeground(UiColors.BROWN);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        return subtitle;
    }

    /** The two stacked buttons, faded in together. */
    private JPanel buildButtons(Font bold, Font regular) {
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setOpaque(false);

        RoundedButton start = new RoundedButton("Start Over", bold.deriveFont(16f),
            UiColors.ORANGE, UiColors.ORANGE_HOVER, null);
        start.setForeground(UiColors.WHITE);
        start.addActionListener(e -> confirmStartOver());
        buttons.add(start);
        buttons.add(Box.createVerticalStrut(10));

        // Go Back: the secondary text colour at 33% opacity for its outline.
        RoundedButton back = new RoundedButton("Go Back", regular.deriveFont(15f),
            UiColors.BACKGROUND, UiColors.HOVER_GREY,
            UiColors.overlay(UiColors.BROWN, 84));
        back.setForeground(UiColors.BROWN);
        back.addActionListener(e -> goBack());
        buttons.add(back);

        Dimension size = new Dimension(330, 48);
        for (Component button : buttons.getComponents()) {
            if (button instanceof RoundedButton) {
                button.setMaximumSize(size);
                button.setPreferredSize(size);
                ((JComponent) button).setAlignmentX(Component.CENTER_ALIGNMENT);
            }
        }
        return buttons;
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
            case HEADING -> headingFade.setAlpha(alpha);
            case SUBTITLE -> subtitleFade.setAlpha(alpha);
            case BUTTONS -> buttonsFade.setAlpha(alpha);
        }
    }

    // ============================================================
    // REUSABLE COMPONENTS - rounded button with a soft shadow
    // ============================================================

    /** Rounded button with a soft drop shadow and an optional outline. */
    private static class RoundedButton extends JButton {
        private static final int ARC = 8;
        private static final int SHADOW = 3;

        private final Color base;
        private final Color hover;
        private final Color outline;

        RoundedButton(String text, Font font, Color base, Color hover, Color outline) {
            super(text);
            this.base = base;
            this.hover = hover;
            this.outline = outline;
            setFont(font);
            setBackground(base);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(hover); }
                @Override public void mouseExited(MouseEvent e) { setBackground(base); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int faceHeight = getHeight() - SHADOW;

            for (int offset = SHADOW; offset > 0; offset--) {
                g2.setColor(UiColors.shadow(5 + (SHADOW - offset) * 5));
                g2.fillRoundRect(0, offset, getWidth(), faceHeight, ARC, ARC);
            }

            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), faceHeight, ARC, ARC);

            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, faceHeight - 1, ARC, ARC);
            }
            g2.dispose();

            Graphics textGraphics = g.create();
            textGraphics.translate(0, -SHADOW / 2);
            super.paintComponent(textGraphics);
            textGraphics.dispose();
        }
    }

    // ============================================================
    // BACKEND - clearing the order or backing out
    // ============================================================

    /** Yes: hand control to KioskMain, which clears the cart and shows the landing page. */
    private void confirmStartOver() {
        onStartOver.run();
    }

    /** No: return to the page this prompt was opened from, order untouched. */
    private void goBack() {
        onGoBack.run();
    }
}
