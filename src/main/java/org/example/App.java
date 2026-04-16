package org.example;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import lib.editor.EditorWindow;
import lib.game.GameWorld;
import lib.game.LevelManager;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.persistence.MapDataMapper;
import lib.persistence.MapRepository;
import lib.render.SwingGamePanel;
import lib.state.DefaultGameStateMachine;
import lib.state.GameRuntimeActions;
import lib.state.GameState;
import lib.object.dto.MapData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
    private static final int DEFAULT_WIDTH = 960;
    private static final int DEFAULT_HEIGHT = 540;
    private static final String DEMO_MAP = "demo-map";
    private static final String MAIN_MENU_NAME = "main-menu";
    private static final String LEVEL_SELECT_MENU_NAME = "level-select-menu";
    private static final String PAUSE_MENU_NAME = "pause-menu";
    private static final String OPTIONS_MENU_NAME = "options-menu";

    public static void main(String[] args) {
        if (args != null && args.length > 0 && "editor".equalsIgnoreCase(args[0])) {
            SwingUtilities.invokeLater(App::startEditor);
            return;
        }
        SwingUtilities.invokeLater(App::startGame);
    }

    public static int add(int a, int b) {
        return a + b;
    }

    private static void startGame() {
        MapRepository repository = new MapRepository();
        LevelManager levelManager = new LevelManager();
        ensureBuiltinLevels(repository, levelManager);

        GameWorld world = loadGameWorld(repository, levelManager, DEMO_MAP, 18);
        SwingGamePanel panel = new SwingGamePanel(world);
        JFrame frame = new JFrame("Primary Software Game Demo");
        panel.setRuntimeActions(createRuntimeActions(frame, panel, world, repository, levelManager));

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                panel.stop();
            }
        });
        frame.setContentPane(panel);
        
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("游戏 (Game)");
        JMenuItem restartItem = new JMenuItem("重置 (Restart)");
        restartItem.addActionListener(e -> reloadGameWorld(world, repository, levelManager, DEMO_MAP, panel));
        JMenuItem exitItem = new JMenuItem("退出 (Exit)");
        exitItem.addActionListener(e -> requestExit(frame, panel));
        gameMenu.add(restartItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        
        JMenu toolsMenu = new JMenu("工具 (Tools)");
        JMenuItem editorItem = new JMenuItem("地图编辑器 (Level Editor)");
        editorItem.addActionListener(e -> EditorWindow.open(snapshotEditorWorld(world), repository));
        toolsMenu.add(editorItem);
        
        JMenu helpMenu = new JMenu("帮助 (Help)");
        JMenuItem aboutItem = new JMenuItem("关于 (About)");
        aboutItem.addActionListener(e -> javax.swing.JOptionPane.showMessageDialog(frame, "Primary Software Game Demo\nWASD/IJKL/Arrows to Move\nC to Cycle Color\nSpace to Jump (Gravity On)\nMouse Left: Build Voxel\nMouse Right: Destroy Voxel"));
        helpMenu.add(aboutItem);
        
        menuBar.add(gameMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        panel.start();

        log.info("Game window started with {} objects", world.getObjects().size());
    }

    private static void startEditor() {
        MapRepository repository = new MapRepository();
        LevelManager levelManager = new LevelManager();
        ensureBuiltinLevels(repository, levelManager);
        GameWorld world = loadEditorWorld(repository, levelManager, DEMO_MAP);
        EditorWindow.open(world, repository);
    }

    private static GameRuntimeActions createRuntimeActions(
        JFrame frame,
        SwingGamePanel panel,
        GameWorld world,
        MapRepository repository,
        LevelManager levelManager
    ) {
        return new GameRuntimeActions() {
            @Override
            public void requestExit() {
                App.requestExit(frame, panel);
            }

            @Override
            public void requestLoadLevel(String levelName) {
                Runnable action = () -> reloadGameWorld(world, repository, levelManager, levelName, panel);
                runOnEdt(action);
            }

            @Override
            public void requestOpenEditor() {
                Runnable action = () -> EditorWindow.open(snapshotEditorWorld(world), repository);
                runOnEdt(action);
            }
        };
    }

    private static void requestExit(JFrame frame, SwingGamePanel panel) {
        Runnable exitAction = () -> {
            panel.stop();
            if (frame.isDisplayable()) {
                frame.dispose();
            }
        };
        runOnEdt(exitAction);
    }

    private static void runOnEdt(Runnable action) {
        if (action == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeLater(action);
    }

    private static void ensureBuiltinLevels(MapRepository repository, LevelManager levelManager) {
        for (String levelName : levelManager.getLevelNames()) {
            repository.saveMap(levelManager.createLevelData(levelName));
        }
    }

    private static GameWorld loadGameWorld(
        MapRepository repository,
        LevelManager levelManager,
        String levelName,
        int uiFontSize
    ) {
        MapData mapData = loadLevelData(repository, levelManager, levelName);
        GameWorld world = mapData == null ? createFallbackWorld() : MapDataMapper.toWorld(mapData);
        removeShellMenus(world);
        installGameShell(world, resolveLevelNames(repository, levelManager), true, uiFontSize);
        ensureMainMenuSelection(world);
        world.setStateMachine(new DefaultGameStateMachine(resolveInitialState(world)));
        return world;
    }

    private static GameWorld loadEditorWorld(MapRepository repository, LevelManager levelManager, String levelName) {
        MapData mapData = loadLevelData(repository, levelManager, levelName);
        GameWorld world = mapData == null ? createFallbackWorld() : MapDataMapper.toWorld(mapData);
        removeShellMenus(world);
        return world;
    }

    private static void reloadGameWorld(GameWorld world, MapRepository repository, LevelManager levelManager,
                                        String levelName, SwingGamePanel panel) {
        if (world == null || panel == null) {
            return;
        }
        MapData mapData = loadLevelData(repository, levelManager, levelName);
        if (mapData == null) {
            return;
        }
        MapDataMapper.applyToWorld(world, mapData);
        removeShellMenus(world);
        installGameShell(world, resolveLevelNames(repository, levelManager), false, panel.getUIFontSize());
        panel.setResolution(world.getWidth(), world.getHeight());
        panel.applyUIFontSizeToWorld();
        panel.getInputController().getKeyboardManager().reset();
        panel.getInputController().getMouseManager().reset();
        panel.repaint();
    }

    private static MapData loadLevelData(MapRepository repository, LevelManager levelManager, String levelName) {
        String normalized = normalizeLevelName(levelName);
        if (normalized == null) {
            normalized = DEMO_MAP;
        }

        MapData mapData = repository.loadMapByName(normalized);
        if (mapData != null) {
            return mapData;
        }
        if (levelManager.getLevelNames().contains(normalized)) {
            MapData created = levelManager.createLevelData(normalized);
            repository.saveMap(created);
            return created;
        }
        return repository.loadMapByName(DEMO_MAP);
    }

    private static List<String> resolveLevelNames(MapRepository repository, LevelManager levelManager) {
        Set<String> names = new LinkedHashSet<>();
        for (String name : repository.listMapNames()) {
            if (isPlayableLevelName(name)) {
                names.add(name);
            }
        }
        for (String name : levelManager.getLevelNames()) {
            if (isPlayableLevelName(name)) {
                names.add(name);
            }
        }
        if (names.isEmpty()) {
            names.add(DEMO_MAP);
        }
        return new ArrayList<>(names);
    }

    private static boolean isPlayableLevelName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return !Set.of(
            MAIN_MENU_NAME,
            LEVEL_SELECT_MENU_NAME,
            PAUSE_MENU_NAME,
            OPTIONS_MENU_NAME
        ).contains(name);
    }

    private static void installGameShell(GameWorld world, List<String> levelNames, boolean mainMenuActive, int uiFontSize) {
        if (world == null) {
            return;
        }
        removeShellMenus(world);
        world.addObject(createMainMenu(world, mainMenuActive, uiFontSize));
        world.addObject(createLevelSelectMenu(world, levelNames, false, uiFontSize));
    }

    private static MenuObject createMainMenu(GameWorld world, boolean active, int uiFontSize) {
        List<String> options = List.of("Start", "Levels", "Editor", "Options", "Exit");
        int menuWidth = 240;
        int menuHeight = Math.max(180, 48 + (options.size() * Math.max(20, uiFontSize + 8)) + 12);
        MenuObject menu = new MenuObject(
            MAIN_MENU_NAME,
            24,
            24,
            menuWidth,
            menuHeight,
            "Demo Menu",
            options
        );
        menu.setActive(active);
        menu.setSelectedIndex(0);
        menu.setFontSize(uiFontSize);
        menu.setSize(menuWidth, Math.max(menuHeight, menu.getPreferredHeight()));
        menu.setPosition(24, 24);
        return menu;
    }

    private static MenuObject createLevelSelectMenu(GameWorld world, List<String> levelNames, boolean active, int uiFontSize) {
        List<String> options = new ArrayList<>();
        if (levelNames != null) {
            options.addAll(levelNames);
        }
        if (options.isEmpty()) {
            options.add(DEMO_MAP);
        }
        options.add("Back");
        int maxOptionLength = options.stream().mapToInt(option -> option == null ? 0 : option.length()).max().orElse(12);
        int menuWidth = Math.max(320, 18 * maxOptionLength);
        int menuHeight = Math.max(180, 56 + (options.size() * Math.max(20, uiFontSize + 8)) + 12);
        MenuObject menu = new MenuObject(
            LEVEL_SELECT_MENU_NAME,
            Math.max(24, (world.getWidth() - menuWidth) / 2),
            Math.max(24, (world.getHeight() - menuHeight) / 2),
            menuWidth,
            menuHeight,
            "Select Level",
            options
        );
        menu.setActive(active);
        menu.setSelectedIndex(0);
        menu.setFontSize(uiFontSize);
        menu.setSize(menuWidth, Math.max(menuHeight, menu.getPreferredHeight()));
        menu.setPosition(
            Math.max(24, (world.getWidth() - menuWidth) / 2),
            Math.max(24, (world.getHeight() - menuHeight) / 2)
        );
        return menu;
    }

    private static void removeShellMenus(GameWorld world) {
        if (world == null) {
            return;
        }
        world.getObjectsByType(GameObjectType.MENU).stream()
            .filter(object -> isShellMenuName(object.getName()))
            .forEach(world::removeObject);
    }

    private static boolean isShellMenuName(String name) {
        return MAIN_MENU_NAME.equals(name)
            || LEVEL_SELECT_MENU_NAME.equals(name)
            || PAUSE_MENU_NAME.equals(name)
            || OPTIONS_MENU_NAME.equals(name);
    }

    private static void ensureMainMenuSelection(GameWorld world) {
        if (world == null) {
            return;
        }
        for (GameObject object : world.getObjectsByType(GameObjectType.MENU)) {
            if (object instanceof MenuObject menu && MAIN_MENU_NAME.equals(menu.getName())) {
                menu.setSelectedIndex(0);
                menu.setActive(true);
            }
        }
    }

    private static GameWorld snapshotEditorWorld(GameWorld source) {
        if (source == null) {
            return createFallbackWorld();
        }
        MapData mapData = MapDataMapper.fromWorld(source, "editor-snapshot");
        GameWorld snapshot = mapData == null ? createFallbackWorld() : MapDataMapper.toWorld(mapData);
        removeShellMenus(snapshot);
        return snapshot;
    }

    private static GameWorld createFallbackWorld() {
        GameWorld world = new GameWorld(DEFAULT_WIDTH, DEFAULT_HEIGHT, new Color(32, 36, 48));
        world.setStateMachine(new DefaultGameStateMachine(GameState.PLAYING));
        return world;
    }

    private static GameState resolveInitialState(GameWorld world) {
        if (hasActiveObject(world, GameObjectType.MENU)) {
            return GameState.MENU;
        }
        if (hasActiveObject(world, GameObjectType.DIALOG)) {
            return GameState.DIALOG;
        }
        return GameState.PLAYING;
    }

    private static boolean hasActiveObject(GameWorld world, GameObjectType type) {
        if (world == null) {
            return false;
        }
        for (GameObject object : world.getObjectsByType(type)) {
            if (object.isActive()) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeLevelName(String levelName) {
        if (levelName == null || levelName.isBlank()) {
            return null;
        }
        return levelName.trim();
    }
}
