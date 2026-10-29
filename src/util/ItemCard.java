package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.*;

public class ItemCard extends JPanel {

    private static final int WIDTH = 188;
    private static final int HEIGHT = 221;

    public ItemCard(Product product, Font font, Consumer<Product> onSelect) {
        setLayout(new BorderLayout());
        setBackground(new Color(246, 246, 246));
        setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        add(createPicture(product), BorderLayout.CENTER);
        add(createTextArea(product, font), BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (onSelect != null) {
                    onSelect.accept(product);
                }
            }
        });
    }

    private JLabel createPicture(Product product) {
        ImageIcon icon = new ImageIcon(product.getImagePath());
        Image scaled = icon.getImage().getScaledInstance(130, 130, Image.SCALE_SMOOTH);
        return new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
    }

    private JPanel createTextArea(Product product, Font font) {
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        text.setPreferredSize(new Dimension(WIDTH, 92));

        JTextArea name = new JTextArea(product.getName());
        name.setFont(font.deriveFont(15f));
        name.setForeground(new Color(74, 45, 35));
        name.setOpaque(false);
        name.setEditable(false);
        name.setFocusable(false);
        name.setLineWrap(true);
        name.setWrapStyleWord(true);
        name.setBorder(null);
        name.setMargin(new Insets(0, 0, 0, 0));

        JLabel price = new JLabel("\u20B1" + String.format("%.2f", product.getPrice()));
        price.setFont(font.deriveFont(16f));
        price.setForeground(new Color(204, 80, 0));

        text.add(name, BorderLayout.CENTER);
        text.add(price, BorderLayout.SOUTH);
        return text;
    }
}
