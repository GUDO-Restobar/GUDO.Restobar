package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

/**
 * One category button in the menu's orange sidebar (Meals, Snacks, Drinks).
 *
 * Main purpose: reusable UI component - a white rounded card with a picture
 * and a caption that runs the given action when it is clicked. Used by
 * MenuPage to scroll the product list to a section.
 *
 * When its section is the one the customer is looking at, the card is
 * "active": it smoothly grows a little wider (ease-out animation), so the
 * sidebar always shows where you are.
 */
public class CategoryTab extends JPanel {
    private static final int WIDTH = 104;
    private static final int HEIGHT = 142;
    /** Extra width added while this tab is the active category. */
    private static final int EXPAND = 18;
    private static final int ARC = 12;

    /** True while the customer is looking at this tab's section. */
    private boolean active;
    /** 0 = normal size, 1 = fully expanded (animated in small steps). */
    private double grow;

    private Timer animation;

    /** 0 = invisible, 1 = fully visible (the menu's entrance fade). */
    private float fade = 1f;

    /** @param onClick runs when the customer taps the tab */
    public CategoryTab(String label, String imagePath, Font font, Runnable onClick) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));

        ImageIcon icon = UiKit.icon(imagePath, 88, 88);
        add(new JLabel(icon, SwingConstants.CENTER), BorderLayout.CENTER);

        JLabel name = new JLabel(label, SwingConstants.CENTER);
        name.setFont(font.deriveFont(12f));
        name.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        add(name, BorderLayout.SOUTH);

        MouseAdapter tap = new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e){
                onClick.run();
            }
        };
        UiKit.listenEverywhere(this, tap);   // picture and caption included
    }

    /** Shows or removes the focused state (the width animates smoothly). */
    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;

        if (animation == null) {
            animation = new Timer(15, e -> {
                double target = this.active ? 1 : 0;   // always the latest state
                grow += (target - grow) * 0.28;        // ease out toward the target
                if (Math.abs(target - grow) < 0.02) {
                    grow = target;
                    ((Timer) e.getSource()).stop();
                }
                applySize();
            });
        }
        if (!animation.isRunning()) {
            animation.start();
        }
    }

    /** Resizes the card for the current animation frame. */
    private void applySize() {
        int width = (int) Math.round(WIDTH + EXPAND * grow);
        setPreferredSize(new Dimension(width, HEIGHT));
        setMaximumSize(new Dimension(width, HEIGHT));
        revalidate();
        repaint();
    }

    /** Sets the opacity and repaints (used by the menu's entrance fade). */
    public void setFade(float fade) {
        fade = Math.max(0f, Math.min(1f, fade));
        if (fade == this.fade
                || (fade != 0f && fade != 1f && Math.abs(this.fade - fade) < 0.01f)) {
            return;                        // nothing visible would change
        }
        this.fade = fade;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = UiKit.translucent(g, fade);
        if (g2 == null) {
            super.paint(g);
            return;
        }
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(UiColors.SURFACE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);

        g2.dispose();
    }
}
