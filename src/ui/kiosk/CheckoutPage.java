package ui.kiosk;

import java.awt.*;
import javax.swing.*;
import util.*;

public class CheckoutPage extends JPanel{

    private final Cart cart;

    private final JPanel itemsPanel = new JPanel();
    private final JLabel totalLabel = new JLabel();

    public CheckoutPage(Cart cart) {
        
        this.cart = cart;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel("Your Order");
        add(heading, BorderLayout.NORTH);

    }
}
