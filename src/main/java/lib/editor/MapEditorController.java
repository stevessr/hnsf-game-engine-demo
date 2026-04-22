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
    private Point dragStartPos;
    private java.awt.Dimension dragStartSize;
    private int resizeHandle = -1; // -1: none, 0: top-left, 1: top-right, 2: bottom-left, 3: bottom-right
    private Consumer<GameObject> selectionListener = object -> {
    };
    private Consumer<EditMode> modeChangeListener = mode -> {
    };
    private Runnable saveListener = () -> {
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

    public void setSaveListener(Runnable saveListener) {
        this.saveListener = saveListener == null ? () -> {
        } : saveListener;
    }

    public void bind() {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleMousePressed(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                handleMouseReleased(event);
            }
        });
        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                handleMouseDragged(event);
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                handleMouseMoved(event);
            }
        });

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKeyPressed(event);
            }
        });
    }

    private void handleMouseMoved(MouseEvent event) {
        Point point = event.getPoint();
        if (editMode == EditMode.SELECT) {
            int handle = getResizeHandleAt(point);
            if (handle != -1) {
                switch (handle) {
                    case 0, 3 -> panel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.NW_RESIZE_CURSOR));
                    case 1, 2 -> panel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.NE_RESIZE_CURSOR));
                }
                return;
            }
            if (findObjectAt(point) != null) {
                panel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
                return;
            }
        } else if (editMode == EditMode.BUILD) {
            panel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.CROSSHAIR_CURSOR));
            return;
        } else if (editMode == EditMode.ERASE) {
            panel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            return;
        }
        panel.setCursor(java.awt.Cursor.getDefaultCursor());
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
        overlay.setSelectedObject(selectedObject);
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
            dragStartPos = null;
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
        overlay.setSelectedObject(null);
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
        
        String name = sourceData.getName();
        if (name.contains("-copy-")) {
            name = name.substring(0, name.lastIndexOf("-copy-"));
        }
        sourceData.setName(name + "-copy-" + System.currentTimeMillis() % 10000);
        
        int offset = gridSnap ? Math.max(1, gridSize) : 10;
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
        overlay.setSelectedObject(duplicated);
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
        overlay.setSelectedObject(selectedObject);
        notifySelectionChanged();
        panel.repaint();
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        EditorCommand command = redoStack.pop();
        command.redo();
        undoStack.push(command);
        overlay.setSelectedObject(selectedObject);
        notifySelectionChanged();
        panel.repaint();
    }

    public void executePropertyChange(Runnable action, Runnable undoAction) {
        if (action == null || undoAction == null) {
            return;
        }
        PropertyCommand command = new PropertyCommand(action, undoAction);
        command.redo();
        pushCommand(command);
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

        // Check for resize handles if an object is selected
        if (selectedObject != null) {
            resizeHandle = getResizeHandleAt(point);
            if (resizeHandle != -1) {
                dragStartPos = new Point(selectedObject.getX(), selectedObject.getY());
                dragStartSize = new java.awt.Dimension(selectedObject.getWidth(), selectedObject.getHeight());
                panel.repaint();
                return;
            }
        }

        GameObject hit = findObjectAt(point);
        if (hit != null) {
            selectedObject = hit;
            overlay.setSelectedObject(hit);
            notifySelectionChanged();
            dragOffset = new Point(point.x - hit.getX(), point.y - hit.getY());
            dragStartPos = new Point(hit.getX(), hit.getY());
            dragStartSize = new java.awt.Dimension(hit.getWidth(), hit.getHeight());
            panel.repaint();
            return;
        }
        GameObject created = createObjectAt(point.x, point.y);
        if (created != null) {
            world.addObject(created);
            selectedObject = created;
            overlay.setSelectedObject(created);
            notifySelectionChanged();
            pushCommand(new AddCommand(created));
        }
        panel.repaint();
    }

    private void handleMouseReleased(MouseEvent event) {
        if (selectedObject != null && dragStartPos != null) {
            if (resizeHandle != -1) {
                if (selectedObject.getX() != dragStartPos.x || selectedObject.getY() != dragStartPos.y ||
                    selectedObject.getWidth() != dragStartSize.width || selectedObject.getHeight() != dragStartSize.height) {
                    pushCommand(new ResizeCommand(
                        selectedObject,
                        dragStartPos.x, dragStartPos.y,
                        dragStartSize.width, dragStartSize.height,
                        selectedObject.getX(), selectedObject.getY(),
                        selectedObject.getWidth(), selectedObject.getHeight()
                    ));
                }
            } else if (selectedObject.getX() != dragStartPos.x || selectedObject.getY() != dragStartPos.y) {
                pushCommand(new MoveCommand(
                    selectedObject,
                    dragStartPos.x,
                    dragStartPos.y,
                    selectedObject.getX(),
                    selectedObject.getY()
                ));
            }
        }
        dragOffset = null;
        lastPaintPoint = null;
        dragStartPos = null;
        dragStartSize = null;
        resizeHandle = -1;
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
        if (selectedObject == null) {
            return;
        }

        if (resizeHandle != -1) {
            handleResizeDragging(point);
            panel.repaint();
            return;
        }

        if (dragOffset == null) {
            return;
        }
        
        int targetX = point.x - dragOffset.x;
        int targetY = point.y - dragOffset.y;
        if (gridSnap) {
            targetX = snap(targetX, gridSize);
            targetY = snap(targetY, gridSize);
        }
        EditorBounds.Rect normalized = EditorBounds.normalizePosition(selectedObject, targetX, targetY, world.getWidth(), world.getHeight());
        selectedObject.setPosition(normalized.x(), normalized.y());
        panel.repaint();
    }

    private int getResizeHandleAt(Point p) {
        if (selectedObject == null) return -1;
        int x = selectedObject.getX();
        int y = selectedObject.getY();
        int w = selectedObject.getWidth();
        int h = selectedObject.getHeight();
        int hs = 8; // Larger hit area for handles
        
        if (new Rectangle(x - hs, y - hs, hs * 2, hs * 2).contains(p)) return 0;
        if (new Rectangle(x + w - hs, y - hs, hs * 2, hs * 2).contains(p)) return 1;
        if (new Rectangle(x - hs, y + h - hs, hs * 2, hs * 2).contains(p)) return 2;
        if (new Rectangle(x + w - hs, y + h - hs, hs * 2, hs * 2).contains(p)) return 3;
        
        return -1;
    }

    private void handleResizeDragging(Point p) {
        int x = selectedObject.getX();
        int y = selectedObject.getY();
        int w = selectedObject.getWidth();
        int h = selectedObject.getHeight();
        
        int px = gridSnap ? snap(p.x, gridSize) : p.x;
        int py = gridSnap ? snap(p.y, gridSize) : p.y;
        
        int minSize = 4;
        
        switch (resizeHandle) {
            case 0 -> { // Top-left
                int newX = Math.min(px, x + w - minSize);
                int newY = Math.min(py, y + h - minSize);
                selectedObject.setPosition(newX, newY);
                selectedObject.setSize(x + w - newX, y + h - newY);
            }
            case 1 -> { // Top-right
                int newY = Math.min(py, y + h - minSize);
                selectedObject.setPosition(x, newY);
                selectedObject.setSize(Math.max(minSize, px - x), y + h - newY);
            }
            case 2 -> { // Bottom-left
                int newX = Math.min(px, x + w - minSize);
                selectedObject.setPosition(newX, y);
                selectedObject.setSize(x + w - newX, Math.max(minSize, py - y));
            }
            case 3 -> { // Bottom-right
                selectedObject.setSize(Math.max(minSize, px - x), Math.max(minSize, py - y));
            }
        }
        
        // Normalize bounds
        EditorBounds.Rect norm = EditorBounds.normalizeRect(
            selectedObject.getX(), selectedObject.getY(),
            selectedObject.getWidth(), selectedObject.getHeight(),
            world.getWidth(), world.getHeight()
        );
        selectedObject.setPosition(norm.x(), norm.y());
        selectedObject.setSize(norm.width(), norm.height());
        notifySelectionChanged();
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
        overlay.setSelectedObject(created);
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
            overlay.setSelectedObject(null);
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
        if (event.isControlDown() && event.getKeyCode() == KeyEvent.VK_S) {
            saveListener.run();
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
        int fromX = selectedObject.getX();
        int fromY = selectedObject.getY();
        int targetX = fromX + dx;
        int targetY = fromY + dy;
        if (gridSnap) {
            targetX = snap(targetX, gridSize);
            targetY = snap(targetY, gridSize);
        }
        EditorBounds.Rect normalized = EditorBounds.normalizePosition(selectedObject, targetX, targetY, world.getWidth(), world.getHeight());
        targetX = normalized.x();
        targetY = normalized.y();
        if (targetX == fromX && targetY == fromY) {
            return;
        }
        selectedObject.setPosition(targetX, targetY);
        pushCommand(new MoveCommand(selectedObject, fromX, fromY, targetX, targetY));
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
            if (selectedObject == object) {
                selectedObject = null;
                overlay.setSelectedObject(null);
            }
        }

        @Override
        public void redo() {
            world.addObject(object);
            selectedObject = object;
            overlay.setSelectedObject(object);
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
            selectedObject = object;
            overlay.setSelectedObject(object);
        }

        @Override
        public void redo() {
            world.removeObject(object);
            if (selectedObject == object) {
                selectedObject = null;
                overlay.setSelectedObject(null);
            }
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
            selectedObject = object;
            overlay.setSelectedObject(object);
        }

        @Override
        public void redo() {
            object.setPosition(toX, toY);
            selectedObject = object;
            overlay.setSelectedObject(object);
        }
    }

    private final class ResizeCommand implements EditorCommand {
        private final GameObject object;
        private final int fromX, fromY, fromW, fromH;
        private final int toX, toY, toW, toH;

        private ResizeCommand(GameObject object, int fromX, int fromY, int fromW, int fromH, int toX, int toY, int toW, int toH) {
            this.object = object;
            this.fromX = fromX; this.fromY = fromY;
            this.fromW = fromW; this.fromH = fromH;
            this.toX = toX; this.toY = toY;
            this.toW = toW; this.toH = toH;
        }

        @Override
        public void undo() {
            object.setPosition(fromX, fromY);
            object.setSize(fromW, fromH);
            selectedObject = object;
            overlay.setSelectedObject(object);
            notifySelectionChanged();
        }

        @Override
        public void redo() {
            object.setPosition(toX, toY);
            object.setSize(toW, toH);
            selectedObject = object;
            overlay.setSelectedObject(object);
            notifySelectionChanged();
        }
    }

    private final class PropertyCommand implements EditorCommand {
        private final Runnable action;
        private final Runnable undoAction;

        private PropertyCommand(Runnable action, Runnable undoAction) {
            this.action = action;
            this.undoAction = undoAction;
        }

        @Override
        public void undo() {
            undoAction.run();
            notifySelectionChanged();
            panel.repaint();
        }

        @Override
        public void redo() {
            action.run();
            notifySelectionChanged();
            panel.repaint();
        }
    }
}
