import ui.manager.mainView;
import javax.swing.*;

public class ManagerMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            mainView manager = new mainView();
            manager.setVisible(true);
        });
    }

}
