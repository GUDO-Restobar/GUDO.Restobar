package ui.kiosk;

import data.OrderDAO;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import util.*;


public class LandingPage extends JFrame {
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

    public LandingPage() {

        setTitle("Gudo Restobar");
        setSize(580, 1040);
        getContentPane().setBackground(new Color(246, 246, 246));
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel landingPanel = new JPanel(new BorderLayout());
        landingPanel.setBackground(new Color(246, 246, 246));

        JLabel bannerLabel = new FadingBanner(
            new String[] {
                "public/assets/banners/landing-page-1.png",
                "public/assets/banners/landing-page-2.png",
                "public/assets/banners/landing-page-3.png",
            }, 580, 676);

        ImageIcon LogoImage = new ImageIcon("public/assets/logo/logo-horizontal-transparent.png");
        Image logoImage = LogoImage.getImage().getScaledInstance(500, 121, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(logoImage));

        Font interExtraBold;
        Font interLight;

        try {
            interExtraBold = FontLoader.loadFont(
            "public/assets/fonts/Inter/static/Inter_28pt-ExtraBold.ttf"
            );
            interLight = FontLoader.loadFont(
            "public/assets/fonts/Inter/static/Inter_18pt-Light.ttf"
            );
        } catch (java.io.IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load the button fonts.", e);
        }

        ImageIcon buttonIcon = new ImageIcon("public/assets/icons/touch.png");
        Image scaledIcon = buttonIcon.getImage()
            .getScaledInstance(96, 96, Image.SCALE_SMOOTH);

        JButton startBtn = new JButton(
            "<html><center>"
                + "<font face='" + interExtraBold.getFontName() +"' size='7'>Start Order</font><br>"
                + "<font face='" + interLight.getFontName() +"' size='5'>To unlock Gudo's Flavor</font>"
                + "</center></html>",
            new ImageIcon(scaledIcon)
        ) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );

                int shadowSize = 8;
                int cornerRadius = 10;
                int buttonHeight = getHeight() - shadowSize;

                // Layered translucent shapes create a soft-shadow effect.
                for (int offset = shadowSize; offset > 0; offset--) {
                    int alpha = 4 + (shadowSize - offset) * 3;
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.fillRoundRect(
                        0, offset, getWidth(), buttonHeight,
                        cornerRadius, cornerRadius
                    );
                }

                g2.setColor(getBackground());
                g2.fillRoundRect(
                    0, 0, getWidth(), buttonHeight,
                    cornerRadius, cornerRadius
                );

                g2.dispose();
                super.paintComponent(g);
            }
        };

        Color buttonColor = new Color(236, 94, 2);
        Color hoverColor = new Color(255, 120, 35);

        startBtn.setBackground(buttonColor);
        startBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                startBtn.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                startBtn.setBackground(buttonColor);
            }
        });

        startBtn.setForeground(new Color(246, 246, 246));

        startBtn.setPreferredSize(new Dimension(500, 140));
        startBtn.setIconTextGap(12);
        startBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        startBtn.setMargin(new Insets(0, 0, 0, 30));
        startBtn.setOpaque(false);
        startBtn.setContentAreaFilled(false);
        startBtn.setBorderPainted(false);
        startBtn.setFocusPainted(false);
        startBtn.addActionListener(e -> cardLayout.show(screenPanel, ORDER_OPTION_SCREEN));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setBackground(new Color(246, 246, 246));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 30, 12));
        buttonPanel.add(startBtn);

        landingPanel.add(bannerLabel, BorderLayout.NORTH);
        landingPanel.add(logoLabel, BorderLayout.CENTER);
        landingPanel.add(buttonPanel, BorderLayout.SOUTH);

        screenPanel.add(landingPanel, LANDING_SCREEN);
        screenPanel.add(new OrderOption(), ORDER_OPTION_SCREEN);

        menuPage = new MenuPage(
            product -> showAddOrder(product, false),
            () -> cardLayout.show(screenPanel, CHECKOUT_SCREEN),
            () -> cardLayout.show(screenPanel, START_OVER_SCREEN)
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
            () -> cardLayout.show(screenPanel, MENU_SCREEN),
            () -> cardLayout.show(screenPanel, START_OVER_SCREEN),
            () -> {
                // Nothing to pay for if the order is empty.
                if (!cart.getItems().isEmpty()) {
                    cardLayout.show(screenPanel, PAYMENT_OPTION_SCREEN);
                }
            },
            this::showEditOrder
        ), CHECKOUT_SCREEN);
        screenPanel.add(new StartOver(
            () -> {
                cart.clear();
                menuPage.setTotal(cart.getTotal());
                cardLayout.show(screenPanel, LANDING_SCREEN);
            },
            () -> cardLayout.show(screenPanel, MENU_SCREEN)
        ), START_OVER_SCREEN);

        screenPanel.add(new PaymentOption(), PAYMENT_OPTION_SCREEN);

        OrderReceipt receipt = new OrderReceipt();
        screenPanel.add(receipt, RECEIPT_SCREEN);
        // After the receipt is shown for a while, reset the kiosk for the next customer.
        Timer receiptTimer = new Timer(10000, e -> {
            cart.clear();
            menuPage.setTotal(cart.getTotal());
            cardLayout.show(screenPanel, LANDING_SCREEN);
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
        cardLayout.show(screenPanel, LANDING_SCREEN);
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
        cardLayout.show(screenPanel, CHECKOUT_SCREEN);
    }

    private void reloadStock() {
        try {
            Inventory.refresh();
        } catch (SQLException e) {
            e.printStackTrace(); // keep using the last known stock
        }
    }

    private void showAddOrder(Product product, boolean editMode) {
        if (addOrderPage != null) screenPanel.remove(addOrderPage);

        addOrderPage = new AddOrder(
            product,
            cart,
            editMode,
            () -> {
                menuPage.setTotal(cart.getTotal());
                showOrderAdded(editMode);
            },
            () -> cardLayout.show(screenPanel, editMode ? CHECKOUT_SCREEN : MENU_SCREEN)
        );
        screenPanel.add(addOrderPage, ADD_ORDER_SCREEN);
        cardLayout.show(screenPanel, ADD_ORDER_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }

    private void showOrderAdded(boolean edited) {
        if (orderAddedPage != null) screenPanel.remove(orderAddedPage);

        orderAddedPage = new OrderAdded(
            cart.getTotal(),
            edited,
            () -> cardLayout.show(screenPanel, MENU_SCREEN)
        );
        screenPanel.add(orderAddedPage, ORDER_ADDED_SCREEN);
        cardLayout.show(screenPanel, ORDER_ADDED_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }

    /** Opens the Edit Order page for a meal that is already in the order. */
    private void showEditOrder(Product product) {
        if (editOrderPage != null) screenPanel.remove(editOrderPage);

        editOrderPage = new EditOrder(
            product,
            cart,
            this::showOrderEdited,
            () -> cardLayout.show(screenPanel, CHECKOUT_SCREEN)
        );
        screenPanel.add(editOrderPage, EDIT_ORDER_SCREEN);
        cardLayout.show(screenPanel, EDIT_ORDER_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }

    /** Shows "Your Cravings has Been Modified" for 2 seconds, then returns to Checkout. */
    private void showOrderEdited() {
        if (orderEditedPage != null) screenPanel.remove(orderEditedPage);

        orderEditedPage = new OrderEdited(
            cart.getTotal(),
            () -> cardLayout.show(screenPanel, CHECKOUT_SCREEN)
        );
        screenPanel.add(orderEditedPage, ORDER_EDITED_SCREEN);
        cardLayout.show(screenPanel, ORDER_EDITED_SCREEN);
        screenPanel.revalidate();
        screenPanel.repaint();
    }
}
