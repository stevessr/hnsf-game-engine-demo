package lib.editor;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import lib.game.GameWorld;
import lib.object.GameObject;
import lib.object.GameObjectFactory;
import lib.object.GameObjectType;
import lib.object.dto.ObjectData;

public final class MapEditorController {
    private final GameWorld world;
    private final EditorGamePanel panel;
    private final EditorOverlay overlay;
    private GameObjectType selectedType;
    private GameObject selectedObject;
    private boolean gridSnap = true;
    private int gridSize = 20;
    private Point dragOffset;
    private final Deque<EditorCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditorCommand> redoStack = new ArrayDeque<>();

    public MapEditorController(GameWorld world, EditorGamePanel panel, EditorOverlay overlay) {
        this.world = Objects.requireNonNull(world, "world must not be null");
        this.panel = Objects.requireNonNull(panel, "panel must not be null");
        this.overlay = Objects.requireNonNull(overlay, "overlay must not be null");
        this.selectedType = GameObjectType.SCENE;
    }

    public void bind() {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleMousePressed(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                dragOffset = null;
            }
        });
        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                handleMouseDragged(event);
            }
        });
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKeyPressed(event);
            }
        });
    }

    public void setSelectedType(GameObjectType type) {
        if (type == null) {
            return;
        }
        this.selectedType = type;
    }

    public GameObjectType getSelectedType() {
        return selectedType;
    }

    public void setGridSnap(boolean gridSnap) {
        this.gridSnap = gridSnap;
    }

    public boolean isGridSnap() {
        return gridSnap;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = Math.max(4, gridSize);
        overlay.setGridSize(this.gridSize);
    }

    public int getGridSize() {
        return gridSize;
    }

    public GameObject getSelectedObject() {
        return selectedObject;
    }

    public void setSelectedObject(GameObject selectedObject) {
        this.selectedObject = selectedObject;
    }

    public void deleteSelected() {
        if (selectedObject == null) {
            return;
        }
        GameObject removed = selectedObject;
        world.removeObject(removed);
        selectedObject = null;
        pushCommand(new RemoveCommand(removed));
        panel.repaint();
    }

    public void toggleGrid() {
        overlay.setShowGrid(!overlay.isShowGrid());
        panel.repaint();
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        EditorCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        panel.repaint();
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        EditorCommand command = redoStack.pop();
        command.redo();
        undoStack.push(command);
        panel.repaint();
    }

    private void handleMousePressed(MouseEvent event) {
        panel.requestFocusInWindow();
        Point point = event.getPoint();
        GameObject hit = findObjectAt(point);
        if (hit != null) {
            selectedObject = hit;
            dragOffset = new Point(point.x - hit.getX(), point.y - hit.getY());
            return;
        }
        GameObject created = createObjectAt(point.x, point.y);
        if (created != null) {
            world.addObject(created);
            selectedObject = created;
            pushCommand(new AddCommand(created));
        }
        panel.repaint();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (selectedObject == null || dragOffset == null) {
            return;
        }
        int targetX = event.getX() - dragOffset.x;
        int targetY = event.getY() - dragOffset.y;
        if (gridSnap) {
            targetX = snap(targetX, gridSize);
            targetY = snap(targetY, gridSize);
        }
        MoveCommand command = new MoveCommand(selectedObject, selectedObject.getX(), selectedObject.getY(), targetX, targetY);
        command.redo();
        pushCommand(command);
        panel.repaint();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_DELETE) {
            deleteSelected();
            return;
        }
        if (event.isControlDown() && event.getKeyCode() == KeyEvent.VK_Z) {
            undo();
            return;
        }
        if (event.isControlDown() && event.getKeyCode() == KeyEvent.VK_Y) {
            redo();
        }
    }

    private GameObject findObjectAt(Point point) {
        for (GameObject object : world.getObjects()) {
            Rectangle bounds = new Rectangle(object.getX(), object.getY(), object.getWidth(), object.getHeight());
            if (bounds.contains(point)) {
                return object;
            }
        }
        return null;
    }

    private GameObject createObjectAt(int x, int y) {
        ObjectData data = new ObjectData();
        data.setType(selectedType);
        data.setName(selectedType.name().toLowerCase() + "-" + System.currentTimeMillis());
        data.setX(gridSnap ? snap(x, gridSize) : x);
        data.setY(gridSnap ? snap(y, gridSize) : y);
        data.setWidth(60);
        data.setHeight(40);
        data.setColor(new Color(180, 180, 200));
        if (selectedType == GameObjectType.SCENE || selectedType == GameObjectType.WALL || selectedType == GameObjectType.BOUNDARY) {
            data.setSolid(true);
            data.setBackground(false);
        }
        GameObject object = GameObjectFactory.fromObjectData(data);
        return object;
    }

    private int snap(int value, int grid) {
        if (grid <= 0) {
            return value;
        }
        return Math.round(value / (float) grid) * grid;
    }

    private void pushCommand(EditorCommand command) {
        if (command == null) {
            return;
        }
        undoStack.push(command);
        redoStack.clear();
    }

    private interface EditorCommand {
        void undo();

        void redo();
    }

    private final class AddCommand implements EditorCommand {
        private final GameObject object;

        private AddCommand(GameObject object) {
            this.object = object;
        }

        @Override
        public void undo() {
            world.removeObject(object);
        }

        @Override
        public void redo() {
            world.addObject(object);
        }
    }

    private final class RemoveCommand implements EditorCommand {
        private final GameObject object;

        private RemoveCommand(GameObject object) {
            this.object = object;
        }

        @Override
        public void undo() {
            world.addObject(object);
        }

        @Override
        public void redo() {
            world.removeObject(object);
        }
    }

    private final class MoveCommand implements EditorCommand {
        private final GameObject object;
        private final int fromX;
        private final int fromY;
        private final int toX;
        private final int toY;

        private MoveCommand(GameObject object, int fromX, int fromY, int toX, int toY) {
            this.object = object;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        @Override
        public void undo() {
            object.setPosition(fromX, fromY);
        }

        @Override
        public void redo() {
            object.setPosition(toX, toY);
        }
    }
}
