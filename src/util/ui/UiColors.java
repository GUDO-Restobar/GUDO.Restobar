package util.ui;

import java.awt.Color;

public final class UiColors {

    private UiColors() {
        // static-only class
    }

    public static final Color BACKGROUND = new Color(246, 246, 246);    // MAIN WHITE #F6F6F6 
    public static final Color SURFACE = BACKGROUND;                     // MAIN WHITE #F6F6F6
    public static final Color ORANGE = new Color(236, 94, 2);           // MAIN ORANGE #EC5E02
    public static final Color ORANGE_HOVER = new Color(201, 79, 0);     // ACCENT TEXT #C94F00
    public static final Color ORANGE_DARK = new Color(201, 79, 0);      // ACCENT TEXT #C94F00

    public static final Color ORANGE_TINT = mix(BACKGROUND, ORANGE, 0.10); // Light MAIN ORANGE tint: 10% MAIN ORANGE over MAIN WHITE.

    // TEXT COLOURS
    public static final Color BROWN = new Color(92, 74, 65);            // SECONDARY TEXT #5C4A41
    public static final Color TEXT_DARK = new Color(32, 14, 8);         // PRIMARY TEXT #200E08
    public static final Color TEXT_MUTED = BROWN;                       // SECONDARY TEXT #5C4A41
    public static final Color TEXT_HINT = new Color(138, 118, 108);     // MUTED TEXT #8A766C
    public static final Color WHITE = BACKGROUND;                       // MAIN WHITE #F6F6F6
    public static final Color BLACK = TEXT_DARK;                        // PRIMARY TEXT #200E08 

    public static final Color SELECTION = ORANGE_TINT;                      // Row selection: the light MAIN ORANGE tint over MAIN WHITE.

    // DISABLED CONTROLS
    public static final Color DISABLED = new Color(184, 169, 160);      // DISABLED TEXT #B8A9A0

    // BORDERS, DIVIDERS AND OUTLINES - SECONDARY TEXT 
    public static final Color BORDER = BROWN;                           // SECONDARY TEXT #5C4A41
    public static final Color BORDER_MID = BROWN;                       // SECONDARY TEXT #5C4A41
    public static final Color BORDER_CARD = BROWN;                      // SECONDARY TEXT #5C4A41
    public static final Color BORDER_BUTTON = BROWN;                    // SECONDARY TEXT #5C4A41
    public static final Color BORDER_FIELD = BROWN;                     // SECONDARY TEXT #5C4A41
    public static final Color BORDER_FIELD_SOFT = BROWN;                // SECONDARY TEXT #5C4A41
    public static final Color BORDER_ROW = BROWN;                       // SECONDARY TEXT #5C4A41

    // HOVER FILLS - light MAIN ORANGE tint (10% over MAIN WHITE)
    /* Fill of a light button while it is hovered. */
    public static final Color HOVER_GREY = ORANGE_TINT;                 // light MAIN ORANGE tint (10% over MAIN WHITE)
    public static final Color HOVER_GREY_SOFT = ORANGE_TINT;            // light MAIN ORANGE tint (10% over MAIN WHITE)
    public static final Color HOVER_GREY_PALE = ORANGE_TINT;            // light MAIN ORANGE tint (10% over MAIN WHITE)

    // STATUS COLOURS
    public static final Color DANGER = new Color(198, 40, 40);          // RED STATUS #C62828
    public static final Color SUCCESS = new Color(79, 138, 60);         // GREEN STATUS #4F8A3C
    public static final Color YELLOW = new Color(242, 169, 0);          //  YELLOW STATUS #F2A900

    // =====================================================================
    // Translucent helpers - shadows and overlays are drawn with alpha
    // =====================================================================

    //drop-shadow colour
    public static Color shadow(int alpha) {
        return new Color(BLACK.getRed(), BLACK.getGreen(), BLACK.getBlue(), alpha);
    }

    /*
     The given colour at a different opacity - used for the milky
      "unavailable" veil over a sold-out menu item.
    */
    public static Color overlay(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }

    // Blends two palette colours
    private static Color mix(Color a, Color b, double t) {
        return new Color(
            (int) Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }
}
