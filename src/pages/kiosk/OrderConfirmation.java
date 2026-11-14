package pages.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import java.awt.event.HierarchyEvent;
import javax.swing.*;
import util.ui.ConfirmationBackground;
import util.ui.FadeInPage.FadeInSequence;
import util.ui.FadingPanel;
import util.ui.UiColors;
import util.ui.UiKit;
import util.ui.VideoPanel;

/**
 * The page shared by the two order confirmations ({@link OrderAdded} and
 * {@link OrderEdited}): logo, two-line heading, subheading, running total and
 * the added-order animation.
 *
 * Template method: this class builds and animates the whole page; a subclass
 * only supplies what differs - its heading text - and where to go next.
 *
 * Every time the page is shown, the top ribbon, logo, heading, subheading
 * (with the total) and bottom ribbon fade in one after the other while the
 * bag animation plays. The page then returns to the next screen after
 * {@link #AUTO_REDIRECT_MILLIS}.
 */
public abstract class OrderConfirmation extends ConfirmationBackground {

    // ============================================================
    // FRONTEND - layout and entrance animation
    // ============================================================

    /** Entrance animation: how long one element fades, and the delay between elements. */
    private static final int FADE_MILLIS = 300;
    private static final int STAGGER_MILLIS = 120;

    private static final int LOGO_SIZE = 166;
    /** The logo picture has empty margins; this much is trimmed off its top and bottom. */
    private static final int LOGO_TRIM = 8;
    /** The bag animation is a square clip; the bag fills about 78% of it. */
    private static final int ANIMATION_SIZE = 248;

    /** Plays added-order.mp4 whenever the page is shown. */
    private final VideoPanel animation;
    /** Fades the page elements in, in order, whenever the page is shown. */
    private final FadeInSequence entrance;
    /** Stops the automatic redirect if the page is thrown away early. */
    private final Timer redirectTimer;

    /**
     * @param headingTop    first heading line, e.g. "Your Cravings has"
     * @param headingBottom second heading line, e.g. "Been Answered"
     * @param total         the running total shown under the heading
     * @param onFinished    runs {@link #AUTO_REDIRECT_MILLIS} after the page opens
     */
    protected OrderConfirmation(String headingTop, String headingBottom,
                                double total, Runnable onFinished) {
        Runnable next = onFinished != null ? onFinished : () -> {};
        redirectTimer = new Timer(AUTO_REDIRECT_MILLIS, e -> next.run());
        redirectTimer.setRepeats(false);

        setLayout(new BorderLayout());
        setBackground(UiColors.BACKGROUND);

        Font bold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
        Font regular = UiKit.font("Inter/static/Inter_18pt-Regular.ttf");

        FadingPanel logo = new FadingPanel(buildLogo());
        FadingPanel heading = new FadingPanel(buildHeading(headingTop, headingBottom, bold.deriveFont(30.5f)));
        FadingPanel details = new FadingPanel(buildDetails(total, bold.deriveFont(20.5f), regular.deriveFont(15f)));

        // The animation starts on a blank frame and draws itself, so it needs no fade.
        animation = new VideoPanel("public/assets/animations/added-order.mp4",
            ANIMATION_SIZE, ANIMATION_SIZE);
        animation.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(144, 24, 0, 24));
        content.add(logo);
        content.add(heading);
        content.add(details);
        content.add(animation);
        content.add(Box.createVerticalGlue());
        add(content, BorderLayout.CENTER);

        // The order the elements appear in.
        entrance = new FadeInSequence(this, FADE_MILLIS, STAGGER_MILLIS);
        entrance.add(alpha -> topLeftRibbonAlpha = alpha);
        entrance.add(logo::setFade);
        entrance.add(heading::setFade);
        entrance.add(details::setFade);
        entrance.add(alpha -> bottomRightRibbonAlpha = alpha);
        entrance.reset();   // nothing is visible until the page is shown

        // A hierarchy listener also fires the first time the page is displayed.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            if (isShowing()) {
                entrance.start();
                animation.play();
            } else {
                entrance.reset();
                animation.stop();
            }
        });

        redirectTimer.start();
    }

    // ------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------

    private JLabel buildLogo() {
        JLabel logo = new JLabel(UiKit.icon("public/assets/logo/logo.png", LOGO_SIZE, LOGO_SIZE));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(LOGO_SIZE, LOGO_SIZE - 2 * LOGO_TRIM));
        return logo;
    }

    /** Two labels, not one HTML label: HTML would ignore the Montserrat font. */
    private JPanel buildHeading(String top, String bottom, Font font) {
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.add(buildLabel(top, font, UiColors.ORANGE));
        heading.add(buildLabel(bottom, font, UiColors.ORANGE));
        return heading;
    }

    /** The subheading with the running total under it. */
    private JPanel buildDetails(double total, Font totalFont, Font subheadingFont) {
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        details.add(buildLabel("Your total has been updated", subheadingFont, UiColors.BROWN));
        details.add(Box.createVerticalStrut(33));
        details.add(buildLabel(UiKit.peso(total), totalFont, UiColors.ORANGE));
        return details;
    }

    private JLabel buildLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    // ============================================================
    // BACKEND - the automatic redirect
    // ============================================================

    @Override
    public void removeNotify() {
        // If this page is discarded early, make sure the timer cannot fire later.
        redirectTimer.stop();
        super.removeNotify();
    }
}