package lib.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import lib.game.GameWorld;
import lib.game.WinConditionType;
import lib.object.ActorObject;
import lib.object.BaseObject;
import lib.object.DialogObject;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.GameObjectFactory;
import lib.object.ItemObject;
import lib.object.MenuObject;
import lib.object.MonsterObject;
import lib.object.MonsterKind;
import lib.object.PlayerObject;
import lib.object.ProjectileType;
import lib.object.SceneObject;
import lib.object.SpawnerObject;
import lib.object.TriggerAction;
import lib.object.TriggerObject;
import lib.object.dto.MapBackgroundMode;
import lib.object.dto.MapBackgroundPreset;
import lib.persistence.MapDataMapper;
import lib.persistence.MapRepository;

public final class EditorWindow extends JFrame {
    private final GameWorld world;
    private final EditorOverlay overlay;
    private final EditorGamePanel previewPanel;
    private final MapEditorController controller;
    private final MapRepository repository;
    private final JComboBox<GameObjectType> typeSelector;
    private final JTextField nameField;
    private final JCheckBox activeToggle;
    private final JComboBox<String> levelSelector;
    private final JComboBox<String> modeSelector;
    private final JTextField mapNameField;
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JSpinner gravityStrengthSpinner;
    private final JToggleButton gravityToggle;
    private final JComboBox<WinConditionType> winConditionSelector;
    private final JSpinner targetKillsSpinner;
    private final JSpinner targetItemsSpinner;
    private final JComboBox<MapBackgroundPreset> backgroundPresetSelector;
    private final JComboBox<MapBackgroundMode> backgroundModeSelector;
    private final JButton backgroundColorButton;
    private final JTextField backgroundImageField;
    private final JButton importBackgroundImageButton;
    private final JButton clearBackgroundImageButton;
    private final JSpinner xSpinner;
    private final JSpinner ySpinner;
    private final JSpinner wSpinner;
    private final JSpinner hSpinner;
    private final JSpinner fontSizeSpinner;
    private final JSpinner menuColumnsSpinner;
    private final JSpinner maxVisibleRowsSpinner;
    private final JSpinner damageSpinner;
    private final JButton colorButton;
    private final JCheckBox damageToggle;
    private final JComboBox<ProjectileType> projectileTypeSelector;
    private final JTextField itemKindField;
    private final JSpinner itemValueSpinner;
    private final JTextField itemMessageField;
    private final JTextField triggerTargetField;
    private final JComboBox<TriggerAction> triggerActionSelector;
    private final JCheckBox triggerOnceToggle;
    private final JComboBox<MonsterKind> spawnerKindSelector;
    private final JSpinner spawnIntervalSpinner;
    private final JSpinner maxAliveSpinner;
    private final JSpinner spawnWaveSizeSpinner;
    private final JSpinner spawnRadiusSpinner;
    private final JSpinner spawnOffsetXSpinner;
    private final JSpinner spawnOffsetYSpinner;
    private final JTextField texturePathField;
    private final JTextField materialField;
    private final JSpinner healthSpinner;
    private final JSpinner attackSpinner;
    private final JSpinner speedSpinner;
    private final JSpinner monsterHealDropSpinner;
    private final JCheckBox rangedAttackToggle;
    private final JSpinner shootRangeSpinner;
    private final JSpinner projectileSpeedSpinner;
    private final JSpinner shootCooldownSpinner;
    private final JComboBox<MonsterKind> monsterKindSelector;
    private final JSpinner monsterGravitySpinner;
    private final JCheckBox monsterRevivableToggle;
    private final JSpinner monsterReviveDelaySpinner;
    private final JCheckBox destructibleToggle;
    private final JSpinner durabilitySpinner;
    private final JCheckBox collapseToggle;
    private final JSpinner collapseDamageSpinner;
    private final JSpinner breakAfterStepsSpinner;
    private final JToggleButton gridToggle;
    private final JToggleButton snapToggle;
    private final JSpinner gridSizeSpinner;
    private final JLabel selectionInfoLabel;
    private final JLabel modeHintLabel;
    private boolean updatingControls;

    public EditorWindow(GameWorld world, MapRepository repository) {
        super("关卡编辑器");
        this.world = world;
        this.repository = repository;
        this.overlay = new EditorOverlay();
        this.previewPanel = new EditorGamePanel(world, overlay);
        this.controller = new MapEditorController(world, previewPanel, overlay);
        this.typeSelector = new JComboBox<>(GameObjectType.values());
        this.nameField = new JTextField();
        this.nameField.setColumns(14);
        this.activeToggle = new JCheckBox("启用", true);
        this.levelSelector = new JComboBox<>();
        this.modeSelector = new JComboBox<>(new String[] {"选择", "建造", "破坏"});
        this.mapNameField = new JTextField("demo-map");
        this.widthSpinner = new JSpinner(new SpinnerNumberModel(world.getWidth(), 320, 4000, 10));
        this.heightSpinner = new JSpinner(new SpinnerNumberModel(world.getHeight(), 240, 3000, 10));
        this.gravityStrengthSpinner = new JSpinner(new SpinnerNumberModel(world.getGravityStrength(), 0, 5000, 10));
        this.gravityToggle = new JToggleButton("重力", world.isGravityEnabled());
        this.winConditionSelector = new JComboBox<>(WinConditionType.values());
        this.targetKillsSpinner = new JSpinner(new SpinnerNumberModel(world.getTargetKills(), 0, 999, 1));
        this.targetItemsSpinner = new JSpinner(new SpinnerNumberModel(world.getTargetItems(), 0, 999, 1));
        this.backgroundPresetSelector = new JComboBox<>(MapBackgroundPreset.values());
        this.backgroundModeSelector = new JComboBox<>(MapBackgroundMode.values());
        this.backgroundColorButton = new JButton("背景颜色");
        this.backgroundColorButton.setOpaque(true);
        this.backgroundColorButton.setBackground(world.getBackgroundColor());
        this.backgroundImageField = new JTextField("");
        this.backgroundImageField.setEditable(false);
        this.importBackgroundImageButton = new JButton("导入图片");
        this.clearBackgroundImageButton = new JButton("清除图片");
        this.xSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 4000, 1));
        this.ySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 4000, 1));
        this.wSpinner = new JSpinner(new SpinnerNumberModel(80, 4, 4000, 1));
        this.hSpinner = new JSpinner(new SpinnerNumberModel(60, 4, 4000, 1));
        this.fontSizeSpinner = new JSpinner(new SpinnerNumberModel(18, 10, 64, 1));
        this.menuColumnsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        this.maxVisibleRowsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        this.damageSpinner = new JSpinner(new SpinnerNumberModel(14, 0, 999, 1));
        this.colorButton = new JButton("颜色");
        this.colorButton.setOpaque(true);
        this.damageToggle = new JCheckBox("互补色伤害", true);
        this.projectileTypeSelector = new JComboBox<>(ProjectileType.values());
        this.itemKindField = new JTextField("coin");
        this.itemKindField.setColumns(10);
        this.itemValueSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 9999, 1));
        this.itemMessageField = new JTextField("");
        this.itemMessageField.setColumns(12);
        this.triggerTargetField = new JTextField("");
        this.triggerTargetField.setColumns(12);
        this.triggerActionSelector = new JComboBox<>(TriggerAction.values());
        this.triggerOnceToggle = new JCheckBox("仅触发一次", false);
        this.spawnerKindSelector = new JComboBox<>(MonsterKind.values());
        this.spawnIntervalSpinner = new JSpinner(new SpinnerNumberModel(4.0, 0.1, 60.0, 0.1));
        this.maxAliveSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        this.spawnWaveSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        this.spawnRadiusSpinner = new JSpinner(new SpinnerNumberModel(24, 0, 2000, 1));
        this.spawnOffsetXSpinner = new JSpinner(new SpinnerNumberModel(0, -4000, 4000, 1));
        this.spawnOffsetYSpinner = new JSpinner(new SpinnerNumberModel(0, -4000, 4000, 1));
        this.texturePathField = new JTextField("");
        this.materialField = new JTextField("");
        this.healthSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 9999, 1));
        this.attackSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 999, 1));
        this.speedSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 999, 1));
        this.monsterHealDropSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
        this.rangedAttackToggle = new JCheckBox("远程攻击", false);
        this.shootRangeSpinner = new JSpinner(new SpinnerNumberModel(360, 40, 4000, 10));
        this.projectileSpeedSpinner = new JSpinner(new SpinnerNumberModel(320, 80, 4000, 10));
        this.shootCooldownSpinner = new JSpinner(new SpinnerNumberModel(1.2, 0.1, 10.0, 0.1));
        this.monsterKindSelector = new JComboBox<>(MonsterKind.values());
        this.monsterGravitySpinner = new JSpinner(new SpinnerNumberModel(100, 0, 200, 1));
        this.monsterRevivableToggle = new JCheckBox("可复活", false);
        this.monsterReviveDelaySpinner = new JSpinner(new SpinnerNumberModel(6.0, 0.0, 3600.0, 0.5));
        this.destructibleToggle = new JCheckBox("可破坏", false);
        this.durabilitySpinner = new JSpinner(new SpinnerNumberModel(100, 1, 9999, 1));
        this.collapseToggle = new JCheckBox("失去支撑后倒塌", false);
        this.collapseDamageSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 9999, 1));
        this.breakAfterStepsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
        this.gridToggle = new JToggleButton("网格显示", true);
        this.snapToggle = new JToggleButton("网格吸附", true);
        this.gridSizeSpinner = new JSpinner(new SpinnerNumberModel(20, 4, 200, 1));
        this.selectionInfoLabel = new JLabel("未选中对象");
        this.modeHintLabel = new JLabel();
        this.modeHintLabel.setText(formatModeHint(MapEditorController.EditMode.SELECT));
        initLayout();
        initActions();
        controller.setSelectionListener(this::updateInspectorFromSelection);
        controller.setModeChangeListener(mode -> {
            updatingControls = true;
            modeSelector.setSelectedItem(formatEditMode(mode));
            modeHintLabel.setText(formatModeHint(mode));
            updatingControls = false;
        });
        controller.setSaveListener(this::saveMap);
        refreshLevelSelector();
        updateWorldControlsFromWorld();
        updateInspectorFromSelection(null);
        controller.setEditMode(MapEditorController.EditMode.SELECT);
        controller.bind();
    }

    private void initLayout() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        setMinimumSize(new Dimension(1280, 800));

        JPanel sidebarPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        sidebarPanel.setPreferredSize(new Dimension(360, 0));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        sidebarPanel.add(buildSelectionEditPanel());
        sidebarPanel.add(buildAddPalettePanel());

        JPanel previewWrapper = new JPanel(new BorderLayout());
        previewWrapper.setBorder(BorderFactory.createTitledBorder("预览"));
        previewWrapper.add(new JScrollPane(previewPanel), BorderLayout.CENTER);

        JTabbedPane inspectorTabs = new JTabbedPane();
        inspectorTabs.addTab("地图", buildMapPanel());
        inspectorTabs.addTab("对象", buildPropertyPanel());
        inspectorTabs.addTab("工具", buildToolPanel());

        add(sidebarPanel, BorderLayout.WEST);
        add(previewWrapper, BorderLayout.CENTER);
        add(inspectorTabs, BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildSelectionEditPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("选择 / 编辑"));
        panel.add(new JLabel("编辑模式"));
        panel.add(modeSelector);
        panel.add(modeHintLabel);
        return panel;
    }

    private JPanel buildAddPalettePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("单次添加"));
        JPanel buttons = new JPanel(new GridLayout(0, 1, 6, 6));
        for (GameObjectType type : GameObjectType.values()) {
            buttons.add(createSingleAddButton(type));
        }
        JScrollPane scrollPane = new JScrollPane(buttons);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JButton createSingleAddButton(GameObjectType type) {
        JButton button = new JButton(type.name());
        button.setToolTipText("添加一个 " + type.name());
        button.addActionListener(event -> {
            controller.setEditMode(MapEditorController.EditMode.SELECT);
            controller.addObjectOnce(type);
            previewPanel.requestFocusInWindow();
        });
        return button;
    }

    private JPanel buildMapPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("关卡"));
        form.add(new JLabel("选择"));
        form.add(levelSelector);
        form.add(new JLabel("名称"));
        form.add(mapNameField);
        form.add(new JLabel("宽度"));
        form.add(widthSpinner);
        form.add(new JLabel("高度"));
        form.add(heightSpinner);
        form.add(new JLabel("预设风格"));
        form.add(backgroundPresetSelector);
        form.add(new JLabel("背景模式"));
        form.add(backgroundModeSelector);
        form.add(new JLabel("背景主色"));
        form.add(backgroundColorButton);
        form.add(new JLabel("背景图片"));
        JPanel backgroundImageRow = new JPanel(new BorderLayout(4, 0));
        backgroundImageField.setColumns(12);
        backgroundImageRow.add(backgroundImageField, BorderLayout.CENTER);
        JPanel backgroundImageButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        backgroundImageButtons.add(importBackgroundImageButton);
        backgroundImageButtons.add(clearBackgroundImageButton);
        backgroundImageRow.add(backgroundImageButtons, BorderLayout.EAST);
        form.add(backgroundImageRow);
        form.add(new JLabel("重力开关"));
        form.add(gravityToggle);
        form.add(new JLabel("重力强度"));
        form.add(gravityStrengthSpinner);
        form.add(new JLabel("胜利条件"));
        form.add(winConditionSelector);
        form.add(new JLabel("击杀目标"));
        form.add(targetKillsSpinner);
        form.add(new JLabel("收集目标"));
        form.add(targetItemsSpinner);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPropertyPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("对象属性"));
        form.add(new JLabel("选中"));
        form.add(selectionInfoLabel);
        form.add(new JLabel("名称"));
        form.add(nameField);
        form.add(new JLabel("类型"));
        form.add(typeSelector);
        form.add(new JLabel("启用"));
        form.add(activeToggle);
        form.add(new JLabel("X"));
        form.add(xSpinner);
        form.add(new JLabel("Y"));
        form.add(ySpinner);
        form.add(new JLabel("宽"));
        form.add(wSpinner);
        form.add(new JLabel("高"));
        form.add(hSpinner);
        form.add(new JLabel("颜色"));
        form.add(colorButton);
        form.add(new JLabel("字号"));
        form.add(fontSizeSpinner);
        form.add(new JLabel("菜单列数"));
        form.add(menuColumnsSpinner);
        form.add(new JLabel("最大显示行数(0=自适应)"));
        form.add(maxVisibleRowsSpinner);
        
        form.add(new JLabel("纹理路径"));
        form.add(texturePathField);
        form.add(new JLabel("材质"));
        form.add(materialField);
        
        form.add(new JLabel("--- 战斗属性 ---"));
        form.add(new JLabel(""));
        form.add(new JLabel("生命值"));
        form.add(healthSpinner);
        form.add(new JLabel("攻击力"));
        form.add(attackSpinner);
        form.add(new JLabel("速度"));
        form.add(speedSpinner);
        form.add(new JLabel("怪物回血掉落"));
        form.add(monsterHealDropSpinner);
        form.add(new JLabel("远程攻击"));
        form.add(rangedAttackToggle);
        form.add(new JLabel("射击范围"));
        form.add(shootRangeSpinner);
        form.add(new JLabel("弹速"));
        form.add(projectileSpeedSpinner);
        form.add(new JLabel("射击冷却"));
        form.add(shootCooldownSpinner);
        form.add(new JLabel("怪物种类"));
        form.add(monsterKindSelector);
        form.add(new JLabel("重力系数%"));
        form.add(monsterGravitySpinner);
        form.add(new JLabel("可复活"));
        form.add(monsterRevivableToggle);
        form.add(new JLabel("复活延迟（秒）"));
        form.add(monsterReviveDelaySpinner);

        form.add(new JLabel("--- 建筑属性 ---"));
        form.add(new JLabel(""));
        form.add(new JLabel("可破坏"));
        form.add(destructibleToggle);
        form.add(new JLabel("耐久"));
        form.add(durabilitySpinner);
        form.add(new JLabel("支撑丢失后倒塌"));
        form.add(collapseToggle);
        form.add(new JLabel("倒塌伤害"));
        form.add(collapseDamageSpinner);
        form.add(new JLabel("踩踏几次后损坏"));
        form.add(breakAfterStepsSpinner);
        
        form.add(new JLabel("--- 玩家属性 ---"));
        form.add(new JLabel(""));
        form.add(new JLabel("互补色伤害"));
        form.add(damageSpinner);
        form.add(new JLabel("伤害开关"));
        form.add(damageToggle);
        form.add(new JLabel("子弹类型"));
        form.add(projectileTypeSelector);
        
        form.add(new JLabel("--- 物品属性 ---"));
        form.add(new JLabel(""));
        form.add(new JLabel("种类"));
        form.add(itemKindField);
        form.add(new JLabel("数值"));
        form.add(itemValueSpinner);
        form.add(new JLabel("消息"));
        form.add(itemMessageField);

        form.add(new JLabel("--- 触发器 ---"));
        form.add(new JLabel(""));
        form.add(new JLabel("目标名称"));
        form.add(triggerTargetField);
        form.add(new JLabel("触发动作"));
        form.add(triggerActionSelector);
        form.add(new JLabel("仅触发一次"));
        form.add(triggerOnceToggle);

        form.add(new JLabel("--- 刷怪笼 ---"));
        form.add(new JLabel(""));
        form.add(new JLabel("刷怪种类"));
        form.add(spawnerKindSelector);
        form.add(new JLabel("刷新间隔（秒）"));
        form.add(spawnIntervalSpinner);
        form.add(new JLabel("同时存在上限"));
        form.add(maxAliveSpinner);
        form.add(new JLabel("每波数量"));
        form.add(spawnWaveSizeSpinner);
        form.add(new JLabel("生成半径"));
        form.add(spawnRadiusSpinner);
        form.add(new JLabel("X 偏移"));
        form.add(spawnOffsetXSpinner);
        form.add(new JLabel("Y 偏移"));
        form.add(spawnOffsetYSpinner);

        JScrollPane scrollPane = new JScrollPane(form);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildToolPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(new JLabel("网格大小"));
        panel.add(gridSizeSpinner);
        panel.add(new JLabel("吸附"));
        panel.add(snapToggle);
        panel.add(new JLabel("显示网格"));
        panel.add(gridToggle);
        return buildToolFooter(panel);
    }

    private JPanel buildToolFooter(JPanel panel) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        footer.setBorder(BorderFactory.createTitledBorder("操作"));
        JButton saveButton = new JButton("保存");
        JButton loadButton = new JButton("加载");
        JButton deleteButton = new JButton("删除");
        JButton duplicateButton = new JButton("复制");
        JButton applyButton = new JButton("应用属性");
        JButton undoButton = new JButton("撤销");
        JButton redoButton = new JButton("重做");
        JButton scanButton = new JButton("扫描");
        JButton importButton = new JButton("导入");
        JButton exportButton = new JButton("导出");
        footer.add(saveButton);
        footer.add(loadButton);
        footer.add(importButton);
        footer.add(exportButton);
        footer.add(scanButton);
        footer.add(deleteButton);
        footer.add(duplicateButton);
        footer.add(applyButton);
        footer.add(undoButton);
        footer.add(redoButton);

        saveButton.addActionListener(event -> saveMap());
        loadButton.addActionListener(event -> loadMap());
        importButton.addActionListener(event -> importMap());
        exportButton.addActionListener(event -> exportMap());
        scanButton.addActionListener(event -> showScanReport("手动扫描结果", LevelAutoScanner.scan(world), true));
        deleteButton.addActionListener(event -> controller.deleteSelected());
        duplicateButton.addActionListener(event -> controller.duplicateSelected());
        applyButton.addActionListener(event -> applyPropertyChanges());
        undoButton.addActionListener(event -> controller.undo());
        redoButton.addActionListener(event -> controller.redo());

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);
        return wrapper;
    }

    private void initActions() {
        typeSelector.addActionListener(event -> {
            if (updatingControls) return;
            GameObjectType selected = (GameObjectType) typeSelector.getSelectedItem();
            GameObject current = controller.getSelectedObject();
            if (selected == null) {
                return;
            }
            if (current == null) {
                controller.setSelectedType(selected);
                return;
            }
            if (selected != current.getType()) {
                convertSelectedObjectType(selected);
            }
        });

        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }

            private void update() {
                if (updatingControls) {
                    return;
                }
                GameObject selected = controller.getSelectedObject();
                if (selected == null) {
                    return;
                }
                selected.setName(nameField.getText());
                selectionInfoLabel.setText(selected.getName() + " / " + selected.getType());
                previewPanel.repaint();
            }
        });

        activeToggle.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            GameObject selected = controller.getSelectedObject();
            if (selected == null) {
                return;
            }
            selected.setActive(activeToggle.isSelected());
            previewPanel.repaint();
        });

        modeSelector.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            controller.setEditMode(parseEditMode((String) modeSelector.getSelectedItem()));
        });

        colorButton.addActionListener(event -> {
            if (updatingControls) return;
            GameObject selected = controller.getSelectedObject();
            if (selected == null) return;
            Color oldColor = selected.getColor();
            Color chosen = JColorChooser.showDialog(this, "选择颜色", oldColor);
            if (chosen != null) {
                controller.executePropertyChange(
                    () -> {
                        selected.setColor(chosen);
                        colorButton.setBackground(chosen);
                        controller.setBrushColor(chosen);
                    },
                    () -> {
                        selected.setColor(oldColor);
                        colorButton.setBackground(oldColor);
                        controller.setBrushColor(oldColor);
                    }
                );
            }
        });

        // Map Level properties
        levelSelector.addActionListener(event -> {
            if (updatingControls) return;
            String selected = (String) levelSelector.getSelectedItem();
            if (selected != null && !selected.isBlank()) {
                mapNameField.setText(selected);
            }
        });

        widthSpinner.addChangeListener(event -> {
            if (updatingControls) return;
            int oldVal = world.getWidth();
            int newVal = (int) widthSpinner.getValue();
            if (oldVal == newVal) return;
            world.setSize(newVal, world.getHeight());
            normalizeWorldObjectsWithinBounds();
            syncPreviewSize();
        });
        heightSpinner.addChangeListener(event -> {
            if (updatingControls) return;
            int oldVal = world.getHeight();
            int newVal = (int) heightSpinner.getValue();
            if (oldVal == newVal) return;
            world.setSize(world.getWidth(), newVal);
            normalizeWorldObjectsWithinBounds();
            syncPreviewSize();
        });
        
        gravityToggle.addActionListener(event -> {
            if (updatingControls) return;
            world.setGravityEnabled(gravityToggle.isSelected());
            previewPanel.repaint();
        });
        gravityStrengthSpinner.addChangeListener(event -> {
            if (updatingControls) return;
            world.setGravityStrength((int) gravityStrengthSpinner.getValue());
        });

        // Inspector properties
        xSpinner.addChangeListener(e -> syncPosition());
        ySpinner.addChangeListener(e -> syncPosition());
        wSpinner.addChangeListener(e -> syncSize());
        hSpinner.addChangeListener(e -> syncSize());

        texturePathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            private void update() {
                if (updatingControls) return;
                GameObject selected = controller.getSelectedObject();
                if (selected instanceof BaseObject bo) {
                    bo.setTexturePath(texturePathField.getText().isBlank() ? null : texturePathField.getText());
                    previewPanel.repaint();
                }
            }
        });
        materialField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            private void update() {
                if (updatingControls) return;
                GameObject selected = controller.getSelectedObject();
                if (selected instanceof BaseObject bo) {
                    bo.setMaterial(materialField.getText().isBlank() ? null : materialField.getText());
                    previewPanel.repaint();
                }
            }
        });

        addActorListeners();
        addMonsterListeners();
        addSceneListeners();
        addItemListeners();
        addPlayerListeners();

        backgroundModeSelector.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            MapBackgroundMode selected = (MapBackgroundMode) backgroundModeSelector.getSelectedItem();
            world.setBackgroundMode(selected);
            updateBackgroundImageControls();
            previewPanel.repaint();
        });
        backgroundPresetSelector.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            MapBackgroundPreset selected = (MapBackgroundPreset) backgroundPresetSelector.getSelectedItem();
            if (selected == null) {
                selected = MapBackgroundPreset.DEFAULT;
            }
            world.setBackgroundPreset(selected);
            Color suggested = selected.getSuggestedBaseColor();
            if (suggested != null) {
                world.setBackgroundColor(suggested);
            }
            if (world.getBackgroundMode() != MapBackgroundMode.IMAGE) {
                world.setBackgroundMode(MapBackgroundMode.GRADIENT);
            }
            updateWorldControlsFromWorld();
        });
        backgroundColorButton.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            Color chosen = JColorChooser.showDialog(this, "选择背景颜色", backgroundColorButton.getBackground());
            if (chosen != null) {
                world.setBackgroundColor(chosen);
                backgroundColorButton.setBackground(chosen);
                previewPanel.repaint();
            }
        });
        importBackgroundImageButton.addActionListener(event -> importBackgroundImage());
        clearBackgroundImageButton.addActionListener(event -> clearBackgroundImage());
        winConditionSelector.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            WinConditionType selected = (WinConditionType) winConditionSelector.getSelectedItem();
            world.setWinCondition(selected);
            previewPanel.repaint();
        });
        targetKillsSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            world.setTargetKills((int) targetKillsSpinner.getValue());
        });
        targetItemsSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            world.setTargetItems((int) targetItemsSpinner.getValue());
        });
        gridToggle.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            overlay.setShowGrid(gridToggle.isSelected());
            previewPanel.repaint();
        });
        snapToggle.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            controller.setGridSnap(snapToggle.isSelected());
        });
        gridSizeSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            int gridSize = (int) gridSizeSpinner.getValue();
            controller.setGridSize(gridSize);
            overlay.setGridSize(gridSize);
            previewPanel.repaint();
        });
        fontSizeSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            int fontSize = (int) fontSizeSpinner.getValue();
            controller.setDefaultFontSize(fontSize);
            GameObject selected = controller.getSelectedObject();
            if (selected instanceof MenuObject menu) {
                menu.setFontSize(fontSize);
                menu.setSize(menu.getWidth(), Math.max(menu.getHeight(), menu.getPreferredHeight()));
            } else if (selected instanceof DialogObject dialog) {
                dialog.setFontSize(fontSize);
            }
            previewPanel.repaint();
        });
        menuColumnsSpinner.addChangeListener(event -> {
            if (updatingControls) return;
            GameObject selected = controller.getSelectedObject();
            if (selected instanceof MenuObject menu) {
                menu.setOptionColumns((int) menuColumnsSpinner.getValue());
                previewPanel.repaint();
            }
        });
        maxVisibleRowsSpinner.addChangeListener(event -> {
            if (updatingControls) return;
            GameObject selected = controller.getSelectedObject();
            if (selected instanceof MenuObject menu) {
                menu.setMaxVisibleRows((int) maxVisibleRowsSpinner.getValue());
                previewPanel.repaint();
            }
        });
    }

    private void syncPosition() {
        if (updatingControls) return;
        GameObject selected = controller.getSelectedObject();
        if (selected == null) return;
        int nx = (int) xSpinner.getValue();
        int ny = (int) ySpinner.getValue();
        if (selected.getX() == nx && selected.getY() == ny) return;
        selected.setPosition(nx, ny);
        if (selected instanceof PlayerObject) {
            world.refreshRespawnPointFromPlayers();
        }
        previewPanel.repaint();
    }

    private void syncSize() {
        if (updatingControls) return;
        GameObject selected = controller.getSelectedObject();
        if (selected == null) return;
        int nw = (int) wSpinner.getValue();
        int nh = (int) hSpinner.getValue();
        if (selected.getWidth() == nw && selected.getHeight() == nh) return;
        selected.setSize(nw, nh);
        if (selected instanceof PlayerObject) {
            world.refreshRespawnPointFromPlayers();
        }
        previewPanel.repaint();
    }

    private void addActorListeners() {
        healthSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof ActorObject actor) {
                actor.setHealth((int) healthSpinner.getValue());
            }
        });
        attackSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof ActorObject actor) {
                actor.setAttack((int) attackSpinner.getValue());
            }
        });
        speedSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof ActorObject actor) {
                actor.setSpeed((int) speedSpinner.getValue());
            }
        });
    }

    private void addMonsterListeners() {
        monsterHealDropSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof MonsterObject monster) {
                monster.setHealDropAmount((int) monsterHealDropSpinner.getValue());
            }
        });
        rangedAttackToggle.addActionListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof MonsterObject monster) {
                monster.setRangedAttacker(rangedAttackToggle.isSelected());
                updateInspectorFromSelection(monster);
            }
        });
        shootRangeSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof MonsterObject monster) {
                monster.setShootRange((int) shootRangeSpinner.getValue());
            }
        });
        projectileSpeedSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof MonsterObject monster) {
                monster.setProjectileSpeed((int) projectileSpeedSpinner.getValue());
            }
        });
        shootCooldownSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof MonsterObject monster) {
                monster.setShootCooldown(((Number) shootCooldownSpinner.getValue()).doubleValue());
            }
        });
        monsterKindSelector.addActionListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof MonsterObject monster) {
                monster.setMonsterKind((MonsterKind) monsterKindSelector.getSelectedItem());
                updateInspectorFromSelection(monster);
            }
        });
        monsterGravitySpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof MonsterObject monster) {
                monster.setGravityPercent((int) monsterGravitySpinner.getValue());
            }
        });
        monsterRevivableToggle.addActionListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof MonsterObject monster) {
                monster.setRevivable(monsterRevivableToggle.isSelected());
                updateInspectorFromSelection(monster);
            }
        });
        monsterReviveDelaySpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof MonsterObject monster) {
                monster.setReviveDelaySeconds(((Number) monsterReviveDelaySpinner.getValue()).doubleValue());
            }
        });
    }

    private void addSceneListeners() {
        destructibleToggle.addActionListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof SceneObject scene) {
                scene.setDestructible(destructibleToggle.isSelected());
                updateInspectorFromSelection(scene);
            }
        });
        durabilitySpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof SceneObject scene) {
                scene.setDurability((int) durabilitySpinner.getValue());
            }
        });
        collapseToggle.addActionListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof SceneObject scene) {
                scene.setCollapseWhenUnsupported(collapseToggle.isSelected());
                updateInspectorFromSelection(scene);
            }
        });
        collapseDamageSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof SceneObject scene) {
                scene.setCollapseDamage((int) collapseDamageSpinner.getValue());
            }
        });
        breakAfterStepsSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof SceneObject scene) {
                scene.setBreakAfterSteps((int) breakAfterStepsSpinner.getValue());
            }
        });
    }

    private void addItemListeners() {
        itemKindField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            private void update() {
                if (updatingControls) return;
                if (controller.getSelectedObject() instanceof ItemObject item) {
                    item.setKind(itemKindField.getText());
                    previewPanel.repaint();
                }
            }
        });
        itemValueSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof ItemObject item) {
                item.setValue((int) itemValueSpinner.getValue());
                previewPanel.repaint();
            }
        });
        itemMessageField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            private void update() {
                if (updatingControls) return;
                if (controller.getSelectedObject() instanceof ItemObject item) {
                    item.setMessage(itemMessageField.getText());
                    previewPanel.repaint();
                }
            }
        });

        triggerTargetField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }

            private void update() {
                if (updatingControls) {
                    return;
                }
                if (controller.getSelectedObject() instanceof TriggerObject trigger) {
                    trigger.setTargetName(triggerTargetField.getText());
                    previewPanel.repaint();
                }
            }
        });
        triggerActionSelector.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            if (controller.getSelectedObject() instanceof TriggerObject trigger) {
                TriggerAction action = (TriggerAction) triggerActionSelector.getSelectedItem();
                trigger.setAction(action);
                triggerTargetField.setEnabled(action != null && action.requiresTargetName());
                previewPanel.repaint();
            }
        });
        triggerOnceToggle.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            if (controller.getSelectedObject() instanceof TriggerObject trigger) {
                trigger.setTriggerOnce(triggerOnceToggle.isSelected());
                previewPanel.repaint();
            }
        });

        spawnerKindSelector.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            if (controller.getSelectedObject() instanceof SpawnerObject spawner) {
                spawner.setMonsterKind((MonsterKind) spawnerKindSelector.getSelectedItem());
                previewPanel.repaint();
            }
        });
        spawnIntervalSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            if (controller.getSelectedObject() instanceof SpawnerObject spawner) {
                spawner.setSpawnIntervalSeconds(((Number) spawnIntervalSpinner.getValue()).doubleValue());
                previewPanel.repaint();
            }
        });
        maxAliveSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            if (controller.getSelectedObject() instanceof SpawnerObject spawner) {
                spawner.setMaxAlive((int) maxAliveSpinner.getValue());
                previewPanel.repaint();
            }
        });
        spawnWaveSizeSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            if (controller.getSelectedObject() instanceof SpawnerObject spawner) {
                spawner.setSpawnWaveSize((int) spawnWaveSizeSpinner.getValue());
                previewPanel.repaint();
            }
        });
        spawnRadiusSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            if (controller.getSelectedObject() instanceof SpawnerObject spawner) {
                spawner.setSpawnRadius((int) spawnRadiusSpinner.getValue());
                previewPanel.repaint();
            }
        });
        spawnOffsetXSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            if (controller.getSelectedObject() instanceof SpawnerObject spawner) {
                spawner.setSpawnOffsetX((int) spawnOffsetXSpinner.getValue());
                previewPanel.repaint();
            }
        });
        spawnOffsetYSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            if (controller.getSelectedObject() instanceof SpawnerObject spawner) {
                spawner.setSpawnOffsetY((int) spawnOffsetYSpinner.getValue());
                previewPanel.repaint();
            }
        });
    }

    private void addPlayerListeners() {
        damageToggle.addActionListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof PlayerObject player) {
                player.setComplementaryColorDamageEnabled(damageToggle.isSelected());
            }
        });
        damageSpinner.addChangeListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof PlayerObject player) {
                player.setComplementaryColorDamage((int) damageSpinner.getValue());
            }
        });
        projectileTypeSelector.addActionListener(e -> {
            if (updatingControls) return;
            if (controller.getSelectedObject() instanceof PlayerObject player) {
                player.setProjectileType((ProjectileType) projectileTypeSelector.getSelectedItem());
            }
        });
    }

    private void updateInspectorFromSelection(GameObject selected) {
        updatingControls = true;
        try {
            if (selected == null) {
                selectionInfoLabel.setText("未选中对象");
                disableSpecialPanels();
                nameField.setText("");
                activeToggle.setSelected(false);
                fontSizeSpinner.setValue(controller.getDefaultFontSize());
                texturePathField.setText("");
                materialField.setText("");
                triggerTargetField.setText("");
                triggerActionSelector.setSelectedItem(TriggerAction.TOGGLE);
                triggerOnceToggle.setSelected(false);
                spawnIntervalSpinner.setValue(4.0);
                maxAliveSpinner.setValue(2);
                spawnWaveSizeSpinner.setValue(1);
                spawnRadiusSpinner.setValue(24);
                spawnOffsetXSpinner.setValue(0);
                spawnOffsetYSpinner.setValue(0);
                return;
            }
            selectionInfoLabel.setText(selected.getName() + " / " + selected.getType());
            nameField.setText(selected.getName());
            nameField.setEnabled(true);
            activeToggle.setSelected(selected.isActive());
            activeToggle.setEnabled(true);
            typeSelector.setSelectedItem(selected.getType());
            xSpinner.setValue(selected.getX());
            ySpinner.setValue(selected.getY());
            wSpinner.setValue(selected.getWidth());
            hSpinner.setValue(selected.getHeight());
            Color color = selected.getColor();
            if (color != null) {
                colorButton.setBackground(color);
                colorButton.setOpaque(true);
                controller.setBrushColor(color);
            }
            
            if (selected instanceof BaseObject bo) {
                texturePathField.setText(bo.getTexturePath() != null ? bo.getTexturePath() : "");
                materialField.setText(bo.getMaterial() != null ? bo.getMaterial() : "");
            } else {
                texturePathField.setText("");
                materialField.setText("");
            }

            // Default font size
            if (selected instanceof MenuObject menu) {
                fontSizeSpinner.setValue(menu.getFontSize());
                fontSizeSpinner.setEnabled(true);
                menuColumnsSpinner.setValue(menu.getOptionColumns());
                menuColumnsSpinner.setEnabled(true);
                maxVisibleRowsSpinner.setValue(menu.getMaxVisibleRows() == Integer.MAX_VALUE ? 0 : menu.getMaxVisibleRows());
                maxVisibleRowsSpinner.setEnabled(true);
            } else if (selected instanceof DialogObject dialog) {
                fontSizeSpinner.setValue(dialog.getFontSize());
                fontSizeSpinner.setEnabled(true);
                menuColumnsSpinner.setEnabled(false);
                maxVisibleRowsSpinner.setEnabled(false);
            } else {
                fontSizeSpinner.setValue(controller.getDefaultFontSize());
                fontSizeSpinner.setEnabled(false);
                menuColumnsSpinner.setEnabled(false);
                maxVisibleRowsSpinner.setEnabled(false);
            }

            // Actor attributes
            if (selected instanceof ActorObject actor) {
                healthSpinner.setEnabled(true);
                attackSpinner.setEnabled(true);
                speedSpinner.setEnabled(true);
                healthSpinner.setValue(actor.getHealth());
                attackSpinner.setValue(actor.getAttack());
                speedSpinner.setValue(actor.getSpeed());
            } else {
                healthSpinner.setEnabled(false);
                attackSpinner.setEnabled(false);
                speedSpinner.setEnabled(false);
            }

            if (selected instanceof MonsterObject monster) {
                monsterHealDropSpinner.setEnabled(true);
                monsterHealDropSpinner.setValue(monster.getHealDropAmount());
                rangedAttackToggle.setEnabled(true);
                rangedAttackToggle.setSelected(monster.isRangedAttacker());
                shootRangeSpinner.setEnabled(monster.isRangedAttacker());
                projectileSpeedSpinner.setEnabled(monster.isRangedAttacker());
                shootCooldownSpinner.setEnabled(monster.isRangedAttacker());
                shootRangeSpinner.setValue(monster.getShootRange());
                projectileSpeedSpinner.setValue(monster.getProjectileSpeed());
                shootCooldownSpinner.setValue(monster.getShootCooldown());
                monsterKindSelector.setEnabled(true);
                monsterKindSelector.setSelectedItem(monster.getMonsterKind());
                monsterGravitySpinner.setEnabled(true);
                monsterGravitySpinner.setValue(monster.getGravityPercent());
                monsterRevivableToggle.setEnabled(true);
                monsterRevivableToggle.setSelected(monster.isRevivable());
                monsterReviveDelaySpinner.setEnabled(monster.isRevivable());
                monsterReviveDelaySpinner.setValue(monster.getReviveDelaySeconds());
            } else {
                monsterHealDropSpinner.setEnabled(false);
                monsterHealDropSpinner.setValue(0);
                rangedAttackToggle.setEnabled(false);
                rangedAttackToggle.setSelected(false);
                shootRangeSpinner.setEnabled(false);
                projectileSpeedSpinner.setEnabled(false);
                shootCooldownSpinner.setEnabled(false);
                shootRangeSpinner.setValue(360);
                projectileSpeedSpinner.setValue(320);
                shootCooldownSpinner.setValue(1.2);
                monsterKindSelector.setEnabled(false);
                monsterKindSelector.setSelectedItem(MonsterKind.DEFAULT);
                monsterGravitySpinner.setEnabled(false);
                monsterGravitySpinner.setValue(100);
                monsterRevivableToggle.setEnabled(false);
                monsterRevivableToggle.setSelected(false);
                monsterReviveDelaySpinner.setEnabled(false);
                monsterReviveDelaySpinner.setValue(6.0);
            }

            if (selected instanceof SceneObject scene) {
                destructibleToggle.setEnabled(true);
                durabilitySpinner.setEnabled(scene.isDestructible());
                destructibleToggle.setSelected(scene.isDestructible());
                durabilitySpinner.setValue(scene.getDurability());
                collapseToggle.setEnabled(true);
                collapseToggle.setSelected(scene.isCollapseWhenUnsupported());
                collapseDamageSpinner.setEnabled(scene.isCollapseWhenUnsupported());
                collapseDamageSpinner.setValue(scene.getCollapseDamage());
                breakAfterStepsSpinner.setEnabled(true);
                breakAfterStepsSpinner.setValue(scene.getBreakAfterSteps());
            } else {
                destructibleToggle.setEnabled(false);
                destructibleToggle.setSelected(false);
                durabilitySpinner.setEnabled(false);
                durabilitySpinner.setValue(100);
                collapseToggle.setEnabled(false);
                collapseToggle.setSelected(false);
                collapseDamageSpinner.setEnabled(false);
                collapseDamageSpinner.setValue(30);
                breakAfterStepsSpinner.setEnabled(false);
                breakAfterStepsSpinner.setValue(0);
            }

            // Player attributes
            if (selected instanceof PlayerObject player) {
                damageToggle.setEnabled(true);
                damageSpinner.setEnabled(true);
                damageToggle.setSelected(player.isComplementaryColorDamageEnabled());
                damageSpinner.setValue(player.getComplementaryColorDamage());
                projectileTypeSelector.setEnabled(true);
                projectileTypeSelector.setSelectedItem(player.getProjectileType());
            } else {
                damageToggle.setEnabled(false);
                damageSpinner.setEnabled(false);
                projectileTypeSelector.setEnabled(false);
                projectileTypeSelector.setSelectedItem(ProjectileType.STANDARD);
            }

            // Item attributes
            if (selected instanceof ItemObject item) {
                itemKindField.setEnabled(true);
                itemValueSpinner.setEnabled(true);
                itemMessageField.setEnabled(true);
                itemKindField.setText(item.getKind());
                itemValueSpinner.setValue(item.getValue());
                itemMessageField.setText(item.getMessage());
            } else {
                itemKindField.setEnabled(false);
                itemValueSpinner.setEnabled(false);
                itemMessageField.setEnabled(false);
            }

            if (selected instanceof TriggerObject trigger) {
                triggerTargetField.setEnabled(true);
                triggerActionSelector.setEnabled(true);
                triggerOnceToggle.setEnabled(true);
                triggerTargetField.setText(trigger.getTargetName());
                triggerActionSelector.setSelectedItem(trigger.getAction());
                triggerOnceToggle.setSelected(trigger.isTriggerOnce());
                triggerTargetField.setEnabled(trigger.getAction() != null && trigger.getAction().requiresTargetName());
            } else {
                triggerTargetField.setEnabled(false);
                triggerActionSelector.setEnabled(false);
                triggerOnceToggle.setEnabled(false);
                triggerTargetField.setText("");
                triggerActionSelector.setSelectedItem(TriggerAction.TOGGLE);
                triggerOnceToggle.setSelected(false);
            }

            if (selected instanceof SpawnerObject spawner) {
                spawnerKindSelector.setEnabled(true);
                spawnIntervalSpinner.setEnabled(true);
                maxAliveSpinner.setEnabled(true);
                spawnWaveSizeSpinner.setEnabled(true);
                spawnRadiusSpinner.setEnabled(true);
                spawnOffsetXSpinner.setEnabled(true);
                spawnOffsetYSpinner.setEnabled(true);
                spawnerKindSelector.setSelectedItem(spawner.getMonsterKind());
                spawnIntervalSpinner.setValue(spawner.getSpawnIntervalSeconds());
                maxAliveSpinner.setValue(spawner.getMaxAlive());
                spawnWaveSizeSpinner.setValue(spawner.getSpawnWaveSize());
                spawnRadiusSpinner.setValue(spawner.getSpawnRadius());
                spawnOffsetXSpinner.setValue(spawner.getSpawnOffsetX());
                spawnOffsetYSpinner.setValue(spawner.getSpawnOffsetY());
            } else {
                spawnerKindSelector.setEnabled(false);
                spawnIntervalSpinner.setEnabled(false);
                maxAliveSpinner.setEnabled(false);
                spawnWaveSizeSpinner.setEnabled(false);
                spawnRadiusSpinner.setEnabled(false);
                spawnOffsetXSpinner.setEnabled(false);
                spawnOffsetYSpinner.setEnabled(false);
                spawnerKindSelector.setSelectedItem(MonsterKind.DEFAULT);
                spawnIntervalSpinner.setValue(4.0);
                maxAliveSpinner.setValue(2);
                spawnWaveSizeSpinner.setValue(1);
                spawnRadiusSpinner.setValue(24);
                spawnOffsetXSpinner.setValue(0);
                spawnOffsetYSpinner.setValue(0);
            }
        } finally {
            updatingControls = false;
        }
    }

    private void disableSpecialPanels() {
        nameField.setEnabled(false);
        activeToggle.setEnabled(false);
        healthSpinner.setEnabled(false);
        attackSpinner.setEnabled(false);
        speedSpinner.setEnabled(false);
        monsterHealDropSpinner.setEnabled(false);
        rangedAttackToggle.setEnabled(false);
        rangedAttackToggle.setSelected(false);
        shootRangeSpinner.setEnabled(false);
        projectileSpeedSpinner.setEnabled(false);
        shootCooldownSpinner.setEnabled(false);
        monsterKindSelector.setEnabled(false);
        monsterKindSelector.setSelectedItem(MonsterKind.DEFAULT);
        monsterGravitySpinner.setEnabled(false);
        monsterGravitySpinner.setValue(100);
        monsterRevivableToggle.setEnabled(false);
        monsterRevivableToggle.setSelected(false);
        monsterReviveDelaySpinner.setEnabled(false);
        destructibleToggle.setEnabled(false);
        destructibleToggle.setSelected(false);
        durabilitySpinner.setEnabled(false);
        collapseToggle.setEnabled(false);
        collapseToggle.setSelected(false);
        collapseDamageSpinner.setEnabled(false);
        breakAfterStepsSpinner.setEnabled(false);
        damageToggle.setEnabled(false);
        damageSpinner.setEnabled(false);
        projectileTypeSelector.setEnabled(false);
        projectileTypeSelector.setSelectedItem(ProjectileType.STANDARD);
        itemKindField.setEnabled(false);
        itemValueSpinner.setEnabled(false);
        itemMessageField.setEnabled(false);
        triggerTargetField.setEnabled(false);
        triggerActionSelector.setEnabled(false);
        triggerOnceToggle.setEnabled(false);
        spawnerKindSelector.setEnabled(false);
        spawnIntervalSpinner.setEnabled(false);
        maxAliveSpinner.setEnabled(false);
        spawnWaveSizeSpinner.setEnabled(false);
        spawnRadiusSpinner.setEnabled(false);
        spawnOffsetXSpinner.setEnabled(false);
        spawnOffsetYSpinner.setEnabled(false);
        fontSizeSpinner.setEnabled(false);
        menuColumnsSpinner.setEnabled(false);
        maxVisibleRowsSpinner.setEnabled(false);
    }

    private void updateWorldControlsFromWorld() {
        updatingControls = true;
        try {
            gravityToggle.setSelected(world.isGravityEnabled());
            gravityStrengthSpinner.setValue(world.getGravityStrength());
            backgroundPresetSelector.setSelectedItem(world.getBackgroundPreset());
            backgroundModeSelector.setSelectedItem(world.getBackgroundMode());
            backgroundColorButton.setBackground(world.getBackgroundColor());
            backgroundImageField.setText(resolveBackgroundImageLabel());
            winConditionSelector.setSelectedItem(world.getWinCondition());
            targetKillsSpinner.setValue(world.getTargetKills());
            targetItemsSpinner.setValue(world.getTargetItems());
            gridToggle.setSelected(overlay.isShowGrid());
            snapToggle.setSelected(controller.isGridSnap());
            gridSizeSpinner.setValue(controller.getGridSize());
            modeSelector.setSelectedItem(formatEditMode(controller.getEditMode()));
            modeHintLabel.setText(formatModeHint(controller.getEditMode()));
            updateBackgroundImageControls();
        } finally {
            updatingControls = false;
        }
        overlay.setGridSize(controller.getGridSize());
        previewPanel.repaint();
    }

    private String resolveBackgroundImageLabel() {
        if (world.getBackgroundImageName() != null && !world.getBackgroundImageName().isBlank()) {
            return world.getBackgroundImageName();
        }
        if (world.getBackgroundImage() != null) {
            return "已导入图片";
        }
        return "";
    }

    private void updateBackgroundImageControls() {
        backgroundImageField.setText(resolveBackgroundImageLabel());
        clearBackgroundImageButton.setEnabled(world.getBackgroundImage() != null);
        importBackgroundImageButton.setEnabled(true);
    }

    private void importBackgroundImage() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null) {
            return;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                JOptionPane.showMessageDialog(this, "无法识别图片文件", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            world.setBackgroundImage(image, file.getName());
            world.setBackgroundMode(MapBackgroundMode.IMAGE);
            updateWorldControlsFromWorld();
            previewPanel.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "导入背景图片失败: " + ex.getMessage(), "提示", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearBackgroundImage() {
        world.clearBackgroundImage();
        if (world.getBackgroundMode() == MapBackgroundMode.IMAGE) {
            world.setBackgroundMode(MapBackgroundMode.GRADIENT);
        }
        updateWorldControlsFromWorld();
        previewPanel.repaint();
    }

    private String formatEditMode(MapEditorController.EditMode mode) {
        if (mode == MapEditorController.EditMode.BUILD) {
            return "建造";
        }
        if (mode == MapEditorController.EditMode.ERASE) {
            return "破坏";
        }
        return "选择";
    }

    private String formatModeHint(MapEditorController.EditMode mode) {
        if (mode == MapEditorController.EditMode.BUILD) {
            return "<html><b>建造</b>：连续放置当前类型对象<br>适合铺地形或批量摆放</html>";
        }
        if (mode == MapEditorController.EditMode.ERASE) {
            return "<html><b>破坏</b>：点击或拖拽删除对象<br>适合快速清理场景</html>";
        }
        return "<html><b>选择</b>：点击选中对象并编辑属性<br>拖拽移动，空白处取消选中</html>";
    }

    private void applyPropertyChanges() {
        GameObject selected = controller.getSelectedObject();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "未选中对象", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EditorBounds.Rect normalizedRect = EditorBounds.normalizeRect(
            (int) xSpinner.getValue(),
            (int) ySpinner.getValue(),
            (int) wSpinner.getValue(),
            (int) hSpinner.getValue(),
            world.getWidth(),
            world.getHeight()
        );
        selected.setPosition(normalizedRect.x(), normalizedRect.y());
        selected.setSize(normalizedRect.width(), normalizedRect.height());
        xSpinner.setValue(normalizedRect.x());
        ySpinner.setValue(normalizedRect.y());
        wSpinner.setValue(normalizedRect.width());
        hSpinner.setValue(normalizedRect.height());
        selected.setName(nameField.getText());
        selected.setActive(activeToggle.isSelected());
        nameField.setText(selected.getName());
        activeToggle.setSelected(selected.isActive());
        selectionInfoLabel.setText(selected.getName() + " / " + selected.getType());
        Color color = colorButton.getBackground();
        if (color != null) {
            selected.setColor(color);
            controller.setBrushColor(color);
        }
        
        if (selected instanceof BaseObject bo) {
            bo.setTexturePath(texturePathField.getText().isBlank() ? null : texturePathField.getText());
            bo.setMaterial(materialField.getText().isBlank() ? null : materialField.getText());
        }

        if (selected instanceof MenuObject menu) {
            menu.setFontSize((int) fontSizeSpinner.getValue());
            menu.setOptionColumns((int) menuColumnsSpinner.getValue());
            menu.setMaxVisibleRows((int) maxVisibleRowsSpinner.getValue());
            menu.setSize(menu.getWidth(), Math.max(menu.getHeight(), menu.getPreferredHeight()));
        } else if (selected instanceof DialogObject dialog) {
            dialog.setFontSize((int) fontSizeSpinner.getValue());
        }

        if (selected instanceof ActorObject actor) {
            actor.setHealth((int) healthSpinner.getValue());
            actor.setAttack((int) attackSpinner.getValue());
            actor.setSpeed((int) speedSpinner.getValue());
        }

        if (selected instanceof MonsterObject monster) {
            monster.setHealDropAmount((int) monsterHealDropSpinner.getValue());
            MonsterKind kind = (MonsterKind) monsterKindSelector.getSelectedItem();
            monster.setMonsterKind(kind == null ? MonsterKind.DEFAULT : kind);
            monster.setGravityPercent((int) monsterGravitySpinner.getValue());
            monster.setRangedAttacker(rangedAttackToggle.isSelected());
            monster.setShootRange((int) shootRangeSpinner.getValue());
            monster.setProjectileSpeed((int) projectileSpeedSpinner.getValue());
            monster.setShootCooldown(((Number) shootCooldownSpinner.getValue()).doubleValue());
            monster.setRevivable(monsterRevivableToggle.isSelected());
            monster.setReviveDelaySeconds(((Number) monsterReviveDelaySpinner.getValue()).doubleValue());
        }

        if (selected instanceof SceneObject scene) {
            scene.setDestructible(destructibleToggle.isSelected());
            scene.setDurability((int) durabilitySpinner.getValue());
            scene.setCollapseWhenUnsupported(collapseToggle.isSelected());
            scene.setCollapseDamage((int) collapseDamageSpinner.getValue());
            scene.setBreakAfterSteps((int) breakAfterStepsSpinner.getValue());
        }

        if (selected instanceof PlayerObject player) {
            player.setComplementaryColorDamageEnabled(damageToggle.isSelected());
            player.setComplementaryColorDamage((int) damageSpinner.getValue());
            ProjectileType projectileType = (ProjectileType) projectileTypeSelector.getSelectedItem();
            player.setProjectileType(projectileType);
        }

        if (selected instanceof ItemObject item) {
            item.setKind(itemKindField.getText());
            item.setValue((int) itemValueSpinner.getValue());
            item.setMessage(itemMessageField.getText());
        }

        if (selected instanceof TriggerObject trigger) {
            trigger.setTargetName(triggerTargetField.getText());
            trigger.setAction((TriggerAction) triggerActionSelector.getSelectedItem());
            trigger.setTriggerOnce(triggerOnceToggle.isSelected());
            triggerTargetField.setEnabled(trigger.getAction() != null && trigger.getAction().requiresTargetName());
        }

        if (selected instanceof SpawnerObject spawner) {
            spawner.setMonsterKind((MonsterKind) spawnerKindSelector.getSelectedItem());
            spawner.setSpawnIntervalSeconds(((Number) spawnIntervalSpinner.getValue()).doubleValue());
            spawner.setMaxAlive((int) maxAliveSpinner.getValue());
            spawner.setSpawnWaveSize((int) spawnWaveSizeSpinner.getValue());
            spawner.setSpawnRadius((int) spawnRadiusSpinner.getValue());
            spawner.setSpawnOffsetX((int) spawnOffsetXSpinner.getValue());
            spawner.setSpawnOffsetY((int) spawnOffsetYSpinner.getValue());
        }

        if (selected instanceof PlayerObject) {
            world.refreshRespawnPointFromPlayers();
        }

        previewPanel.repaint();
    }

    private void convertSelectedObjectType(GameObjectType targetType) {
        GameObject selected = controller.getSelectedObject();
        if (selected == null || targetType == null || selected.getType() == targetType) {
            return;
        }
        var data = GameObjectFactory.toObjectData(selected);
        if (data == null) {
            return;
        }
        data.setType(targetType);
        GameObject replacement = GameObjectFactory.fromObjectData(data);
        if (replacement == null) {
            return;
        }
        controller.executePropertyChange(
            () -> replaceSelectedObject(selected, replacement),
            () -> replaceSelectedObject(replacement, selected)
        );
    }

    private void replaceSelectedObject(GameObject oldObject, GameObject newObject) {
        if (oldObject == null || newObject == null) {
            return;
        }
        boolean wasSelected = controller.getSelectedObject() == oldObject;
        world.removeObject(oldObject);
        world.addObject(newObject);
        if (wasSelected) {
            controller.setSelectedObject(newObject);
        }
        if (oldObject instanceof PlayerObject || newObject instanceof PlayerObject) {
            world.refreshRespawnPointFromPlayers();
        }
        previewPanel.repaint();
    }

    private void importMap() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        if (chooser.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            try {
                String content = java.nio.file.Files.readString(chooser.getSelectedFile().toPath());
                var mapData = MapDataMapper.importFromJson(new org.json.JSONObject(content));
                MapDataMapper.applyToWorld(world, mapData);
                normalizeWorldObjectsWithinBounds();
                mapNameField.setText(mapData.getName());
                updateWorldControlsFromWorld();
                previewPanel.repaint();
                JOptionPane.showMessageDialog(this, "导入成功");
                showScanReport("导入后自动扫描", LevelAutoScanner.scan(world), false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "导入失败: " + ex.getMessage());
            }
        }
    }

    private void exportMap() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        if (chooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            try {
                var mapData = MapDataMapper.fromWorld(world, mapNameField.getText());
                var json = MapDataMapper.exportToJson(mapData);
                java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), json.toString(4));
                JOptionPane.showMessageDialog(this, "导出成功");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage());
            }
        }
    }

    private void saveMap() {
        normalizeWorldObjectsWithinBounds();
        List<LevelAutoScanner.ScanIssue> issues = LevelAutoScanner.scan(world);
        if (hasSeverity(issues, LevelAutoScanner.Severity.ERROR)) {
            int result = JOptionPane.showConfirmDialog(
                this,
                buildScanMessage(issues),
                "自动扫描发现错误，是否继续保存？",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }
        repository.saveMap(MapDataMapper.fromWorld(world, mapNameField.getText()));
        refreshLevelSelector();
        JOptionPane.showMessageDialog(this, "保存完成", "提示", JOptionPane.INFORMATION_MESSAGE);
        if (!issues.isEmpty()) {
            showScanReport("保存后自动扫描", issues, false);
        }
    }

    private void loadMap() {
        String name = mapNameField.getText();
        var mapData = repository.loadMapByName(name);
        if (mapData == null) {
            JOptionPane.showMessageDialog(this, "未找到地图", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        MapDataMapper.applyToWorld(world, mapData);
        normalizeWorldObjectsWithinBounds();
        mapNameField.setText(mapData.getName());
        widthSpinner.setValue(world.getWidth());
        heightSpinner.setValue(world.getHeight());
        updateWorldControlsFromWorld();
        controller.clearBrushColor();
        controller.setSelectedObject(null);
        colorButton.setBackground(new Color(180, 180, 200));
        colorButton.setOpaque(true);
        refreshLevelSelector();
        syncPreviewSize();
        previewPanel.repaint();
        showScanReport("加载后自动扫描", LevelAutoScanner.scan(world), false);
    }

    private void refreshLevelSelector() {
        List<String> names = new ArrayList<>(repository.listMapNames());
        names.removeIf(this::isShellLevelName);
        String currentName = mapNameField.getText();
        if (currentName != null && !currentName.isBlank()
            && !isShellLevelName(currentName)
            && !names.contains(currentName)) {
            names.add(currentName);
        }
        if (names.isEmpty()) {
            names.add("demo-map");
        }
        levelSelector.setModel(new DefaultComboBoxModel<>(names.toArray(new String[0])));
        if (currentName != null && !currentName.isBlank()) {
            levelSelector.setSelectedItem(currentName);
        }
    }

    private boolean isShellLevelName(String name) {
        return "main-menu".equals(name)
            || "level-select-menu".equals(name)
            || "pause-menu".equals(name)
            || "options-menu".equals(name);
    }

    private void syncPreviewSize() {
        previewPanel.setPreferredSize(new Dimension(world.getWidth(), world.getHeight()));
        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private void normalizeWorldObjectsWithinBounds() {
        GameObject selected = controller.getSelectedObject();
        for (GameObject object : world.getObjects()) {
            EditorBounds.Rect normalizedRect = EditorBounds.normalizeRect(
                object.getX(),
                object.getY(),
                object.getWidth(),
                object.getHeight(),
                world.getWidth(),
                world.getHeight(),
                1
            );
            if (normalizedRect.x() != object.getX() || normalizedRect.y() != object.getY()) {
                object.setPosition(normalizedRect.x(), normalizedRect.y());
            }
            if (normalizedRect.width() != object.getWidth() || normalizedRect.height() != object.getHeight()) {
                object.setSize(normalizedRect.width(), normalizedRect.height());
            }
        }
        if (selected != null) {
            updateInspectorFromSelection(selected);
        }
    }

    private void showScanReport(String title, List<LevelAutoScanner.ScanIssue> issues, boolean showCleanMessage) {
        if (issues == null || issues.isEmpty()) {
            if (showCleanMessage) {
                JOptionPane.showMessageDialog(this, "自动扫描未发现问题。", title, JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }
        boolean hasError = hasSeverity(issues, LevelAutoScanner.Severity.ERROR);
        boolean hasWarning = hasSeverity(issues, LevelAutoScanner.Severity.WARNING);
        if (!showCleanMessage && !hasError && !hasWarning) {
            return;
        }
        int type = hasError ? JOptionPane.ERROR_MESSAGE : (hasWarning ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        JOptionPane.showMessageDialog(this, buildScanMessage(issues), title, type);
    }

    private boolean hasSeverity(List<LevelAutoScanner.ScanIssue> issues, LevelAutoScanner.Severity severity) {
        if (issues == null || severity == null) {
            return false;
        }
        for (LevelAutoScanner.ScanIssue issue : issues) {
            if (issue.severity() == severity) {
                return true;
            }
        }
        return false;
    }

    private String buildScanMessage(List<LevelAutoScanner.ScanIssue> issues) {
        StringBuilder builder = new StringBuilder();
        int maxLines = 14;
        int count = 0;
        for (LevelAutoScanner.ScanIssue issue : issues) {
            builder.append('[')
                .append(issue.severity().name())
                .append("] ")
                .append(issue.message())
                .append('\n');
            count++;
            if (count >= maxLines && issues.size() > maxLines) {
                builder.append("... 其余 ").append(issues.size() - maxLines).append(" 条已省略");
                break;
            }
        }
        return builder.toString();
    }

    private MapEditorController.EditMode parseEditMode(String value) {
        if ("建造".equals(value)) {
            return MapEditorController.EditMode.BUILD;
        }
        if ("破坏".equals(value)) {
            return MapEditorController.EditMode.ERASE;
        }
        return MapEditorController.EditMode.SELECT;
    }

    public static void open(GameWorld world, MapRepository repository) {
        SwingUtilities.invokeLater(() -> {
            EditorWindow window = new EditorWindow(world, repository);
            window.setVisible(true);
        });
    }
}
