package ui.manager;

import java.awt.*;
import java.io.IOException;
import javax.swing.ImageIcon;
import util.FontLoader;

/** Colors, fonts and the logo shared by the manager screens. */
public final class ManagerTheme {
    public static final Color MAINWHITE = new Color(246, 246, 246);
    public static final Color MAINORANGE = Color.decode("#EC5E02");
    public static final ImageIcon LOGO = new ImageIcon("public\\assets\\logo\\logo-circle.png");
    public static final Font BOLD = loadFont("Montserrat/static/Montserrat-Bold.ttf");
    public static final Font REGULAR = loadFont("Montserrat/static/Montserrat-Regular.ttf");

    private ManagerTheme() {}

    public static Font loadFont(String file) {
        try {
            return FontLoader.loadFont("public/assets/fonts/" + file);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("Could not load font: " + file, e);
        }
    }
}