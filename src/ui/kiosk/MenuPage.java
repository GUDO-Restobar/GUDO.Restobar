package ui.kiosk;
 
import java.awt.*;
import javax.swing.*;
 

public class MenuPage extends JPanel {
 
    public MenuPage() {
        setLayout(new BorderLayout());
 
        add(createSidebar(), BorderLayout.WEST);
        add(createContentArea(), BorderLayout.CENTER);
    }
 
    private JPanel createSidebar() {
        JPanel sidebar = box("SIDEBAR", new Color(236, 94, 2));
        sidebar.setPreferredSize(new Dimension(130, 0));
        return sidebar;
    }
 
    private JPanel createContentArea() {
        JPanel content = new JPanel(new BorderLayout());
 
        content.add(createBanner(), BorderLayout.NORTH);
        content.add(createItemArea(), BorderLayout.CENTER);
        content.add(createBottomBar(), BorderLayout.SOUTH);
 
        return content;
    }
 
    private JPanel createBanner() {
        JPanel banner = box("BANNER", new Color(250, 200, 30));
        banner.setPreferredSize(new Dimension(0, 150));
        return banner;
    }
 
    private JPanel createItemArea() {
        return box("ITEM AREA", new Color(246, 246, 246));
    }
 
    private JPanel createBottomBar() {
        JPanel bar = box("BOTTOM BAR", Color.WHITE);
        bar.setPreferredSize(new Dimension(0, 135));
        return bar;
    }
 
    private JPanel box(String name, Color color) {
        JPanel panel = new JPanel();
        panel.setBackground(color);
        panel.add(new JLabel(name));
        return panel;
    }
}