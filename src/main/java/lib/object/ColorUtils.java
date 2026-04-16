package lib.object;

import java.awt.Color;

public final class ColorUtils {
    private static final float COMPLEMENTARY_HUE_DELTA = 0.5f;
    private static final float SATURATION_THRESHOLD = 0.18f;
    private static final float TOLERANCE = 0.12f;

    private ColorUtils() {
    }

    public static Color complement(Color color) {
        if (color == null) {
            return Color.WHITE;
        }
        return new Color(
            255 - color.getRed(),
            255 - color.getGreen(),
            255 - color.getBlue(),
            color.getAlpha()
        );
    }

    public static boolean isComplementary(Color first, Color second) {
        if (first == null || second == null) {
            return false;
        }
        float[] hsbFirst = Color.RGBtoHSB(first.getRed(), first.getGreen(), first.getBlue(), null);
        float[] hsbSecond = Color.RGBtoHSB(second.getRed(), second.getGreen(), second.getBlue(), null);
        if (hsbFirst[1] < SATURATION_THRESHOLD || hsbSecond[1] < SATURATION_THRESHOLD) {
            return false;
        }
        float hueDelta = Math.abs(hsbFirst[0] - hsbSecond[0]);
        hueDelta = Math.min(hueDelta, 1.0f - hueDelta);
        return Math.abs(hueDelta - COMPLEMENTARY_HUE_DELTA) <= TOLERANCE;
    }
}
