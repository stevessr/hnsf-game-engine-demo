package lib.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorBoundsTest {
    @Test
    void normalizeRectShouldClampPositionIntoWorld() {
        EditorBounds.Rect rect = EditorBounds.normalizeRect(-20, -30, 80, 60, 200, 120);

        assertEquals(0, rect.x());
        assertEquals(0, rect.y());
        assertEquals(80, rect.width());
        assertEquals(60, rect.height());
    }

    @Test
    void normalizeRectShouldShrinkWhenObjectExceedsWorld() {
        EditorBounds.Rect rect = EditorBounds.normalizeRect(10, 10, 500, 300, 200, 120);

        assertEquals(0, rect.x());
        assertEquals(0, rect.y());
        assertEquals(200, rect.width());
        assertEquals(120, rect.height());
    }
}
