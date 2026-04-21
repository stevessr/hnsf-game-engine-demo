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
import java.util.function.Consumer;

import lib.game.GameWorld;
import lib.object.DialogObject;
import lib.object.GameObject;
import lib.object.GameObjectFactory;
import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.object.PlayerObject;
import lib.object.VoxelObject;
import lib.object.dto.ObjectData;

public final class MapEditorController {
    public enum EditMode {
        SELECT,
        BUILD,
        ERASE
    }

    private final GameWorld world;
    private final EditorGamePanel panel;
    private final EditorOverlay overlay;
    private GameObjectType selectedType;
    private GameObject selectedObject;
    private boolean gridSnap = true;
    private int gridSize = 20;
    private int defaultFontSize = 18;
    private Color brushColor;
    private EditMode editMode = EditMode.SELECT;
    private Point dragOffset;
    private Point lastPaintPoint;
    private Consumer<GameObject> selectionListener = object -> {
    };
    private Consumer<EditMode> modeChangeListener = mode -> {
    };
    private final Deque<EditorCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditorCommand> redoStack = new ArrayDeque<>();

    public MapEditorController(GameWorld world, EditorGamePanel panel, EditorOverlay overlay) {
        this.world = Objects.requireNonNull(world, "world must not be null");
        this.panel = Objects.requireNonNull(panel, "panel must not be null");
        this.overlay = Objects.requireNonNull(overlay, "overlay must not be null");
        this.selectedType = GameObjectType.SCENE;
        overlay.setModeInfo(editMode.name());
    }

    public void setSelectionListener(Consumer<GameObject> selectionListener) {
        this.selectionListener = selectionListener == null ? object -> {
        } : selectionListener;
    }

    public void setModeChangeListener(Consumer<EditMode> modeChangeListener) {
        this.modeChangeListener = modeChangeListener == null ? mode -> {
        } : modeChangeListener;
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
                lastPaintPoint = null;
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
        notifySelectionChanged();
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        if (editMode != null) {
            this.editMode = editMode;
            dragOffset = null;
            lastPaintPoint = null;
            overlay.setModeInfo(editMode.name());
            modeChangeListener.accept(editMode);
            panel.repaint();
        }
    }

    public int getDefaultFontSize() {
        return defaultFontSize;
    }

    public void setDefaultFontSize(int defaultFontSize) {
        this.defaultFontSize = Math.max(10, Math.min(64, defaultFontSize));
    }

    public Color getBrushColor() {
        return brushColor;
    }

    public void setBrushColor(Color brushColor) {
        if (brushColor != null) {
            this.brushColor = brushColor;
        }
    }

    public void clearBrushColor() {
        this.brushColor = null;
    }

    public void deleteSelected() {
        if (selectedObject == null) {
            return;
        }
        GameObject removed = selectedObject;
        world.removeObject(removed);
        selectedObject = null;
        notifySelectionChanged();
        pushCommand(new RemoveCommand(removed));
        panel.repaint();
    }

    public void duplicateSelected() {
        if (selectedObject == null) {
            return;
        }
        ObjectData sourceData = GameObjectFactory.toObjectData(selectedObject);
        if (sourceData == null) {
            return;
        }
        int offset = gridSnap ? Math.max(1, gridSize) : 10;
        sourceData.setName(sourceData.getName() + "-copy-" + System.currentTimeMillis());
        sourceData.setX(sourceData.getX() + offset);
        sourceData.setY(sourceData.getY() + offset);
        EditorBounds.Rect normalized = EditorBounds.normalizeRect(
            sourceData.getX(),
            sourceData.getY(),
            sourceData.getWidth(),
            sourceData.getHeight(),
            world.getWidth(),
            world.getHeight()
        );
        sourceData.setX(normalized.x());
        sourceData.setY(normalized.y());
        sourceData.setWidth(normalized.width());
        sourceData.setHeight(normalized.height());
        GameObject duplicated = GameObjectFactory.fromObjectData(sourceData);
        if (duplicated == null) {
            return;
        }
        world.addObject(duplicated);
        selectedObject = duplicated;
        notifySelectionChanged();
        pushCommand(new AddCommand(duplicated));
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
        if (editMode == EditMode.ERASE) {
            eraseObjectAt(point);
            lastPaintPoint = null;
            return;
        }
        if (editMode == EditMode.BUILD) {
            paintObjectAt(point);
            return;
        }
        GameObject hit = findObjectAt(point);
        if (hit != null) {
            selectedObject = hit;
            notifySelectionChanged();
            dragOffset = new Point(point.x - hit.getX(), point.y - hit.getY());
            panel.repaint();
            return;
        }
        GameObject created = createObjectAt(point.x, point.y);
        if (created != null) {
            world.addObject(created);
            selectedObject = created;
            notifySelectionChanged();
            pushCommand(new AddCommand(created));
        }
        panel.repaint();
    }

    private void handleMouseDragged(MouseEvent event) {
        Point point = event.getPoint();
        if (editMode == EditMode.BUILD) {
            paintObjectAt(point);
            return;
        }
        if (editMode == EditMode.ERASE) {
            eraseObjectAt(point);
            return;
        }
        if (selectedObject == null || dragOffset == null) {
            return;
        }
        int targetX = point.x - dragOffset.x;
        int targetY = point.y - dragOffset.y;
        if (gridSnap) {
            targetX = snap(targetX, gridSize);
            targetY = snap(targetY, gridSize);
        }
        EditorBounds.Rect normalized = EditorBounds.normalizePosition(selectedObject, targetX, targetY, world.getWidth(), world.getHeight());
        MoveCommand command = new MoveCommand(
            selectedObject,
            selectedObject.getX(),
            selectedObject.getY(),
            normalized.x(),
            normalized.y()
        );
        command.redo();
        pushCommand(command);
        panel.repaint();
    }

    private void paintObjectAt(Point point) {
        Point snapPoint = gridSnap ? new Point(snap(point.x, gridSize), snap(point.y, gridSize)) : new Point(point);
        if (snapPoint.equals(lastPaintPoint)) {
            return;
        }
        lastPaintPoint = snapPoint;
        GameObject hit = findObjectAt(snapPoint);
        if (hit != null) {
            return;
        }
        GameObject created = createObjectAt(snapPoint.x, snapPoint.y);
        if (created == null) {
            return;
        }
        world.addObject(created);
        selectedObject = created;
        notifySelectionChanged();
        pushCommand(new AddCommand(created));
        panel.repaint();
    }

    private void eraseObjectAt(Point point) {
        GameObject hit = findObjectAt(point);
        if (hit == null) {
            return;
        }
        world.removeObject(hit);
        if (hit == selectedObject) {
            selectedObject = null;
            notifySelectionChanged();
        }
        pushCommand(new RemoveCommand(hit));
        panel.repaint();
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.isControlDown() && event.getKeyCode() == KeyEvent.VK_D) {
            duplicateSelected();
            return;
        }
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
            return;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.VK_G -> toggleGrid();
            case KeyEvent.VK_S -> setEditMode(EditMode.SELECT);
            case KeyEvent.VK_B -> setEditMode(EditMode.BUILD);
            case KeyEvent.VK_E -> setEditMode(EditMode.ERASE);
            case KeyEvent.VK_LEFT -> moveSelectedBy(-getNudgeStep(event), 0);
            case KeyEvent.VK_RIGHT -> moveSelectedBy(getNudgeStep(event), 0);
            case KeyEvent.VK_UP -> moveSelectedBy(0, -getNudgeStep(event));
            case KeyEvent.VK_DOWN -> moveSelectedBy(0, getNudgeStep(event));
            default -> {
            }
        }
    }

    private GameObject findObjectAt(Point point) {
        var objects = world.getObjects();
        for (int index = objects.size() - 1; index >= 0; index--) {
            GameObject object = objects.get(index);
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
        Color color = brushColor;
        switch (selectedType) {
            case PLAYER -> {
                data.setWidth(48);
                data.setHeight(48);
                data.setColor(color != null ? color : new Color(66, 135, 245));
            }
            case MONSTER -> {
                data.setWidth(44);
                data.setHeight(44);
                data.setColor(color != null ? color : new Color(220, 80, 80));
            }
            case ITEM -> {
                data.setWidth(28);
                data.setHeight(28);
                data.setColor(color != null ? color : new Color(255, 210, 92));
            }
            case VOXEL -> {
                data.setWidth(gridSize);
                data.setHeight(gridSize);
                data.setColor(color != null ? color : new Color(164, 164, 180));
            }
            case MENU -> {
                data.setWidth(220);
                data.setHeight(150);
                data.setColor(color != null ? color : new Color(28, 32, 45, 230));
            }
            case DIALOG -> {
                data.setWidth(320);
                data.setHeight(64);
                data.setColor(color != null ? color : new Color(20, 24, 32, 220));
            }
            default -> {
                data.setWidth(60);
                data.setHeight(40);
                data.setColor(color != null ? color : new Color(180, 180, 200));
            }
        }
        if (selectedType == GameObjectType.SCENE || selectedType == GameObjectType.WALL || selectedType == GameObjectType.BOUNDARY) {
            data.setSolid(true);
            data.setBackground(false);
        }
        EditorBounds.Rect normalized = EditorBounds.normalizeRect(
            data.getX(),
            data.getY(),
            data.getWidth(),
            data.getHeight(),
            world.getWidth(),
            world.getHeight()
        );
        data.setX(normalized.x());
        data.setY(normalized.y());
        data.setWidth(normalized.width());
        data.setHeight(normalized.height());
        GameObject object = GameObjectFactory.fromObjectData(data);
        if (object instanceof MenuObject menu) {
            menu.setFontSize(defaultFontSize);
        } else if (object instanceof DialogObject dialog) {
            dialog.setFontSize(defaultFontSize);
        } else if (object instanceof PlayerObject player) {
            player.setComplementaryColorDamageEnabled(true);
        } else if (object instanceof VoxelObject voxel) {
            voxel.setColor(data.getColor());
        }
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

    private int getNudgeStep(KeyEvent event) {
        if (event != null && event.isShiftDown()) {
            return gridSnap ? Math.max(1, gridSize) : 10;
        }
        return gridSnap ? Math.max(1, gridSize / 2) : 1;
    }

    private void moveSelectedBy(int dx, int dy) {
        if (selectedObject == null || (dx == 0 && dy == 0)) {
            return;
        }
        int targetX = selectedObject.getX() + dx;
        int targetY = selectedObject.getY() + dy;
        if (gridSnap) {
            targetX = snap(targetX, gridSize);
            targetY = snap(targetY, gridSize);
        }
        EditorBounds.Rect normalized = EditorBounds.normalizePosition(selectedObject, targetX, targetY, world.getWidth(), world.getHeight());
        targetX = normalized.x();
        targetY = normalized.y();
        if (targetX == selectedObject.getX() && targetY == selectedObject.getY()) {
            return;
        }
        MoveCommand command = new MoveCommand(selectedObject, selectedObject.getX(), selectedObject.getY(), targetX, targetY);
        command.redo();
        pushCommand(command);
        panel.repaint();
    }

    private void notifySelectionChanged() {
        selectionListener.accept(selectedObject);
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
