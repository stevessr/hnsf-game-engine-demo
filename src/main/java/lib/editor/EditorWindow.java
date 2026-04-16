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
import lib.object.ActorObject;
import lib.object.BaseObject;
import lib.object.DialogObject;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.ItemObject;
import lib.object.MenuObject;
import lib.object.PlayerObject;
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
        this.gridToggle = new JToggleButton("网格显示", true);
        this.snapToggle = new JToggleButton("网格吸附", true);
        this.gridSizeSpinner = new JSpinner(new SpinnerNumberModel(20, 4, 200, 1));
        this.selectionInfoLabel = new JLabel("未选中对象");
        initLayout();
        initActions();
        controller.setSelectionListener(this::updateInspectorFromSelection);
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
        JButton applyButton = new JButton("应用属性");
        JButton undoButton = new JButton("撤销");
        JButton redoButton = new JButton("重做");
        footer.add(saveButton);
        footer.add(loadButton);
        footer.add(deleteButton);
        footer.add(applyButton);
        footer.add(undoButton);
        footer.add(redoButton);

        saveButton.addActionListener(event -> saveMap());
        loadButton.addActionListener(event -> loadMap());
        deleteButton.addActionListener(event -> controller.deleteSelected());
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
            syncPreviewSize();
        });
        heightSpinner.addChangeListener(event -> {
            if (updatingControls) {
                return;
            }
            world.setSize(world.getWidth(), (int) heightSpinner.getValue());
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
            gridToggle.setSelected(overlay.isShowGrid());
            snapToggle.setSelected(controller.isGridSnap());
            gridSizeSpinner.setValue(controller.getGridSize());
        } finally {
            updatingControls = false;
        }
        overlay.setGridSize(controller.getGridSize());
        previewPanel.repaint();
    }

    private void applyPropertyChanges() {
        GameObject selected = controller.getSelectedObject();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "未选中对象", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        selected.setPosition((int) xSpinner.getValue(), (int) ySpinner.getValue());
        selected.setSize((int) wSpinner.getValue(), (int) hSpinner.getValue());
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

    private void saveMap() {
        repository.saveMap(MapDataMapper.fromWorld(world, mapNameField.getText()));
        refreshLevelSelector();
        JOptionPane.showMessageDialog(this, "保存完成", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadMap() {
        String name = mapNameField.getText();
        var mapData = repository.loadMapByName(name);
        if (mapData == null) {
            JOptionPane.showMessageDialog(this, "未找到地图", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        MapDataMapper.applyToWorld(world, mapData);
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
