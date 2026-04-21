package lib.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

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
import lib.object.ItemObject;
import lib.object.MenuObject;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.object.SceneObject;
import lib.persistence.MapDataMapper;
import lib.persistence.MapRepository;

public final class EditorWindow extends JFrame {
    private final GameWorld world;
    private final EditorOverlay overlay;
    private final EditorGamePanel previewPanel;
    private final MapEditorController controller;
    private final MapRepository repository;
    private final JComboBox<GameObjectType> typeSelector;
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
    private final JSpinner xSpinner;
    private final JSpinner ySpinner;
    private final JSpinner wSpinner;
    private final JSpinner hSpinner;
    private final JSpinner fontSizeSpinner;
    private final JSpinner damageSpinner;
    private final JButton colorButton;
    private final JCheckBox damageToggle;
    private final JTextField itemKindField;
    private final JSpinner itemValueSpinner;
    private final JTextField itemMessageField;
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
    private final JCheckBox destructibleToggle;
    private final JSpinner durabilitySpinner;
    private final JCheckBox collapseToggle;
    private final JSpinner collapseDamageSpinner;
    private final JSpinner breakAfterStepsSpinner;
    private final JToggleButton gridToggle;
    private final JToggleButton snapToggle;
    private final JSpinner gridSizeSpinner;
    private final JLabel selectionInfoLabel;
    private boolean updatingControls;

    public EditorWindow(GameWorld world, MapRepository repository) {
        super("关卡编辑器");
        this.world = world;
        this.repository = repository;
        this.overlay = new EditorOverlay();
        this.previewPanel = new EditorGamePanel(world, overlay);
        this.controller = new MapEditorController(world, previewPanel, overlay);
        this.typeSelector = new JComboBox<>(GameObjectType.values());
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
        this.xSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 4000, 1));
        this.ySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 4000, 1));
        this.wSpinner = new JSpinner(new SpinnerNumberModel(80, 4, 4000, 1));
        this.hSpinner = new JSpinner(new SpinnerNumberModel(60, 4, 4000, 1));
        this.fontSizeSpinner = new JSpinner(new SpinnerNumberModel(18, 10, 64, 1));
        this.damageSpinner = new JSpinner(new SpinnerNumberModel(14, 0, 999, 1));
        this.colorButton = new JButton("颜色");
        this.colorButton.setOpaque(true);
        this.damageToggle = new JCheckBox("互补色伤害", true);
        this.itemKindField = new JTextField("coin");
        this.itemValueSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 9999, 1));
        this.itemMessageField = new JTextField("");
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
        this.destructibleToggle = new JCheckBox("可破坏", false);
        this.durabilitySpinner = new JSpinner(new SpinnerNumberModel(100, 1, 9999, 1));
        this.collapseToggle = new JCheckBox("失去支撑后倒塌", false);
        this.collapseDamageSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 9999, 1));
        this.breakAfterStepsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
        this.gridToggle = new JToggleButton("网格显示", true);
        this.snapToggle = new JToggleButton("网格吸附", true);
        this.gridSizeSpinner = new JSpinner(new SpinnerNumberModel(20, 4, 200, 1));
        this.selectionInfoLabel = new JLabel("未选中对象");
        initLayout();
        initActions();
        controller.setSelectionListener(this::updateInspectorFromSelection);
        controller.setModeChangeListener(mode -> {
            updatingControls = true;
            modeSelector.setSelectedItem(formatEditMode(mode));
            updatingControls = false;
        });
        refreshLevelSelector();
        updateWorldControlsFromWorld();
        updateInspectorFromSelection(null);
        controller.bind();
    }

    private void initLayout() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        setMinimumSize(new Dimension(1280, 800));

        JPanel palettePanel = new JPanel(new GridLayout(0, 1, 6, 6));
        palettePanel.setBorder(BorderFactory.createTitledBorder("对象库"));
        for (GameObjectType type : GameObjectType.values()) {
            JButton button = new JButton(type.name());
            button.addActionListener(event -> {
                controller.setSelectedType(type);
                typeSelector.setSelectedItem(type);
            });
            palettePanel.add(button);
        }

        JPanel previewWrapper = new JPanel(new BorderLayout());
        previewWrapper.setBorder(BorderFactory.createTitledBorder("预览"));
        previewWrapper.add(new JScrollPane(previewPanel), BorderLayout.CENTER);

        JTabbedPane inspectorTabs = new JTabbedPane();
        inspectorTabs.addTab("地图", buildMapPanel());
        inspectorTabs.addTab("对象", buildPropertyPanel());
        inspectorTabs.addTab("工具", buildToolPanel());

        add(palettePanel, BorderLayout.WEST);
        add(previewWrapper, BorderLayout.CENTER);
        add(inspectorTabs, BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);
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
        form.add(new JLabel("类型"));
        form.add(typeSelector);
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
        
        form.add(new JLabel("--- 物品属性 ---"));
        form.add(new JLabel(""));
        form.add(new JLabel("种类"));
        form.add(itemKindField);
        form.add(new JLabel("数值"));
        form.add(itemValueSpinner);
        form.add(new JLabel("消息"));
        form.add(itemMessageField);

        JScrollPane scrollPane = new JScrollPane(form);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildToolPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(new JLabel("模式"));
        panel.add(modeSelector);
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
            GameObjectType selected = (GameObjectType) typeSelector.getSelectedItem();
            if (selected != null) {
                controller.setSelectedType(selected);
            }
        });

        modeSelector.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            controller.setEditMode(parseEditMode((String) modeSelector.getSelectedItem()));
        });

        colorButton.addActionListener(event -> {
            Color chosen = JColorChooser.showDialog(this, "选择颜色", colorButton.getBackground());
            if (chosen != null) {
                colorButton.setBackground(chosen);
                colorButton.setOpaque(true);
                controller.setBrushColor(chosen);
                if (controller.getSelectedObject() != null) {
                    controller.getSelectedObject().setColor(chosen);
                    previewPanel.repaint();
                }
            }
        });

        levelSelector.addActionListener(event -> {
            String selected = (String) levelSelector.getSelectedItem();
            if (selected != null && !selected.isBlank()) {
                mapNameField.setText(selected);
            }
        });

        widthSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            world.setSize((int) widthSpinner.getValue(), world.getHeight());
            normalizeWorldObjectsWithinBounds();
            syncPreviewSize();
        });
        heightSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            world.setSize(world.getWidth(), (int) heightSpinner.getValue());
            normalizeWorldObjectsWithinBounds();
            syncPreviewSize();
        });
        gravityToggle.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            world.setGravityEnabled(gravityToggle.isSelected());
            previewPanel.repaint();
        });
        gravityStrengthSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            world.setGravityStrength((int) gravityStrengthSpinner.getValue());
        });
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
        destructibleToggle.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            durabilitySpinner.setEnabled(destructibleToggle.isSelected() && controller.getSelectedObject() instanceof SceneObject);
        });
        rangedAttackToggle.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            boolean enabled = rangedAttackToggle.isSelected() && controller.getSelectedObject() instanceof MonsterObject;
            shootRangeSpinner.setEnabled(enabled);
            projectileSpeedSpinner.setEnabled(enabled);
            shootCooldownSpinner.setEnabled(enabled);
        });
        collapseToggle.addActionListener(event -> {
            if (updatingControls) {
                return;
            }
            boolean enabled = collapseToggle.isSelected() && controller.getSelectedObject() instanceof SceneObject;
            collapseDamageSpinner.setEnabled(enabled);
        });
    }

    private void updateInspectorFromSelection(GameObject selected) {
        updatingControls = true;
        try {
            if (selected == null) {
                selectionInfoLabel.setText("未选中对象");
                disableSpecialPanels();
                fontSizeSpinner.setValue(controller.getDefaultFontSize());
                texturePathField.setText("");
                materialField.setText("");
                return;
            }
            selectionInfoLabel.setText(selected.getName() + " / " + selected.getType());
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
            } else if (selected instanceof DialogObject dialog) {
                fontSizeSpinner.setValue(dialog.getFontSize());
                fontSizeSpinner.setEnabled(true);
            } else {
                fontSizeSpinner.setValue(controller.getDefaultFontSize());
                fontSizeSpinner.setEnabled(false);
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
            } else {
                damageToggle.setEnabled(false);
                damageSpinner.setEnabled(false);
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
        } finally {
            updatingControls = false;
        }
    }

    private void disableSpecialPanels() {
        healthSpinner.setEnabled(false);
        attackSpinner.setEnabled(false);
        speedSpinner.setEnabled(false);
        monsterHealDropSpinner.setEnabled(false);
        rangedAttackToggle.setEnabled(false);
        rangedAttackToggle.setSelected(false);
        shootRangeSpinner.setEnabled(false);
        projectileSpeedSpinner.setEnabled(false);
        shootCooldownSpinner.setEnabled(false);
        destructibleToggle.setEnabled(false);
        destructibleToggle.setSelected(false);
        durabilitySpinner.setEnabled(false);
        collapseToggle.setEnabled(false);
        collapseToggle.setSelected(false);
        collapseDamageSpinner.setEnabled(false);
        breakAfterStepsSpinner.setEnabled(false);
        damageToggle.setEnabled(false);
        damageSpinner.setEnabled(false);
        itemKindField.setEnabled(false);
        itemValueSpinner.setEnabled(false);
        itemMessageField.setEnabled(false);
        fontSizeSpinner.setEnabled(false);
    }

    private void updateWorldControlsFromWorld() {
        updatingControls = true;
        try {
            gravityToggle.setSelected(world.isGravityEnabled());
            gravityStrengthSpinner.setValue(world.getGravityStrength());
            winConditionSelector.setSelectedItem(world.getWinCondition());
            targetKillsSpinner.setValue(world.getTargetKills());
            targetItemsSpinner.setValue(world.getTargetItems());
            gridToggle.setSelected(overlay.isShowGrid());
            snapToggle.setSelected(controller.isGridSnap());
            gridSizeSpinner.setValue(controller.getGridSize());
            modeSelector.setSelectedItem(formatEditMode(controller.getEditMode()));
        } finally {
            updatingControls = false;
        }
        overlay.setGridSize(controller.getGridSize());
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
            monster.setRangedAttacker(rangedAttackToggle.isSelected());
            monster.setShootRange((int) shootRangeSpinner.getValue());
            monster.setProjectileSpeed((int) projectileSpeedSpinner.getValue());
            monster.setShootCooldown(((Number) shootCooldownSpinner.getValue()).doubleValue());
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
        }

        if (selected instanceof ItemObject item) {
            item.setKind(itemKindField.getText());
            item.setValue((int) itemValueSpinner.getValue());
            item.setMessage(itemMessageField.getText());
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
