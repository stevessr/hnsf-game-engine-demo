package lib.object;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import org.junit.jupiter.api.Test;

public class ColorUtilsTest {

    @Test
    public void testComplement() {
        Color color = new Color(100, 150, 200);
        Color complement = ColorUtils.complement(color);
        
        assertEquals(255 - 100, complement.getRed());
        assertEquals(255 - 150, complement.getGreen());
        assertEquals(255 - 200, complement.getBlue());
        
        // Null safety
        assertEquals(Color.WHITE, ColorUtils.complement(null));
    }

    @Test
    public void testIsComplementary() {
        // Red (0) and Cyan (~0.5)
        Color red = Color.RED;
        Color cyan = Color.CYAN;
        
        assertTrue(ColorUtils.isComplementary(red, cyan));
        
        // Red and Blue (not opposite in HSB directly as 0.5 delta)
        // Red H=0, Blue H=0.66
        assertFalse(ColorUtils.isComplementary(red, Color.BLUE));
        
        // Low saturation should return false
        Color gray1 = new Color(128, 128, 128);
        Color gray2 = new Color(127, 127, 127);
        assertFalse(ColorUtils.isComplementary(gray1, gray2));
    }
}
