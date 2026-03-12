package lib.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import lib.game.GameWorld;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.persistence.MapDataMapper;
import lib.persistence.MapRepository;

public final class EditorWindow extends JFrame {
    private final GameWorld world;
    private final EditorOverlay overlay;
    private final EditorGamePanel previewPanel;
    private final MapEditorController controller;
    private final MapRepository repository;
    private final JComboBox<GameObjectType> typeSelector;
    private final JTextField mapNameField;
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JSpinner xSpinner;
    private final JSpinner ySpinner;
    private final JSpinner wSpinner;
    private final JSpinner hSpinner;
    private final JButton colorButton;
    private final JToggleButton gridToggle;

    public EditorWindow(GameWorld world, MapRepository repository) {
        super("地图编辑器");
        this.world = world;
        this.repository = repository;
        this.overlay = new EditorOverlay();
        this.previewPanel = new EditorGamePanel(world, overlay);
        this.controller = new MapEditorController(world, previewPanel, overlay);
        this.typeSelector = new JComboBox<>(GameObjectType.values());
        this.mapNameField = new JTextField("demo-map");
        this.widthSpinner = new JSpinner(new SpinnerNumberModel(world.getWidth(), 320, 4000, 10));
        this.heightSpinner = new JSpinner(new SpinnerNumberModel(world.getHeight(), 240, 3000, 10));
        this.xSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 4000, 1));
        this.ySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 4000, 1));
        this.wSpinner = new JSpinner(new SpinnerNumberModel(80, 4, 4000, 1));
        this.hSpinner = new JSpinner(new SpinnerNumberModel(60, 4, 4000, 1));
        this.colorButton = new JButton("颜色");
        this.gridToggle = new JToggleButton("网格", true);
        initLayout();
        initActions();
        controller.bind();
    }

    private void initLayout() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

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

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(buildMapPanel(), BorderLayout.NORTH);
        rightPanel.add(buildPropertyPanel(), BorderLayout.CENTER);
        rightPanel.add(buildToolPanel(), BorderLayout.SOUTH);

        add(palettePanel, BorderLayout.WEST);
        add(previewWrapper, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildMapPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("地图"));
        panel.add(new JLabel("名称"));
        panel.add(mapNameField);
        panel.add(new JLabel("宽度"));
        panel.add(widthSpinner);
        panel.add(new JLabel("高度"));
        panel.add(heightSpinner);
        return panel;
    }

    private JPanel buildPropertyPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("属性"));
        panel.add(new JLabel("类型"));
        panel.add(typeSelector);
        panel.add(new JLabel("X"));
        panel.add(xSpinner);
        panel.add(new JLabel("Y"));
        panel.add(ySpinner);
        panel.add(new JLabel("宽"));
        panel.add(wSpinner);
        panel.add(new JLabel("高"));
        panel.add(hSpinner);
        panel.add(new JLabel("颜色"));
        panel.add(colorButton);
        return panel;
    }

    private JPanel buildToolPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("工具"));
        JButton saveButton = new JButton("保存");
        JButton loadButton = new JButton("加载");
        JButton deleteButton = new JButton("删除");
        JButton applyButton = new JButton("应用属性");
        panel.add(saveButton);
        panel.add(loadButton);
        panel.add(deleteButton);
        panel.add(applyButton);
        panel.add(gridToggle);

        saveButton.addActionListener(event -> saveMap());
        loadButton.addActionListener(event -> loadMap());
        deleteButton.addActionListener(event -> controller.deleteSelected());
        applyButton.addActionListener(event -> applyPropertyChanges());
        gridToggle.addActionListener(event -> controller.toggleGrid());
        return panel;
    }

    private void initActions() {
        typeSelector.addActionListener(event -> {
            GameObjectType selected = (GameObjectType) typeSelector.getSelectedItem();
            if (selected != null) {
                controller.setSelectedType(selected);
            }
        });

        colorButton.addActionListener(event -> {
            Color chosen = javax.swing.JColorChooser.showDialog(this, "选择颜色", Color.WHITE);
            if (chosen != null) {
                colorButton.setBackground(chosen);
                colorButton.setOpaque(true);
            }
        });

        widthSpinner.addChangeListener(event -> world.setSize((int) widthSpinner.getValue(), world.getHeight()));
        heightSpinner.addChangeListener(event -> world.setSize(world.getWidth(), (int) heightSpinner.getValue()));
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
        }
        previewPanel.repaint();
    }

    private void saveMap() {
        repository.saveMap(MapDataMapper.fromWorld(world, mapNameField.getText()));
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
        previewPanel.repaint();
    }

    public static void open(GameWorld world, MapRepository repository) {
        SwingUtilities.invokeLater(() -> {
            EditorWindow window = new EditorWindow(world, repository);
            window.setVisible(true);
        });
    }
}
