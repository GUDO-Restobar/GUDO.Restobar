package util.ui;

import java.awt.Font;
import javax.swing.JLabel;

/**
 * Creates the small set of widgets that give a window its look.
 *
 * Abstract Factory pattern: {@link Kiosk} and {@link Manager} are two
 * concrete factories. A screen asks for a widget without knowing which
 * window it belongs to, so the kiosk and the manager can style the same
 * kind of widget differently while the pages stay unchanged.
 *
 * Factory Method pattern: {@link #heading(String)} is the factory method;
 * each concrete factory decides which font size and colour the heading gets.
 */
public interface UiFactory {

    /** One of the fonts shipped in public/assets/fonts, already cached by {@link UiKit}. */
    Font BOLD = UiKit.font("Montserrat/static/Montserrat-Bold.ttf");

    /** A page heading in this window's style. */
    JLabel heading(String text);

    /** The factory for the customer kiosk. */
    static UiFactory kiosk() {
        return Kiosk.INSTANCE;
    }

    /** The factory for the manager window. */
    static UiFactory manager() {
        return Manager.INSTANCE;
    }

    /** Kiosk widgets: the orange headings used above the kiosk content. */
    final class Kiosk implements UiFactory {
        private static final Kiosk INSTANCE = new Kiosk();

        private Kiosk() {}

        @Override
        public JLabel heading(String text) {
            JLabel label = new JLabel(text);
            label.setFont(BOLD.deriveFont(29f));
            label.setForeground(UiColors.ORANGE);
            return label;
        }
    }

    /** Manager widgets: the slightly larger orange headings used by the manager tabs. */
    final class Manager implements UiFactory {
        private static final Manager INSTANCE = new Manager();

        private Manager() {}

        @Override
        public JLabel heading(String text) {
            JLabel label = new JLabel(text);
            label.setFont(BOLD.deriveFont(33f));
            label.setForeground(UiColors.ORANGE);
            return label;
        }
    }
}
