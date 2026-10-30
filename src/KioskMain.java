import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import data.OrderDAO;
import ui.kiosk.*;
import util.*;

/**
 * The kiosk window: owns the screen stack (CardLayout), the cart, and all
 * navigation between screens. The actual landing screen is {@link ui.kiosk.LandingPage}.
 */
public class KioskMain extends JFrame {

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
    private MenuPage menuPage;
    private AddOrder addOrderPage;
    private OrderAdded orderAddedPage;
    private EditOrder editOrderPage;
    private OrderEdited orderEditedPage;

    public KioskMain() {

        setTitle("Gudo Restobar");
        setSize(580, 1040);
        getContentPane().setBackground(new Color(246, 246, 246));
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        screenPanel.add(new LandingPage(() -> showScreen(ORDER_OPTION_SCREEN)), LANDING_SCREEN);
        screenPanel.add(new OrderOption(), ORDER_OPTION_SCREEN);

        menuPage = new MenuPage(
            product -> showAddOrder(product, false),
            () -> showScreen(CHECKOUT_SCREEN),
            () -> showScreen(START_OVER_SCREEN)
        );
        screenPanel.add(menuPage, MENU_SCREEN);
        // Pick up stock changes made in the database (e.g. by the manager) every time the menu is shown.
        menuPage.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentShown(java.awt.event.ComponentEvent e) {
                reloadStock();
                menuPage.refreshAvailability(cart);
            }
        });

        // Any change to the cart (Menu, Add Order, Checkout, Edit Order) updates the Menu total.
        // It also greys out any product whose stock is used up or fully in the cart.
        cart.addChangeListener(() -> {
            menuPage.setTotal(cart.getTotal());
            menuPage.refreshAvailability(cart);
        });
        menuPage.refreshAvailability(cart); // products that start out of stock

        screenPanel.add(new CheckoutPage(
            cart,
            () -> showScreen(MENU_SCREEN),
            () -> showScreen(START_OVER_SCREEN),
            () -> {
                // Nothing to pay for if the order is empty.
                if (!cart.getItems().isEmpty()) {
                    showScreen(PAYMENT_OPTION_SCREEN);
                }
            },
            this::showEditOrder
        ), CHECKOUT_SCREEN);
        screenPanel.add(new StartOver(
            () -> {
                cart.clear();
                menuPage.setTotal(cart.getTotal());
                showScreen(LANDING_SCREEN);
            },
            () -> showScreen(MENU_SCREEN)
        ), START_OVER_SCREEN);

        screenPanel.add(new PaymentOption(), PAYMENT_OPTION_SCREEN);

        OrderReceipt receipt = new OrderReceipt();
        screenPanel.add(receipt, RECEIPT_SCREEN);
        // After the receipt is shown for a while, reset the kiosk for the next customer.
        Timer receiptTimer = new Timer(10000, e -> {
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
            @Override public void componentHidden(java.awt.event.ComponentEvent e) { receiptTimer.stop(); }
        });
        setContentPane(screenPanel);
        showScreen(LANDING_SCREEN);
    }

    /**
     * Saves the order (and reduces the stock) in MySQL.
     * @return true if it was saved; false if the customer was sent back to Checkout
     */
    private boolean placeOrder(OrderReceipt receipt) {
        try {
            int orderID = OrderDAO.saveOrder(
                cart, OrderSession.getOrderType(), OrderSession.getPaymentMethod());
            OrderSession.setOrderID(orderID);
            receipt.setOrderInfo(orderID, OrderSession.getOrderType(), OrderSession.getPaymentMethod());
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

    private void orderFailed(String message) {
        reloadStock();
        cart.trimToStock();
        menuPage.refreshAvailability(cart);
        JOptionPane.showMessageDialog(this, message, "Order not placed", JOptionPane.WARNING_MESSAGE);
        showScreen(CHECKOUT_SCREEN);
    }

    /**
     * Every page change goes through here. The kiosk does not update live: it picks up
     * stock changes made in the database (by the manager or another kiosk) only when
     * the customer moves to another page.
     */
    private void showScreen(String screen) {
        if (!RECEIPT_SCREEN.equals(screen)        // the receipt saves the order itself
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

    private void reloadStock() {
        try {
            Inventory.refresh();
        } catch (SQLException e) {
            e.printStackTrace(); // keep using the last known stock
        }
    }

    private void showAddOrder(Product product, boolean editMode) {
        reloadStock(); // the quantity limits are read from the stock when the page is built
        if (addOrderPage != null) screenPanel.remove(addOrderPage);

        addOrderPage = new AddOrder(
            product,
            cart,
            editMode,
            () -> {
                menuPage.setTotal(cart.getTotal());
                showOrderAdded(editMode);
            },
            () -> showScreen(editMode ? CHECKOUT_SCREEN : MENU_SCREEN)
        );
        screenPanel.add(addOrderPage, ADD_ORDER_SCREEN);
        showScreen(ADD_ORDER_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }

    private void showOrderAdded(boolean edited) {
        if (orderAddedPage != null) screenPanel.remove(orderAddedPage);

        orderAddedPage = new OrderAdded(
            cart.getTotal(),
            edited,
            () -> showScreen(MENU_SCREEN)
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

    /** Shows "Your Cravings has Been Modified" for 2 seconds, then returns to Checkout. */
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
