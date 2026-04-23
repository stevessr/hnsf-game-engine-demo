package lib.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.junit.jupiter.api.Test;

import lib.game.GameWorld;
import lib.object.GameObjectType;

class MapEditorControllerTest {
    @Test
    void shouldDefaultToSelectModeAndIgnoreBlankClickCreation() {
        GameWorld world = new GameWorld(400, 300);
        EditorOverlay overlay = new EditorOverlay();
        EditorGamePanel panel = new EditorGamePanel(world, overlay);
        panel.setSize(400, 300);
        MapEditorController controller = new MapEditorController(world, panel, overlay);

        controller.bind();

        assertEquals(MapEditorController.EditMode.SELECT, controller.getEditMode(), "编辑器默认应为选择模式");
        MouseEvent click = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 120, 90, 1, false);
        for (MouseListener listener : panel.getMouseListeners()) {
            listener.mousePressed(click);
        }

        assertTrue(world.getObjects().isEmpty(), "选择模式下点击空白区域不应自动创建对象");
        assertNull(controller.getSelectedObject(), "空白点击应保持未选中状态");
    }

    @Test
    void addObjectOnceShouldCreateExactlyOneObject() {
        GameWorld world = new GameWorld(400, 300);
        EditorOverlay overlay = new EditorOverlay();
        EditorGamePanel panel = new EditorGamePanel(world, overlay);
        panel.setSize(400, 300);
        MapEditorController controller = new MapEditorController(world, panel, overlay);

        controller.bind();
        var created = controller.addObjectOnce(GameObjectType.TRIGGER);

        assertNotNull(created, "添加按钮应成功创建对象");
        assertEquals(1, world.getObjects().size(), "单次添加应只创建一个对象");
        assertEquals(GameObjectType.TRIGGER, world.getObjects().get(0).getType());
        assertEquals(created, controller.getSelectedObject(), "单次添加后应选中新对象便于编辑");
    }
}
