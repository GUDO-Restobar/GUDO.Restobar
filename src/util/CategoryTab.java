package util;
 
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class CategoryTab extends JPanel{
    private static final int WIDTH = 104;
    private static final int HEIGHT = 142;

    public CategoryTab(String label, String imagePath, Font font, Runnable onClick) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));

        ImageIcon icon = new ImageIcon(imagePath);
        Image scaled = icon.getImage().getScaledInstance(88, 88, Image.SCALE_SMOOTH);
        add(new JLabel(new ImageIcon(scaled), SwingConstants.CENTER), BorderLayout.CENTER);

        JLabel name = new JLabel(label, SwingConstants.CENTER);
        name.setFont(font.deriveFont(12f));
        name.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        add(name, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e){
                onClick.run();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

        g2.dispose();
    }
}