import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import data.OrderDAO;
import ui.kiosk.*;
import util.*;

/**
 * The kiosk window.
 *
 * It owns the screen stack (CardLayout), the cart and every navigation
 * decision: each page simply reports what the customer did and this class
 * decides which page comes next. It is also the only place in the project
 * where the window dimensions are set.
 */
public class KioskMain extends JFrame {

    // ============================================================
    // WINDOW SETUP - size, screen keys and construction
    // ============================================================

    /** Kiosk window size. No other class sets the window dimensions. */
    private static final int WINDOW_WIDTH = 580;
    private static final int WINDOW_HEIGHT = 1040;

    /** How long the receipt stays on screen before the kiosk resets itself. */
    private static final int RECEIPT_MILLIS = 10000;

    /** Keys used to find each page inside the CardLayout stack. */
    private static final String LANDING_SCREEN = "landing";
    private static final String ORDER_OPTION_SCREEN = "order-option";
    private static final String MENU_SCREEN = "menu_page";
    private static final String ADD_ORDER_SCREEN = "add-order";
    private static final String ORDER_ADDED_SCREEN = "order-added";
    private static final String EDIT_ORDER_SCREEN = "edit-order";
    private static final String ORDER_EDITED_SCREEN = "order-edited";
    private static final String CHECKOUT_SCREEN = "checkout";
    private static final String START_OVER_SCREEN = "start-over";
    private static final String PAYMENT_OPTION_SCREEN = "payment-option";
    private static final String RECEIPT_SCREEN = "receipt";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screenPanel = new JPanel(cardLayout);
    private final Cart cart = new Cart();

    /** The page on screen right now, and the one that was shown before it. */
    private String currentScreen = LANDING_SCREEN;
    private String previousScreen = LANDING_SCREEN;

    // Pages that are rebuilt whenever their content changes.
    private MenuPage menuPage;
    private AddOrder addOrderPage;
    private OrderAdded orderAddedPage;
    private EditOrder editOrderPage;
    private OrderEdited orderEditedPage;

    public KioskMain() {
        setupWindow();
        buildScreens();
        setContentPane(screenPanel);
        showScreen(LANDING_SCREEN);
    }

    /** Title, size, position and close behaviour of the kiosk window. */
    private void setupWindow() {
        setTitle("Gudo Restobar");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        getContentPane().setBackground(UiColors.BACKGROUND);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    // ============================================================
    // PAGE / NAVIGATION MANAGEMENT - building pages and switching
    // ============================================================

    /** Creates every page once and drops it into the CardLayout stack. */
    private void buildScreens() {

        // --- Entry point: the landing banner ---
        screenPanel.add(new LandingPage(() -> showScreen(ORDER_OPTION_SCREEN)), LANDING_SCREEN);

        // --- Order type: Dine-in or Takeout ---
        screenPanel.add(new OrderOption(
            () -> showScreen(MENU_SCREEN),       // a choice was made
            () -> showScreen(LANDING_SCREEN)     // back
        ), ORDER_OPTION_SCREEN);

        // --- Menu: the hub every other order page returns to ---
        menuPage = new MenuPage(
            product -> showAddOrder(product),    // an item was tapped
            () -> showScreen(CHECKOUT_SCREEN),   // Proceed to Checkout
            () -> showScreen(START_OVER_SCREEN)  // Start Over
        );
        screenPanel.add(menuPage, MENU_SCREEN);

        // The menu is the only page that lives long enough to notice stock
        // changes made elsewhere (by the manager or another kiosk).
        menuPage.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                reloadStock();
                menuPage.refreshAvailability(cart);
            }
        });

        // Any change to the cart (Menu, Add Order, Checkout, Edit Order) updates
        // the menu total and greys out anything that is sold out or fully taken.
        cart.addChangeListener(() -> {
            menuPage.setTotal(cart.getTotal());
            menuPage.refreshAvailability(cart);
        });
        menuPage.refreshAvailability(cart); // products that start out of stock

        // --- Checkout: review the order before paying ---
        screenPanel.add(new CheckoutPage(
            cart,
            () -> showScreen(MENU_SCREEN),        // "Order More"
            () -> showScreen(START_OVER_SCREEN),  // "Start Over"
            () -> {                               // "Complete Order"
                // Nothing to pay for if the order is empty.
                if (!cart.getItems().isEmpty()) {
                    showScreen(PAYMENT_OPTION_SCREEN);
                }
            },
            this::showEditOrder                   // "Edit Meal" on a card
        ), CHECKOUT_SCREEN);

        // --- Start Over confirmation ---
        screenPanel.add(new StartOver(
            () -> {                               // Yes: throw the order away
                cart.clear();
                menuPage.setTotal(cart.getTotal());
                showScreen(LANDING_SCREEN);
            },
            () -> showScreen(previousScreen)      // No: back to the previous page
        ), START_OVER_SCREEN);

        // --- Payment method: Cash or Cashless ---
        screenPanel.add(new PaymentOption(
            () -> showScreen(RECEIPT_SCREEN),     // a choice was made
            () -> showScreen(CHECKOUT_SCREEN)     // back
        ), PAYMENT_OPTION_SCREEN);

        // --- Receipt: saves the order, then resets the kiosk for the next customer ---
        OrderReceipt receipt = new OrderReceipt();
        screenPanel.add(receipt, RECEIPT_SCREEN);

        Timer receiptTimer = new Timer(RECEIPT_MILLIS, e -> {
            cart.clear();
            menuPage.setTotal(cart.getTotal());
            showScreen(LANDING_SCREEN);
        });
        receiptTimer.setRepeats(false);

        receipt.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                // The receipt only stays if the order was really saved to the database.
                if (placeOrder(receipt)) {
                    receiptTimer.restart();
                }
            }
            @Override public void componentHidden(java.awt.event.ComponentEvent e) {
                receiptTimer.stop();
            }
        });
    }

    /**
     * Every page change goes through here.
     *
     * The kiosk does not update live: it picks up stock changes made in the
     * database (by the manager or another kiosk) only when the customer moves
     * to another page.
     */
    private void showScreen(String screen) {
        // Remember where we came from so the Start Over page can go back.
        if (!screen.equals(currentScreen)) {
            previousScreen = currentScreen;
            currentScreen = screen;
        }

        if (!RECEIPT_SCREEN.equals(screen)            // the receipt saves the order itself
                && !ADD_ORDER_SCREEN.equals(screen)   // these two pages were already
                && !EDIT_ORDER_SCREEN.equals(screen)) { // built from freshly loaded stock
            reloadStock();
            if (CHECKOUT_SCREEN.equals(screen) || PAYMENT_OPTION_SCREEN.equals(screen)) {
                cart.trimToStock(); // never let the customer pay for stock that is gone
            }
            menuPage.refreshAvailability(cart);
        }
        cardLayout.show(screenPanel, screen);
    }

    /** Rebuilds the Add Order page for one product and shows it. */
    private void showAddOrder(Product product) {
        reloadStock(); // the quantity limits are read from the stock when the page is built
        if (addOrderPage != null) screenPanel.remove(addOrderPage);

        addOrderPage = new AddOrder(
            product,
            cart,
            () -> { // saved: the cart changed, so refresh the total and confirm
                menuPage.setTotal(cart.getTotal());
                showOrderAdded();
            },
            () -> showScreen(MENU_SCREEN) // cancelled
        );
        screenPanel.add(addOrderPage, ADD_ORDER_SCREEN);
        showScreen(ADD_ORDER_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }

    /** Shows the "item added" confirmation. */
    private void showOrderAdded() {
        if (orderAddedPage != null) screenPanel.remove(orderAddedPage);

        orderAddedPage = new OrderAdded(
            cart.getTotal(),
            () -> showScreen(MENU_SCREEN),     // keep ordering
            () -> showScreen(CHECKOUT_SCREEN)  // or pay now
        );
        screenPanel.add(orderAddedPage, ORDER_ADDED_SCREEN);
        showScreen(ORDER_ADDED_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }

    /** Opens the Edit Order page for a meal that is already in the order. */
    private void showEditOrder(Product product) {
        reloadStock();
        if (editOrderPage != null) screenPanel.remove(editOrderPage);

        editOrderPage = new EditOrder(
            product,
            cart,
            this::showOrderEdited,
            () -> showScreen(CHECKOUT_SCREEN)
        );
        screenPanel.add(editOrderPage, EDIT_ORDER_SCREEN);
        showScreen(EDIT_ORDER_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }

    /** Shows "Your Cravings has Been Modified", then returns to Checkout. */
    private void showOrderEdited() {
        if (orderEditedPage != null) screenPanel.remove(orderEditedPage);

        orderEditedPage = new OrderEdited(
            cart.getTotal(),
            () -> showScreen(CHECKOUT_SCREEN)
        );
        screenPanel.add(orderEditedPage, ORDER_EDITED_SCREEN);
        showScreen(ORDER_EDITED_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }

    // ============================================================
    // WORKFLOW CONTROL - saving the order, stock and startup
    // ============================================================

    /**
     * Saves the order (and reduces the stock) in MySQL.
     * @return true if it was saved; false if the customer was sent back to Checkout
     */
    private boolean placeOrder(OrderReceipt receipt) {
        OrderSession session = OrderSession.getInstance();
        try {
            int orderID = OrderDAO.saveOrder(
                cart, session.getOrderType(), session.getPaymentMethod());
            session.setOrderID(orderID);
            receipt.setOrderInfo(orderID, session.getOrderType(), session.getPaymentMethod());
            reloadStock();
            menuPage.refreshAvailability(cart);
            return true;
        } catch (OrderDAO.OutOfStockException e) {
            orderFailed("Sorry, " + e.getItemName() + " just ran out of stock.\n"
                + "Your order was adjusted to what is still available. Please check it again.");
        } catch (SQLException e) {
            e.printStackTrace();
            orderFailed("Sorry, we couldn't save your order right now.\nPlease try again.");
        }
        return false;
    }

    /** Puts the customer back on Checkout with an order that matches the real stock. */
    private void orderFailed(String message) {
        reloadStock();
        cart.trimToStock();
        menuPage.refreshAvailability(cart);
        JOptionPane.showMessageDialog(this, message, "Order not placed", JOptionPane.WARNING_MESSAGE);
        showScreen(CHECKOUT_SCREEN);
    }

    /** Reloads the stock of every product from the database. */
    private void reloadStock() {
        try {
            Inventory.refresh();
        } catch (SQLException e) {
            e.printStackTrace(); // keep using the last known stock
        }
    }

    /** Boots the kiosk. A failed database connection stops the app with a clear message. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            try {
                Inventory.refresh();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "The kiosk could not connect to the database.\n"
                        + "Make sure MySQL is running and the login in data/dbconnect.java is correct.\n\n"
                        + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            KioskMain kiosk = new KioskMain();
            kiosk.setVisible(true);
        });
    }
}
