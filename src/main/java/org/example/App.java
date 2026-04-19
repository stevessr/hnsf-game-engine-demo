package org.example;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import lib.editor.EditorWindow;
import lib.game.GameWorld;
import lib.game.LevelManager;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.object.dto.MapData;
import lib.persistence.MapDataMapper;
import lib.persistence.MapRepository;
import lib.render.SwingGamePanel;
import lib.state.DefaultGameStateMachine;
import lib.state.GameRuntimeActions;
import lib.state.GameState;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

/**
 * 游戏应用程序主入口。
 */
@Slf4j
public class App {
    private static final int DEFAULT_WIDTH = 960;
    private static final int DEFAULT_HEIGHT = 540;
    private static final String DEFAULT_START_MAP = "tutorial";
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

    /**
     * 简单的加法辅助方法 (用于测试)。
     * 
     * @param a 第一个数
     * @param b 第二个数
     * @return 两数之和
     */
    public static int add(int a, int b) {
        return a + b;
    }

    private static void startGame() {
        MapRepository repository = new MapRepository();
        LevelManager levelManager = new LevelManager();
        ensureBuiltinLevels(repository, levelManager);
        AtomicReference<String> activeLevelName = new AtomicReference<>(DEFAULT_START_MAP);

        GameWorld world = loadGameWorld(repository, levelManager, DEFAULT_START_MAP, 18);
        SwingGamePanel panel = new SwingGamePanel(world);
        JFrame frame = new JFrame("Primary Software Game Demo");
        
        setupMainFrame(frame, panel, world, repository, levelManager, activeLevelName);
        frame.setVisible(true);
        panel.start();

        log.info("Game window started with {} objects", world.getObjects().size());
    }

    private static void setupMainFrame(
        JFrame frame,
        SwingGamePanel panel,
        GameWorld world,
        MapRepository repository,
        LevelManager levelManager,
        AtomicReference<String> activeLevelName
    ) {
        panel.setRuntimeActions(createRuntimeActions(frame, panel, world, repository, levelManager, activeLevelName));

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                panel.stop();
            }
        });
        frame.setContentPane(panel);
        frame.setJMenuBar(createMenuBar(frame, world, repository, levelManager, panel, activeLevelName));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
    }

    private static JMenuBar createMenuBar(
        JFrame frame,
        GameWorld world,
        MapRepository repository,
        LevelManager levelManager,
        SwingGamePanel panel,
        AtomicReference<String> activeLevelName
    ) {
        JMenuBar menuBar = new JMenuBar();
        
        // 游戏菜单
        JMenu gameMenu = new JMenu("游戏 (Game)");
        JMenuItem restartItem = new JMenuItem("重置 (Restart)");
        restartItem.addActionListener(e -> {
            String currentLevel = normalizeLevelName(activeLevelName.get());
            String targetLevel = currentLevel == null ? DEFAULT_START_MAP : currentLevel;
            String loadedLevel = reloadGameWorld(world, repository, levelManager, targetLevel, panel);
            if (loadedLevel != null) {
                activeLevelName.set(loadedLevel);
            }
        });
        
        JMenuItem importItem = new JMenuItem("导入地图 (Import Map)");
        importItem.addActionListener(e -> handleImportMap(frame, world, repository, levelManager, panel, activeLevelName));

        JMenuItem exportItem = new JMenuItem("导出当前地图 (Export Map)");
        exportItem.addActionListener(e -> handleExportMap(frame, world));

        JMenuItem exitItem = new JMenuItem("退出 (Exit)");
        exitItem.addActionListener(e -> requestExit(frame, panel));
        
        gameMenu.add(restartItem);
        gameMenu.add(importItem);
        gameMenu.add(exportItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        
        // 工具菜单
        JMenu toolsMenu = new JMenu("工具 (Tools)");
        JMenuItem editorItem = new JMenuItem("地图编辑器 (Level Editor)");
        editorItem.addActionListener(e -> EditorWindow.open(snapshotEditorWorld(world), repository));
        toolsMenu.add(editorItem);
        
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助 (Help)");
        JMenuItem aboutItem = new JMenuItem("关于 (About)");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, 
            "Primary Software Game Engine Prototype\n\n" +
            "Controls:\n" +
            "- WASD/Arrows: Move\n" +
            "- Space: Jump\n" +
            "- K: Shoot\n" +
            "- T: Cycle Throttle\n" +
            "- C: Cycle Color\n" +
            "- P/Esc: Pause"));
        helpMenu.add(aboutItem);
        
        menuBar.add(gameMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private static void handleImportMap(
        JFrame frame,
        GameWorld world,
        MapRepository repository,
        LevelManager levelManager,
        SwingGamePanel panel,
        AtomicReference<String> activeLevelName
    ) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = Files.readString(chooser.getSelectedFile().toPath());
                var mapData = MapDataMapper.importFromJson(new JSONObject(content));
                repository.saveMap(mapData);
                String loadedLevel = reloadGameWorld(world, repository, levelManager, mapData.getName(), panel);
                if (loadedLevel != null) {
                    activeLevelName.set(loadedLevel);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "导入失败: " + ex.getMessage());
            }
        }
    }

    private static void handleExportMap(JFrame frame, GameWorld world) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                var mapData = MapDataMapper.fromWorld(world, "exported-map");
                var json = MapDataMapper.exportToJson(mapData);
                Files.writeString(chooser.getSelectedFile().toPath(), json.toString(4));
                JOptionPane.showMessageDialog(frame, "地图导出成功！");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "导出失败: " + ex.getMessage());
            }
        }
    }

    private static void startEditor() {
        MapRepository repository = new MapRepository();
        LevelManager levelManager = new LevelManager();
        ensureBuiltinLevels(repository, levelManager);
        GameWorld world = loadEditorWorld(repository, levelManager, DEFAULT_START_MAP);
        EditorWindow.open(world, repository);
    }

    private static GameRuntimeActions createRuntimeActions(
        JFrame frame,
        SwingGamePanel panel,
        GameWorld world,
        MapRepository repository,
        LevelManager levelManager,
        AtomicReference<String> activeLevelName
    ) {
        return new GameRuntimeActions() {
            @Override
            public void requestExit() {
                App.requestExit(frame, panel);
            }

            @Override
            public void requestLoadLevel(String levelName) {
                String normalizedRequested = normalizeLevelName(levelName);
                String fallbackCurrent = normalizeLevelName(activeLevelName.get());
                String targetLevel = normalizedRequested == null ? fallbackCurrent : normalizedRequested;
                if (targetLevel == null) {
                    targetLevel = DEFAULT_START_MAP;
                }
                String levelToLoad = targetLevel;
                Runnable action = () -> {
                    String loadedLevel = reloadGameWorld(world, repository, levelManager, levelToLoad, panel);
                    if (loadedLevel != null) {
                        activeLevelName.set(loadedLevel);
                    }
                };
                runOnEdt(action);
            }

            @Override
            public void requestLoadNextLevel() {
                Runnable action = () -> {
                    String nextLevel = resolveNextLevelName(repository, levelManager, activeLevelName.get());
                    if (nextLevel == null) {
                        return;
                    }
                    String loadedLevel = reloadGameWorld(world, repository, levelManager, nextLevel, panel);
                    if (loadedLevel != null) {
                        activeLevelName.set(loadedLevel);
                    }
                };
                runOnEdt(action);
            }

            @Override
            public boolean hasNextLevel() {
                return resolveNextLevelName(repository, levelManager, activeLevelName.get()) != null;
            }

            @Override
            public void requestOpenEditor() {
                Runnable action = () -> {
                    EditorWindow.open(snapshotEditorWorld(world), repository);
                };
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
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private static void ensureBuiltinLevels(MapRepository repository, LevelManager levelManager) {
        for (String levelName : levelManager.getLevelNames()) {
            repository.saveMap(levelManager.createLevelData(levelName));
        }
    }

    private static GameWorld loadGameWorld(MapRepository repository, LevelManager levelManager, String levelName, int uiFontSize) {
        MapData mapData = loadLevelData(repository, levelManager, levelName);
        GameWorld world = mapData == null ? createFallbackWorld() : MapDataMapper.toWorld(mapData);
        if (mapData != null) {
            levelManager.setCurrentLevel(mapData.getName());
        }
        removeShellMenus(world);
        installGameShell(world, resolveLevelNames(repository, levelManager), true, uiFontSize);
        ensureMainMenuSelection(world);
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(resolveInitialState(world));
        world.setStateMachine(stateMachine);
        stateMachine.init(world);
        return world;
    }

    private static GameWorld loadEditorWorld(MapRepository repository, LevelManager levelManager, String levelName) {
        MapData mapData = loadLevelData(repository, levelManager, levelName);
        GameWorld world = mapData == null ? createFallbackWorld() : MapDataMapper.toWorld(mapData);
        removeShellMenus(world);
        return world;
    }

    private static String reloadGameWorld(
        GameWorld world,
        MapRepository repository,
        LevelManager levelManager,
        String levelName,
        SwingGamePanel panel
    ) {
        if (world == null || panel == null) {
            return null;
        }
        MapData mapData = loadLevelData(repository, levelManager, levelName);
        if (mapData == null) {
            return null;
        }
        
        MapDataMapper.applyToWorld(world, mapData);
        levelManager.setCurrentLevel(mapData.getName());
        removeShellMenus(world);
        installGameShell(world, resolveLevelNames(repository, levelManager), false, panel.getUIFontSize());
        
        if (world.getStateMachine() instanceof DefaultGameStateMachine dsm) {
            dsm.init(world);
        }
        
        panel.getInputController().getKeyboardManager().reset();
        panel.getInputController().getMouseManager().reset();
        panel.repaint();
        return mapData.getName();
    }

    private static MapData loadLevelData(MapRepository repository, LevelManager levelManager, String levelName) {
        String normalized = normalizeLevelName(levelName);
        if (normalized == null) {
            normalized = DEFAULT_START_MAP;
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
        return repository.loadMapByName(DEFAULT_START_MAP);
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
            names.add(DEFAULT_START_MAP);
        }
        return new ArrayList<>(names);
    }

    private static String resolveNextLevelName(MapRepository repository, LevelManager levelManager, String currentLevelName) {
        List<String> names = resolveLevelNames(repository, levelManager);
        if (names.isEmpty()) {
            return null;
        }
        String normalizedCurrent = normalizeLevelName(currentLevelName);
        if (normalizedCurrent == null) {
            return null;
        }
        int currentIndex = names.indexOf(normalizedCurrent);
        if (currentIndex < 0) {
            return null;
        }
        if (currentIndex + 1 >= names.size()) {
            return null;
        }
        return names.get(currentIndex + 1);
    }

    private static boolean isPlayableLevelName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return !Set.of(MAIN_MENU_NAME, LEVEL_SELECT_MENU_NAME, PAUSE_MENU_NAME, OPTIONS_MENU_NAME).contains(name);
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
        MenuObject menu = new MenuObject(MAIN_MENU_NAME, 24, 24, 240, 180, "Demo Menu", options);
        menu.setActive(active);
        menu.setSelectedIndex(0);
        menu.setFontSize(uiFontSize);
        menu.setSize(240, Math.max(180, menu.getPreferredHeight()));
        return menu;
    }

    private static MenuObject createLevelSelectMenu(GameWorld world, List<String> levelNames, boolean active, int uiFontSize) {
        List<String> options = new ArrayList<>();
        if (levelNames != null) {
            options.addAll(levelNames);
        }
        if (options.isEmpty()) {
            options.add(DEFAULT_START_MAP);
        }
        options.add("Back");
        int maxOptionLength = options.stream().mapToInt(s -> s == null ? 0 : s.length()).max().orElse(12);
        MenuObject menu = new MenuObject(LEVEL_SELECT_MENU_NAME, 24, 24, Math.max(320, 18 * maxOptionLength), 180, "Select Level", options);
        menu.setActive(active);
        menu.setSelectedIndex(0);
        menu.setFontSize(uiFontSize);
        menu.setSize(menu.getWidth(), Math.max(180, menu.getPreferredHeight()));
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
        return Set.of(MAIN_MENU_NAME, LEVEL_SELECT_MENU_NAME, PAUSE_MENU_NAME, OPTIONS_MENU_NAME).contains(name);
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
        return levelName == null || levelName.isBlank() ? null : levelName.trim();
    }
}
