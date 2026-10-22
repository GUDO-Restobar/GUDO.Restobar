package util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

public final class FontLoader {
    private FontLoader() {
        
    }

    public static Font loadFont(String path) throws IOException, FontFormatException {
        Font font = Font.createFont(Font.TRUETYPE_FONT, new File(path));
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        return font;
    }
}