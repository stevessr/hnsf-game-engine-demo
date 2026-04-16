package lib.input;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import lib.render.SwingGamePanel;

/**
 * 独立按键绑定设置窗口。
 */
public final class KeyBindingsWindow extends JFrame {
    private final SwingGamePanel gamePanel;
    private final InputActionMapper mapper;
    private final Map<InputAction, JButton> bindButtons = new EnumMap<>(InputAction.class);
    private InputAction pendingAction = null;

    public KeyBindingsWindow(SwingGamePanel gamePanel) {
        super("按键绑定设置 (Key Bindings)");
        this.gamePanel = gamePanel;
        this.mapper = gamePanel.getInputController().getActionMapper();
        
        initLayout();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void initLayout() {
        setLayout(new BorderLayout(10, 10));
        
        JPanel listPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        for (InputAction action : InputAction.values()) {
            listPanel.add(new JLabel(action.name() + ":"));
            
            String currentKeys = getActionKeysString(action);
            JButton btn = new JButton(currentKeys);
            btn.addActionListener(e -> startRebind(action));
            bindButtons.put(action, btn);
            listPanel.add(btn);
        }
        
        add(new JScrollPane(listPanel), BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("完成 (Done)");
        closeBtn.addActionListener(e -> dispose());
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // 添加全局按键监听以捕获改键
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (pendingAction != null) {
                    handleKeyCaptured(e.getKeyCode());
                }
            }
        });
        setFocusable(true);
    }

    private void startRebind(InputAction action) {
        pendingAction = action;
        JButton btn = bindButtons.get(action);
        btn.setText("请按键... (Press any key)");
        btn.requestFocusInWindow();
        requestFocus();
    }

    private void handleKeyCaptured(int keyCode) {
        if (keyCode == KeyEvent.VK_ESCAPE) {
            refreshButtonText(pendingAction);
            pendingAction = null;
            return;
        }

        mapper.clearBindings(pendingAction);
        mapper.bindKey(pendingAction, keyCode);
        
        gamePanel.syncInputMap();
        gamePanel.savePersistentSettings();
        
        refreshButtonText(pendingAction);
        pendingAction = null;
        
        // 提示
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "按键已更新！");
        });
    }

    private void refreshButtonText(InputAction action) {
        JButton btn = bindButtons.get(action);
        if (btn != null) {
            btn.setText(getActionKeysString(action));
        }
    }

    private String getActionKeysString(InputAction action) {
        Set<Integer> keys = mapper.getKeyBindings().get(action);
        if (keys == null || keys.isEmpty()) {
            return "None";
        }
        StringBuilder sb = new StringBuilder();
        for (int key : keys) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(KeyEvent.getKeyText(key));
        }
        return sb.toString();
    }

    public static void open(SwingGamePanel gamePanel) {
        SwingUtilities.invokeLater(() -> {
            KeyBindingsWindow window = new KeyBindingsWindow(gamePanel);
            window.setVisible(true);
        });
    }
}
