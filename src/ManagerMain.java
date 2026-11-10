import javax.swing.*;

import ui.manager.MainView;

/** Boots the manager window. */
public class ManagerMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainView().setVisible(true));
    }
}
