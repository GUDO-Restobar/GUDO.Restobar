package ui.manager;

import java.awt.*;
import javax.swing.*;
import java.util.*;

public class mainView extends JFrame{

    private final static Color MAINWHITE = new Color(246, 246, 246);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screenPanel = new JPanel(cardLayout);

    public mainView() {
        setTitle("Gudo Restobar Manager");
        setSize(1920, 1080);
        getContentPane().setBackground(MAINWHITE);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        add(sideBar());
        add(viewRecords());
    }

    public static JPanel viewRecords() {
        JPanel view = new JPanel();    
            view.setBackground(Color.decode("#200E08"));
            view.setOpaque(false);
            view.setSize(1445, 1080);
        
        JPanel orderHistory = new JPanel();
            orderHistory.setBackground(MAINWHITE);
            orderHistory.setSize(1349, 769);
            orderHistory.set;

        return view;
    }
    public static JPanel sideBar() {
        JPanel sidebar = new JPanel();

        sidebar.setBackground(Color.decode("#EC5E02"));
        sidebar.setSize(475, 1080);

        return sidebar;
    }
}