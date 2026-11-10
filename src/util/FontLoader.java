package util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the TrueType fonts shipped in public/assets/fonts.
 *
 * Main purpose: one place that turns a file path into a usable {@link Font}
 * and registers it with Swing, so every screen can ask for the same font.
 *
 * Flyweight pattern: each font file is read once and the Font object is
 * cached. Later calls return the same shared Font, which the callers then
 * resize with deriveFont() (deriveFont never changes the cached one).
 */
public final class FontLoader {

    /** The fonts already loaded, by file path. */
    private static final Map<String, Font> LOADED = new HashMap<>();

    private FontLoader() {
        // static-only class
    }

    /** Reads the font file, registers it with the graphics environment and returns it. */
    public static synchronized Font loadFont(String path) throws IOException, FontFormatException {
        Font cached = LOADED.get(path);
        if (cached != null) {
            return cached;
        }
        Font font = Font.createFont(Font.TRUETYPE_FONT, new File(path));
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        LOADED.put(path, font);
        return font;
    }
}
