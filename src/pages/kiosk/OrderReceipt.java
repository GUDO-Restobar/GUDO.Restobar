package pages.kiosk;

// ============================================================
// IMPORTS
// ============================================================
import java.awt.*;
import javax.swing.*;
import util.feat.*;
import util.ui.*;

/**
 * The final screen: the order number the customer waits for, and how the
 * order was taken and paid for.
 *
 * Reuse and OOP:
 *  - Inheritance / Template Method: extends {@link FadeInPage}, which owns the
 *    staggered fade-in; this page only lists its elements in order.
 *  - Strategy (the {@link Message} enum): cash and cashless only differ in
 *    their wording, so each payment method carries its own headline and
 *    subtitle instead of the page branching on it.
 *  - Encapsulation: the card, text blocks and loading logo hide their own
 *    painting; the rest of the kiosk only calls {@link #setOrderInfo}.
 *
 * While the order is being saved a pulsing logo is shown. KioskMain calls
 * {@link #setOrderInfo} once the order number exists, and the page then fades in.
 */
public class OrderReceipt extends FadeInPage {

    // ============================================================
    // WORDING - what each kind of payment says (Strategy)
    // ============================================================

    private enum Message {
        /** Cash: the order is not paid yet, so send the customer to the counter. */
        PAY_AT_COUNTER(
            new String[] {"Complete your Payment", "at the Counter"},
            new String[] {"Please collect your receipt and proceed to the", "counter to pay in cash."}),
        /** Cashless: already paid, just wait for the number. */
        FEAST_AWAITS(
            new String[] {"Your Feast Awaits!"},
            new String[] {"Please collect your receipt and wait for your order", "number to be called."});

        final String[] headline;
        final String[] subtitle;

        Message(String[] headline, String[] subtitle) {
            this.headline = headline;
            this.subtitle = subtitle;
        }

        static Message forPayment(String paymentMethod) {
            return "Cashless".equalsIgnoreCase(paymentMethod) ? FEAST_AWAITS : PAY_AT_COUNTER;
        }
    }

    // ============================================================
    // FRONTEND - logo, headline, order-number card and footer
    // ============================================================

    /** The pulsing logo is shown at least this long, so it never just flashes. */
    private static final int MIN_LOADING_MILLIS = 700;
    private static final String LOADING = "loading";
    private static final String CONTENT = "content";

    private final Font montserratBold = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");
    private final Font interLight = UiKit.font("Inter/static/Inter_18pt-Light.ttf");

    private final CardLayout stageLayout = new CardLayout();
    private final JPanel stage = new JPanel(stageLayout);
    private final LoadingLogo loadingLogo = new LoadingLogo();

    private final TextBlock headline = new TextBlock(montserratBold.deriveFont(Font.PLAIN, 32f), UiColors.ORANGE);
    private final TextBlock subtitle = new TextBlock(interLight.deriveFont(Font.PLAIN, 16f), UiColors.TEXT_DARK);
    private final OrderCard card = new OrderCard(montserratBold, interLight);

    private final Timer revealTimer = new Timer(0, e -> reveal());
    private long loadingStartedAt;

    public OrderReceipt() {
        setLayout(new BorderLayout());
        revealTimer.setRepeats(false);

        stage.setOpaque(false);
        stage.add(buildLoading(), LOADING);
        stage.add(buildContent(), CONTENT);
        add(stage, BorderLayout.CENTER);
        finishEntrance();
    }

    /** Only the pulsing logo, centred, while the order is being saved. */
    private JPanel buildLoading() {
        JPanel loading = new JPanel(new GridBagLayout());
        loading.setOpaque(false);
        loading.add(loadingLogo);
        return loading;
    }

    /** The real page. The order below is the order of the fade-in. */
    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        content.add(gap(36));
        content.add(fade(new JLabel(UiKit.icon("public/assets/logo/logo-horizontal-transparent.png", 426, 104))));
        content.add(gap(110));
        content.add(fade(headline));
        content.add(gap(8));
        content.add(fade(subtitle));
        content.add(gap(30));
        content.add(fade(card));
        content.add(gap(40));
        content.add(fade(new TextBlock(interLight.deriveFont(Font.PLAIN, 15f), UiColors.TEXT_DARK,
            "Thank you for your faith in Gudo’s Restobar. Enjoy", "your blessed meal!")));
        content.add(Box.createVerticalGlue());
        return content;
    }

    private static Component gap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }

    // ============================================================
    // BACKEND - loading first, then the saved order
    // ============================================================

    /** Every visit starts on the pulsing logo; the fade-in waits for the order number. */
    @Override
    protected void onShown() {
        revealTimer.stop();
        loadingStartedAt = System.nanoTime();
        stageLayout.show(stage, LOADING);
        loadingLogo.start();
    }

    @Override
    protected boolean playsOnShow() {
        return false;
    }

    /** Shows the real order number, plus how it was ordered and paid. */
    public void setOrderInfo(int orderID, String orderType, String paymentMethod) {
        Message message = Message.forPayment(paymentMethod);
        headline.setLines(message.headline);
        subtitle.setLines(message.subtitle);
        card.setOrder(orderID, label(orderType) + " • " + paymentMethod);

        long shown = (System.nanoTime() - loadingStartedAt) / 1_000_000L;
        revealTimer.setInitialDelay((int) Math.max(0, MIN_LOADING_MILLIS - shown));
        revealTimer.restart();
    }

    /** Swaps the loading logo for the page and fades it in. */
    private void reveal() {
        loadingLogo.stop();
        stageLayout.show(stage, CONTENT);
        playEntrance();
    }

    private static String label(String orderType) {
        if ("Takeout".equalsIgnoreCase(orderType)) return "Take-out";
        if ("Dine-in".equalsIgnoreCase(orderType)) return "Dine in";
        return orderType;
    }

    // ============================================================
    // BUILDING BLOCKS
    // ============================================================

    /** Centred lines of text that can be replaced later. */
    private static final class TextBlock extends JPanel {
        private final Font font;
        private final Color color;

        TextBlock(Font font, Color color, String... lines) {
            this.font = font;
            this.color = color;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setLines(lines);
        }

        void setLines(String... lines) {
            removeAll();
            for (String line : lines) {
                JLabel label = new JLabel(line);
                label.setFont(font);
                label.setForeground(color);
                label.setAlignmentX(Component.CENTER_ALIGNMENT);
                add(label);
            }
            revalidate();
            repaint();
        }
    }

    /** The orange rounded card: title on top, number in the middle, order type at the bottom. */
    private static final class OrderCard extends JPanel {
        private static final int WIDTH = 428;
        private static final int HEIGHT = 268;     // card face (264) plus its soft shadow
        private final JLabel number = new JLabel();
        private final JLabel type = new JLabel();

        OrderCard(Font bold, Font light) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(26, 0, 26, 0));
            setPreferredSize(new Dimension(WIDTH, HEIGHT));

            add(centered(new JLabel("Order Number:"), light.deriveFont(Font.PLAIN, 24f)));
            add(Box.createVerticalGlue());
            add(centered(number, bold.deriveFont(Font.BOLD, 96f)));
            add(Box.createVerticalGlue());
            add(centered(type, light.deriveFont(Font.PLAIN, 22f)));
        }

        void setOrder(int orderID, String orderType) {
            number.setText("#" + orderID);
            type.setText(orderType);
        }

        private static JLabel centered(JLabel label, Font font) {
            label.setFont(font);
            label.setForeground(UiColors.WHITE);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            return label;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 35));
            g2.fillRoundRect(0, 3, getWidth(), getHeight() - 4, 24, 24);
            g2.setColor(UiColors.ORANGE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight() - 4, 24, 24);
            g2.dispose();
        }
    }

    /** The round logo, gently growing and brightening again and again. */
    private static final class LoadingLogo extends JComponent {
        private static final int SIZE = 140;
        private static final double PULSE_SECONDS = 1.4;
        private final Icon logo = UiKit.icon("public/assets/logo/logo-circle.png", SIZE, SIZE);
        private final Timer pulse = new Timer(16, e -> tick());
        private long startedAt;
        private float wave;                        // 0..1, drives size and brightness

        LoadingLogo() {
            setPreferredSize(new Dimension(SIZE + 20, SIZE + 20));
        }

        void start() {
            startedAt = System.nanoTime();
            pulse.restart();
        }

        void stop() {
            pulse.stop();
        }

        private void tick() {
            if (!isShowing()) {                    // the page was left: stop wasting time
                pulse.stop();
                return;
            }
            double seconds = (System.nanoTime() - startedAt) / 1e9;
            wave = (float) ((1 + Math.sin(seconds * 2 * Math.PI / PULSE_SECONDS - Math.PI / 2)) / 2);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setComposite(AlphaComposite.SrcOver.derive(0.35f + 0.65f * wave));
            double scale = 0.9 + 0.1 * wave;
            g2.translate(getWidth() / 2.0, getHeight() / 2.0);
            g2.scale(scale, scale);
            logo.paintIcon(this, g2, -SIZE / 2, -SIZE / 2);
            g2.dispose();
        }
    }
}